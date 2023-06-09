package com.kiko.kadblib

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ConnectException

/**
 * This class represents an ADB connection.
 * @author Cameron Gutman
 */
class AdbConnection private constructor() : Closeable {
    var channel: AdbChannel? = null

    /** The last allocated local stream ID. The ID
     * chosen for the next stream will be this value + 1.
     */
    private var lastLocalId = 0

    /**
     * The backend thread that handles responding to ADB packets.
     */
    private val connectionThread: Thread?

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
    private var delay: Long = 10_000


    /**
     * Specifies whether this connection has already sent a signed token.
     */
    private var sentSignature = false

    /**
     * A hash map of our open streams indexed by local ID.
     */
    private val openStreams: HashMap<Int, AdbStream>

    /**
     * Internal constructor to initialize some internal state
     */
    init {
        openStreams = HashMap<Int, AdbStream>()
        connectionThread = createConnectionThread()
    }

    /**
     * Creates a new connection thread.
     * @return A new connection thread.
     */
    private fun createConnectionThread(): Thread {
        val conn = this
        return Thread(Runnable {
            while (!connectionThread!!.isInterrupted) {
                try {
                    /* Read and parse a message off the socket's input stream */
                    val msg: AdbMessage = AdbMessage.parseAdbMessage(channel!!)

                    /* Verify magic and checksum */if (!AdbProtocol.validateMessage(msg)) continue
                    when (msg.command) {
                        AdbProtocol.CMD_OKAY, AdbProtocol.CMD_WRTE, AdbProtocol.CMD_CLSE -> {
                            /* We must ignore all packets when not connected */if (!conn.connected) continue

                            /* Get the stream object corresponding to the packet */
                            val waitingStream: AdbStream =
                                openStreams[msg.arg1] ?: continue
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
                                    conn.openStreams.remove(msg.arg1)

                                    /* Notify readers and writers */waitingStream.notifyClose()
                                }
                            }
                        }

                        AdbProtocol.CMD_AUTH -> {
                            var packet: AdbMessage
                            if (msg.arg0 == AdbProtocol.AUTH_TYPE_TOKEN) {
                                /* This is an authentication challenge */
                                if (conn.sentSignature) {
                                    /* We've already tried our signature, so send our public key */
                                    packet = AdbProtocol.generateAuth(
                                        AdbProtocol.AUTH_TYPE_RSA_PUBLIC,
                                        conn.crypto?.adbPublicKeyPayload
                                    )
                                } else {
                                    /* We'll sign the token */
                                    packet = AdbProtocol.generateAuth(
                                        AdbProtocol.AUTH_TYPE_SIGNATURE,
                                        conn.crypto?.signAdbTokenPayload(msg.payload)
                                    )
                                    conn.sentSignature = true
                                }

                                /* Write the AUTH reply */conn.channel!!.writex(packet)
                            }
                        }

                        AdbProtocol.CMD_CNXN -> synchronized(conn) {

                            /* We need to store the max data size */
                            conn.maxData = msg.arg1

                            /* Mark us as connected and unwait anyone waiting on the connection */
                            conn.connected = true

                            val lock = Object()
                            synchronized(lock) {
                                conn.mnotifyAll(lock)
                            }
                        }

                        else -> {}
                    }
                } catch (e: Exception) {
                    /* The cleanup is taken care of by a combination of this thread
						 * and close() */
                    e.printStackTrace()
                    break
                }
            }

            /* This thread takes care of cleaning up pending streams */synchronized(conn) {
            cleanupStreams()
            val lock = Object()
            synchronized(lock) {
                conn.mnotifyAll(lock)
            }
            conn.connectAttempted = false
        }
        })
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
        synchronized(this) {

            /* Block if a connection is pending, but not yet complete */if (!connected) mwait()
            if (!connected) {
                throw IOException("Connection failed")
            }
        }
        return maxData
    }

    /**
     * Connects to the remote device. This routine will block until the connection
     * completes.
     * @throws java.io.IOException If the socket fails while connecting
     * @throws InterruptedException If we are unable to wait for the connection to finish
     */
    @Throws(IOException::class, InterruptedException::class)
    fun connect() {
        if (connected) throw IllegalStateException("Already connected")

        /* Write the CONNECT packet */channel!!.writex(AdbProtocol.generateConnect())

        /* Start the connection thread to respond to the peer */connectAttempted = true
        connectionThread!!.start()

        /* Wait for the connection to go live */
        synchronized(this) {
            if (!connected) mwait()
            if (!connected) {
                throw IOException("Connection failed")
            }
        }
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
    @Throws(UnsupportedEncodingException::class, IOException::class, InterruptedException::class)
    fun open(destination: String?): AdbStream {
        val localId = ++lastLocalId
        if (!connectAttempted) throw IllegalStateException("connect() must be called first")

        /* Wait for the connect response */synchronized(this) {
            if (!connected) mwait()
            if (!connected) {
                throw IOException("Connection failed")
            }
        }

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

    /** This routine closes the Adb connection and underlying socket
     * @throws java.io.IOException if the socket fails to close
     */
    @Throws(IOException::class)
    override fun close() {
        /* If the connection thread hasn't spawned yet, there's nothing to do */
        if (connectionThread == null) return

        /* Closing the channel will kick the connection thread */
        channel!!.close()

        /* Wait for the connection thread to die */connectionThread.interrupt()
        try {
            connectionThread.join()
        } catch (e: InterruptedException) {
        }
    }

    companion object {
        /**
         * Creates a AdbConnection object associated with the socket and
         * crypto object specified.
         * @param channel The channel that the connection will use for communcation.
         * @param crypto The crypto object that stores the key pair for authentication.
         * @return A new AdbConnection object.
         * @throws java.io.IOException If there is a socket error
         */
        @Throws(IOException::class)
        fun create(channel: AdbChannel?, crypto: AdbCrypto?, delay: Long = 10_000): AdbConnection {
            val newConn = AdbConnection()
            newConn.delay = delay
            newConn.crypto = crypto
            newConn.channel = channel
            return newConn
        }
    }

    private fun mwait() {
        runBlocking {
            delay(delay)
        }
    }

    fun mnotifyAll(lock: Object) {
        lock.notifyAll()
    }
}
