package com.kiko.kadblib

import android.content.Context
import android.util.Log
import com.kiko.kadblib.states.ConnectionResult
import com.kiko.kadblib.states.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ConnectException

/**
 * This class represents an ADB connection.
 * Класс для работы с подключением к ADB
 * @author Cameron Gutman
 * @author BadKiko
 */
class AdbConnection private constructor() {
    var channel: AdbChannel? = null

    /** The last allocated local stream ID. The ID
     * chosen for the next stream will be this value + 1.
     */
    private var lastLocalId = 0
    /*
        */
    /**
     * Specifies whether a connect has been attempted
     */
    private var connectAttempted = false

    /**
     * Specifies whether a CNXN packet has been received from the peer.
     */
    private var connected = false

    /**
     * Specifies the maximum amount data that can be sent to the remote peer.
     * This is only valid after connect() returns successfully.
     */
    private var maxData = 0

    /**
     * An initialized ADB crypto object that contains a key pair.
     */
    private var crypto: AdbCrypto? = null

    /**
     * Delay to connect
     */
    private var timeout: Long = 10_000


    /**
     * Specifies whether this connection has already sent a signed token.
     */
    private var sentSignature = false

    /**
     * A hash map of our open streams indexed by local ID.
     */
    private val openStreams: HashMap<Int, AdbStream> = HashMap<Int, AdbStream>()

    lateinit var connectionResult: ConnectionResult

    companion object {
        /**
         * Creates a AdbConnection object associated with the socket and
         * crypto object specified.
         * @param channel The channel that the connection will use for communcation.
         * @param crypto The crypto object that stores the key pair for authentication.
         * @param timeToCloseConnection Given time for connecting to device
         * @return A new AdbConnection object.
         * @throws java.io.IOException If there is a socket error
         */
        @Throws(IOException::class)
        fun create(
            channel: AdbChannel?,
            crypto: AdbCrypto?,
            connectionResult: ConnectionResult,
            timeToCloseConnection: Long = 10_000,
        ): AdbConnection {
            val newConn = AdbConnection()
            newConn.timeout = timeToCloseConnection
            newConn.crypto = crypto
            newConn.channel = channel
            newConn.connectionResult = connectionResult
            return newConn
        }
    }

    private fun doAdbWork() {
        val currentAdbConnection = this
        try {
            /* Read and parse a message off the socket's input stream */
            Log.d("ADBConnector", "Start parsing adb message")
            val msg: AdbMessage = AdbMessage.parseAdbMessage(channel!!)


            Log.d("ADBConnector", "Msg is ${msg.command}")
            /* Verify magic and checksum */
            if (!AdbProtocol.validateMessage(msg)) return

            when (msg.command) {
                AdbProtocol.CMD_OKAY, AdbProtocol.CMD_WRTE, AdbProtocol.CMD_CLSE -> {
                    /* We must ignore all packets when not connected */
                    if (!currentAdbConnection.connected) return

                    /* Get the stream object corresponding to the packet */
                    val waitingStream: AdbStream = openStreams[msg.arg1] ?: return

                    synchronized(waitingStream) {
                        if (msg.command == AdbProtocol.CMD_OKAY) {
                            /* We're ready for writes */
                            waitingStream.updateRemoteId(msg.arg0)
                            waitingStream.readyForWrite()

                            /* Unwait an open/write */waitingStream.notifyClose()
                        } else if (msg.command == AdbProtocol.CMD_WRTE) {
                            /* Got some data from our partner */
                            waitingStream.addPayload(msg.payload!!)

                            /* Tell it we're ready for more */waitingStream.sendReady()
                        } else if (msg.command == AdbProtocol.CMD_CLSE) {
                            /* He doesn't like us anymore :-( */
                            currentAdbConnection.openStreams.remove(msg.arg1)

                            /* Notify readers and writers */waitingStream.notifyClose()
                        }
                    }
                }

                AdbProtocol.CMD_AUTH -> {
                    var packet: AdbMessage
                    if (msg.arg0 == AdbProtocol.AUTH_TYPE_TOKEN) {
                        /* This is an authentication challenge */
                        if (currentAdbConnection.sentSignature) {
                            /* We've already tried our signature, so send our public key */
                            packet = AdbProtocol.generateAuth(
                                AdbProtocol.AUTH_TYPE_RSA_PUBLIC,
                                currentAdbConnection.crypto?.adbPublicKeyPayload
                            )
                        } else {
                            /* We'll sign the token */
                            packet = AdbProtocol.generateAuth(
                                AdbProtocol.AUTH_TYPE_SIGNATURE,
                                currentAdbConnection.crypto?.signAdbTokenPayload(msg.payload)
                            )
                            currentAdbConnection.sentSignature = true
                        }

                        /* Write the AUTH reply */
                        currentAdbConnection.channel!!.writex(
                            packet
                        )
                    }
                }

                AdbProtocol.CMD_CNXN -> synchronized(currentAdbConnection) {

                    /* We need to store the max data size */
                    currentAdbConnection.maxData = msg.arg1

                    /* Mark us as connected and unwait anyone waiting on the connection */
                    currentAdbConnection.connected = true
                }

                else -> {}
            }
        } catch (e: Exception) {
            /* The cleanup is taken care of by a combination of this thread
             * and close() */
            connectionResult.onError(Error(e.message))
            currentAdbConnection.cleanupStreams()
            return
        }
    }

    /**
     * Gets the max data size that the remote client supports.
     * A connection must have been attempted before calling this routine.
     * This routine will block if a connection is in progress.
     * @return The maximum data size indicated in the connect packet.
     * @throws InterruptedException If a connection cannot be waited on.
     * @throws java.io.IOException if the connection fails
     */
    @Throws(InterruptedException::class, IOException::class)
    fun getMaxData(): Int {
        if (!connectAttempted) throw IllegalStateException("connect() must be called first")
        /* Block if a connection is pending, but not yet complete */
        connectionCheck()
        return maxData
    }

    /**
     * Connects to the remote device. This routine will block until the connection
     * completes.
     * @throws java.io.IOException If the socket fails while connecting
     * @throws InterruptedException If we are unable to wait for the connection to finish
     */
    @Throws(IOException::class, InterruptedException::class)
    suspend fun connect() {
        if (connected) throw IllegalStateException("Already connected")
        if (channel == null) {
            Log.d("ADBConnection", "Channel is null, maybe target is unreachable")
            return
        }

        // Write connecting state
        connectionResult.changedState(ConnectionState.CONNECTING)
        coroutineScope {
            launch {
                channel?.writex(AdbProtocol.generateConnect())

                // Проверяем ответы от АДБ пока не подключено
                while (!connected) {
                    /* Отправляем запрос на подключение */
                    doAdbWork()
                }
                connectionResult.changedState(ConnectionState.CONNECTED)
                // Это нужная херь видимо, автор библиотеки попросил не удалять :)
                open("shell:exec date")
            }
            launch {
                delay(timeout)
                // Проверяем есть ли коннект
                connectionCheck()
                if (!connected){
                    // Закрываем корутину если в итоге нет подключения
                    this.cancel()
                }
            }
        }
        /* Start the connection thread to respond to the peer */
        connectAttempted = true
    }

    /**
     * Opens an AdbStream object corresponding to the specified destination.
     * This routine will block until the connection completes.
     * @param destination The destination to open on the target
     * @return AdbStream object corresponding to the specified destination
     * @throws java.io.UnsupportedEncodingException If the destination cannot be encoded to UTF-8
     * @throws java.io.IOException If the stream fails while sending the packet
     * @throws InterruptedException If we are unable to wait for the connection to finish
     */
    fun open(destination: String?): AdbStream {
        val localId = ++lastLocalId
        if (!connected) throw Error("connect() must be called first")

        connectionCheck()

        /* Add this stream to this list of half-open streams */
        val stream = AdbStream(this, localId)
        openStreams[localId] = stream

        /* Send the open */
        channel!!.writex(AdbProtocol.generateOpen(localId, destination!!))

        /* Wait for the connection thread to receive the OKAY */
        synchronized(stream) { stream.mwait() }

        /* Check if the open was rejected */
        if (stream.isClosed) throw ConnectException("Stream open actively rejected by remote peer")
        /* We're fully setup now */
        return stream
    }

    /**
     * This function terminates all I/O on streams associated with this ADB connection
     */
    private fun cleanupStreams() {
        /* Close all streams on this connection */
        for (s: AdbStream in openStreams.values) {
            /* We handle exceptions for each close() call to avoid
			 * terminating cleanup for one failed close(). */
            try {
                s.close()
            } catch (e: IOException) {
            }
        }

        /* No open streams anymore */openStreams.clear()
    }

    fun connectionCheck() {
        if (!connected) runBlocking {
            delay(3000)
            coroutineScope {
                if (!connected)
                    connectionResult.changedState(ConnectionState.TIMEOUT)
            }
        }
    }
}
