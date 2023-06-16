package com.kiko.kadblib.adbbase

import com.kiko.kadblib.adbbase.AdbProtocol.generateClose
import com.kiko.kadblib.adbbase.AdbProtocol.generateReady
import com.kiko.kadblib.adbbase.AdbProtocol.generateWrite
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.io.IOException
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * This class abstracts the underlying ADB streams
 * @author Cameron Gutman
 */
class AdbStream(
    /** The AdbConnection object that the stream communicates over  */
    private val adbConn: AdbConnection,
    /** The local ID of the stream  */
    private val localId: Int
) : Closeable {
    /** The remote ID of the stream  */
    private var remoteId = 0

    /** Indicates whether a write is currently allowed  */
    private val writeReady = AtomicBoolean(false)

    /** A queue of data from the target's write packets  */
    private val readQueue: Queue<ByteArray>  = ConcurrentLinkedQueue()
    /**
     * Retreives whether the stream is closed or not
     * @return True if the stream is close, false if not
     */
    /** Indicates whether the connection is closed already  */
    var isClosed = false
        private set

    /**
     * Called by the connection thread to indicate newly received data.
     * @param payload Data inside the write message
     */
    fun addPayload(payload: ByteArray) {
        synchronized(readQueue) {
            readQueue.add(payload)
            (readQueue as java.lang.Object).notifyAll()
        }
    }

    /**
     * Called by the connection thread to send an OKAY packet, allowing the
     * other side to continue transmission.
     * @throws java.io.IOException If the connection fails while sending the packet
     */
    @Throws(IOException::class)
    fun sendReady() {
        /* Generate and send a READY packet */
        adbConn.channel!!.writex(generateReady(localId, remoteId))
    }

    /**
     * Called by the connection thread to update the remote ID for this stream
     * @param remoteId New remote ID
     */
    fun updateRemoteId(remoteId: Int) {
        this.remoteId = remoteId
    }

    /**
     * Called by the connection thread to indicate the stream is okay to send data.
     */
    fun readyForWrite() {
        writeReady.set(true)
    }

    /**
     * Called by the connection thread to notify that the stream was closed by the peer.
     */
    fun notifyClose() {
        /* We don't call close() because it sends another CLOSE */
        isClosed = true

        /* Unwait readers and writers */synchronized(this) { Object().notifyAll() }
        synchronized(readQueue) {(readQueue as java.lang.Object).notifyAll() }
    }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    /**
     * Reads a pending write payload from the other side.
     * @return Byte array containing the payload of the write
     * @throws InterruptedException If we are unable to wait for data
     * @throws java.io.IOException If the stream fails while waiting
     */
    @Throws(InterruptedException::class, IOException::class)
    fun read(): ByteArray? {
        var data: ByteArray? = null
        lock.lock()
        try {
            while (!isClosed && readQueue.poll()?.also { data = it } == null) {
                condition.await()
            }
            if (isClosed) {
                throw IOException("Stream closed")
            }
        } finally {
            lock.unlock()
        }
        return data
    }

    /**
     * Sends a write packet with a given String payload.
     * @param payload Payload in the form of a String
     * @throws java.io.IOException If the stream fails while sending data
     * @throws InterruptedException If we are unable to wait to send data
     */
    @Throws(IOException::class, InterruptedException::class)
    fun write(payload: String) {
        /* ADB needs null-terminated strings */
        write((payload + "\u0000").toByteArray(charset("UTF-8")))
    }

    /**
     * Sends a write packet with a given byte array payload.
     * @param payload Payload in the form of a byte array
     * @throws java.io.IOException If the stream fails while sending data
     * @throws InterruptedException If we are unable to wait to send data
     */
    @Throws(IOException::class, InterruptedException::class)
    fun write(payload: ByteArray?) {
        lock.lock()
        try {
            while (!isClosed && !writeReady.compareAndSet(true, false)) {
                condition.await()
            }
            if (isClosed) {
                throw IOException("Stream closed")
            }
        } finally {
            lock.unlock()
        }

        /* Generate a WRITE packet and send it */
        adbConn.channel!!.writex(generateWrite(localId, remoteId, payload))
    }


    /**
     * Closes the stream. This sends a close message to the peer.
     * @throws java.io.IOException If the stream fails while sending the close message.
     */
    @Throws(IOException::class)
    override fun close() {
        synchronized(this) {

            /* This may already be closed by the remote host */if (isClosed) return

            /* Notify readers/writers that we've closed */notifyClose()
        }
        adbConn.channel!!.writex(generateClose(localId, remoteId))
    }

    fun mwait(){
        runBlocking {
            delay(1_000)
        }
    }
}
