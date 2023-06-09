package com.kiko.kadblib

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class provides an abstraction for the ADB message format.
 * @author Cameron Gutman
 */
class AdbMessage {
    private var mMessageBuffer: ByteBuffer? = null
    /** The payload of the message  */
    @JvmField
    var payload: ByteArray? = null

    private constructor()

    // sets the fields in the command header
    @JvmOverloads
    constructor(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null as ByteArray?) {
        mMessageBuffer =
            ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
        mMessageBuffer!!.putInt(0, command)
        mMessageBuffer!!.putInt(4, arg0)
        mMessageBuffer!!.putInt(8, arg1)
        mMessageBuffer!!.putInt(12, data?.size ?: 0)
        mMessageBuffer!!.putInt(16, if (data == null) 0 else checksum(data))
        mMessageBuffer!!.putInt(20, command xor -0x1)
        payload = data
    }

    val command: Int
        /** The command field of the message  */
        get() = mMessageBuffer!!.getInt(0)
    val arg0: Int
        /** The arg0 field of the message  */
        get() = mMessageBuffer!!.getInt(4)
    val arg1: Int
        /** The arg1 field of the message  */
        get() = mMessageBuffer!!.getInt(8)
    val payloadLength: Int
        /** The payload length field of the message  */
        get() = mMessageBuffer!!.getInt(12)
    val checksum: Int
        /** The checksum field of the message  */
        get() = mMessageBuffer!!.getInt(16)
    val magic: Int
        /** The magic field of the message  */
        get() = mMessageBuffer!!.getInt(20)
    val message: ByteArray
        get() = mMessageBuffer!!.array()

    companion object {
        /**
         * Read and parse an ADB message from the supplied input stream.
         * This message is NOT validated.
         * @param in InputStream object to read data from
         * @return An AdbMessage object represented the message read
         * @throws java.io.IOException If the stream fails while reading
         */
        @Throws(IOException::class)
        fun parseAdbMessage(`in`: AdbChannel): AdbMessage {
            val msg = AdbMessage()
            val packet =
                ByteBuffer.allocate(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

            /* Read the header first */`in`.readx(packet.array(), AdbProtocol.ADB_HEADER_LENGTH)
            msg.mMessageBuffer = packet

            /* If there's a payload supplied, read that too */if (msg.payloadLength != 0) {
                msg.payload = ByteArray(msg.payloadLength)
                `in`.readx(msg.payload, msg.payloadLength)
            }
            return msg
        }

        /**
         * This function performs a checksum on the ADB payload data.
         * @param payload Payload to checksum
         * @return The checksum of the payload
         */
        fun checksum(payload: ByteArray): Int {
            var checksum = 0
            for (b in payload) {
                /* We have to manually "unsign" these bytes because Java sucks */
                checksum += if (b >= 0) b.toInt() else b + 256
            }
            return checksum
        }
    }
}