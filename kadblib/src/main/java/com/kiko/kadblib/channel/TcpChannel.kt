package com.kiko.kadblib.channel

import android.util.Log
import com.kiko.kadblib.adbbase.AdbChannel
import com.kiko.kadblib.adbbase.AdbMessage
import com.kiko.kadblib.results.ConnectionResult
import com.kiko.kadblib.states.ConnectionState
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.NoRouteToHostException
import java.net.Socket

/**
 * Created by xudong on 2/21/14.
 */
class TcpChannel : AdbChannel {

    companion object {
        /**
         * Creates a TCPChannel object which using for connect to ADB
         * @param ipAddress Address of destination device
         * @param port Port of destination device
         * @return TCPChannel object
         */
        fun create(
            ipAddress: String,
            port: Int,
            connectionResult: ConnectionResult
        ): TcpChannel? {
            val tcpChannel = TcpChannel()
            connectionResult.changedState(ConnectionState.CONNECTING)
            return try {
                tcpChannel.socket = Socket(ipAddress, port)
                tcpChannel.createSocket()
                tcpChannel
            } catch (noRouteError: NoRouteToHostException) {
                connectionResult.changedState(ConnectionState.UNREACHABLE)
                Log.e("TCPChannel", "Tcp connection error, target is unreachable")
                null
            }
        }
    }

    /** The underlying socket that this class uses to communicate with the target device.  */
    private var socket: Socket? = null

    /**
     * The input stream that this class uses to read from the socket.
     */
    private var inputStream: InputStream? = null

    /**
     * The output stream that this class uses to read from the socket.
     */
    private var outputStream: OutputStream? = null

    @Throws(IOException::class)
    override fun readx(buffer: ByteArray?, length: Int) {
        if (inputStream == null) {
            Log.e("TCPChannel", "Error, stream is null, tcp channel unreachable")
            return
        }


        var dataRead = 0
        do {
            val bytesRead = inputStream!!.read(buffer, dataRead, length - dataRead)
            dataRead += if (bytesRead < 0) throw IOException("Stream closed") else bytesRead
        } while (dataRead < length)
    }

    @Throws(IOException::class)
    private fun writex(buffer: ByteArray?) {
        if (socket == null) {
            Log.e("TCPChannel", "Error, message is null, tcp channel unreachable")
            return
        }

        outputStream?.write(buffer)
        outputStream?.flush()
    }

    @Throws(IOException::class)
    override fun writex(message: AdbMessage?) {
        if (message == null) {
            Log.e("TCPChannel", "Error, message is null, tcp channel unreachable")
            return
        }

        writex(message.message)
        if (message.payload != null) {
            writex(message.payload)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        if (socket == null) {
            Log.e("TCPChannel", "Error, socket is null, tcp channel unreachable")
            return
        }

        socket?.close()
    }

    private fun createSocket() {
        try {
            if (socket == null) {
                throw IOException("You need create() first")
            }

            /* Disable Nagle because we're sending tiny packets */
            socket?.tcpNoDelay = true
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
