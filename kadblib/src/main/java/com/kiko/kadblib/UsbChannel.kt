package com.kiko.kadblib

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbRequest
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList

/**
 * Created by xudong on 2/21/14.
 */
class UsbChannel(
    private val mDeviceConnection: UsbDeviceConnection,
    private val mInterface: UsbInterface
) : AdbChannel {
    private val mEndpointOut: UsbEndpoint
    private val mEndpointIn: UsbEndpoint
    private val defaultTimeout = 1000
    private val mInRequestPool = LinkedList<UsbRequest>()

    // return an IN request to the pool
    fun releaseInRequest(request: UsbRequest) {
        synchronized(mInRequestPool) { mInRequestPool.add(request) }
    }

    val inRequest: UsbRequest
        // get an IN request from the pool
        get() {
            synchronized(mInRequestPool) {
                return if (mInRequestPool.isEmpty()) {
                    val request = UsbRequest()
                    request.initialize(mDeviceConnection, mEndpointIn)
                    request
                } else {
                    mInRequestPool.removeFirst()
                }
            }
        }

    @Throws(IOException::class)
    override fun readx(buffer: ByteArray?, length: Int) {
        val usbRequest = inRequest
        val expected = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
        usbRequest.clientData = expected
        if (!usbRequest.queue(expected, length)) {
            throw IOException("fail to queue read UsbRequest")
        }
        while (true) {
            val wait = mDeviceConnection.requestWait()
                ?: throw IOException("Connection.requestWait return null")
            val clientData = wait.clientData as ByteBuffer
            wait.clientData = null
            if (wait.endpoint === mEndpointOut) {
                // a write UsbRequest complete, just ignore
            } else if (expected === clientData) {
                releaseInRequest(wait)
                break
            } else {
                throw IOException("unexpected behavior")
            }
        }
        expected.flip()
        expected[buffer]
    }

    // API LEVEL 18 is needed to invoke bulkTransfer(mEndpointOut, buffer, offset, buffer.length - offset, defaultTimeout)
    //    @Override
    //    public void writex(byte[] buffer) throws IOException{
    //
    //        int offset = 0;
    //        int transferred = 0;
    //
    //        while ((transferred = mDeviceConnection.bulkTransfer(mEndpointOut, buffer, offset, buffer.length - offset, defaultTimeout)) >= 0) {
    //            offset += transferred;
    //            if (offset >= buffer.length) {
    //                break;
    //            }
    //        }
    //        if (transferred < 0) {
    //            throw new IOException("bulk transfer fail");
    //        }
    //    }
    // A dirty solution, only API level 12 is needed, not 18
    @Throws(IOException::class)
    private fun writex(buffer: ByteArray?) {
        var offset = 0
        var transferred = 0
        val tmp = ByteArray(buffer!!.size)
        System.arraycopy(buffer, 0, tmp, 0, buffer.size)
        while (mDeviceConnection.bulkTransfer(
                mEndpointOut,
                tmp,
                buffer.size - offset,
                defaultTimeout
            ).also { transferred = it } >= 0
        ) {
            offset += transferred
            if (offset >= buffer.size) {
                break
            } else {
                System.arraycopy(buffer, offset, tmp, 0, buffer.size - offset)
            }
        }
        if (transferred < 0) {
            throw IOException("bulk transfer fail")
        }
    }

    @Throws(IOException::class)
    override fun writex(message: AdbMessage?) {
        // TODO: here is the weirdest thing
        // write (message.head + message.payload) is totally different with write(message.head) + write(head.payload)
        writex(message!!.message)
        if (message.payload != null) {
            writex(message.payload)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        mDeviceConnection.releaseInterface(mInterface)
        mDeviceConnection.close()
    }

    init {
        var epOut: UsbEndpoint? = null
        var epIn: UsbEndpoint? = null
        // look for our bulk endpoints
        for (i in 0 until mInterface.endpointCount) {
            val ep = mInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    epOut = ep
                } else {
                    epIn = ep
                }
            }
        }
        require(!(epOut == null || epIn == null)) { "not all endpoints found" }
        mEndpointOut = epOut
        mEndpointIn = epIn
    }
}
