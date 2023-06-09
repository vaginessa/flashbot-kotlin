package com.kiko.kadblib

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Created by xudong on 2/21/14.
 */
class TcpChannel(socket: Socket) : AdbChannel {
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
        var dataRead = 0
        do {
            val bytesRead = inputStream!!.read(buffer, dataRead, length - dataRead)
            dataRead += if (bytesRead < 0) throw IOException("Stream closed") else bytesRead
        } while (dataRead < length)
    }

    @Throws(IOException::class)
    private fun writex(buffer: ByteArray?) {
        outputStream!!.write(buffer)
        outputStream!!.flush()
    }

    @Throws(IOException::class)
    override fun writex(message: AdbMessage?) {
        writex(message!!.message)
        if (message.payload != null) {
            writex(message.payload)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        socket!!.close()
    }

    init {
        try {
            /* Disable Nagle because we're sending tiny packets */
            socket.tcpNoDelay = true
            this.socket = socket
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
