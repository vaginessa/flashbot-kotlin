package com.kiko.kflashbot.data

import android.util.Log
import com.kiko.kadblib.adbbase.AdbConnection
import com.kiko.kadblib.adbbase.AdbStream
import com.kiko.kflashbot.utils.ByteUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class Pusher {
    private var adbConnection: AdbConnection? = null
    private var local: File? = null
    private var remotePath: String? = null

    fun push(adbConnection: AdbConnection?, local: File?, remotePath: String?) {
        Log.d("Pusher", "Start pushing file")
        Log.d("Pusher", "Local file is: ${local?.path}")
        Log.d("Pusher", "Local file existing: ${local?.exists()}")
        Log.d("Pusher", "Local file can read: ${local?.canRead()}")
        Log.d("Pusher", "Is ADBConnection nullable: ${adbConnection == null}")

        val stream: AdbStream = adbConnection!!.open("shell:")

        stream.write("reboot")

        Log.d("Pusher", "Opened stream sync:")
        val sendId = "SEND"
        val mode = ",33206"
        val length = (remotePath + mode).length

        Log.d("Pusher", "Length is: $length")
        val bytes = ByteUtils.concat(sendId.toByteArray(), ByteUtils.intToByteArray(length))

        Log.d("Pusher", "Bytes: ${bytes.joinToString(",")}")
        stream.write(bytes)
        Log.d("Pusher", "Concatinate with byteUtils")

        stream.write(remotePath!!.toByteArray())
        stream.write(mode.toByteArray())
        Log.d("Pusher", "Stream write bytes")

        val buff = ByteArray(adbConnection.getMaxData())
        val `is`: InputStream = FileInputStream(local)
        var sent: Long = 0
        val total = local!!.length()
        var lastProgress = 0
        Log.d("Pusher", "Continue pushing file")
        while (true) {
            val read = `is`.read(buff)
            if (read < 0) {
                break
            }
            Log.d("Pusher", "Start write data")
            stream.write(ByteUtils.concat("DATA".toByteArray(), ByteUtils.intToByteArray(read)))
            if (read == buff.size) {
                stream.write(buff)
            } else {
                val tmp = ByteArray(read)
                System.arraycopy(buff, 0, tmp, 0, read)
                stream.write(tmp)
            }
            sent += read.toLong()
            val progress = (sent * 100 / total).toInt()
            if (lastProgress != progress) {
                /*handler.sendMessage(
                    handler.obtainMessage(
                        Message.INSTALLING_PROGRESS,
                        Message.PUSH_PART,
                        progress
                    )
                )*/
                Log.d("Pusher", "Push: $progress")

                lastProgress = progress
            }
        }
        Log.d("Pusher", "Start Writing")

        stream.write(
            ByteUtils.concat(
                "DONE".toByteArray(),
                ByteUtils.intToByteArray(System.currentTimeMillis().toInt())
            )
        )
        Log.d("Pusher", "Done")

        stream.write(ByteUtils.concat("QUIT".toByteArray(), ByteUtils.intToByteArray(0)))
        Log.d("Pusher", "Quited")
    }
}