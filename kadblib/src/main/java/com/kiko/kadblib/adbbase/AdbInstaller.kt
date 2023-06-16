package com.kiko.kadblib.adbbase

import android.util.Log
import com.kiko.kadblib.results.InstallResult
import com.kiko.kadblib.utils.ByteUtils
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class AdbInstaller {
    fun install(
        adbConnection: AdbConnection?,
        local: File?,
        remotePath: String?,
        installResult: InstallResult
    ) {
        runBlocking {
            val stream = adbConnection!!.open("sync:")
            val sendId = "SEND"
            val mode = ",33206"
            val length = (remotePath + mode).length
            stream.write(ByteUtils.concat(sendId.toByteArray(), ByteUtils.intToByteArray(length)))
            stream.write(remotePath!!.toByteArray())
            stream.write(mode.toByteArray())
            val buff = ByteArray(adbConnection.getMaxData())
            val `is`: InputStream = FileInputStream(local)
            var sent: Long = 0
            val total = local!!.length()
            var lastProgress = 0
            while (true) {
                val read = `is`.read(buff)
                if (read < 0) {
                    break
                }
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

                    lastProgress = progress
                }
            }
            stream.write(
                ByteUtils.concat(
                    "DONE".toByteArray(),
                    ByteUtils.intToByteArray(System.currentTimeMillis().toInt())
                )
            )
            val res = stream.read()
            stream.write(ByteUtils.concat("QUIT".toByteArray(), ByteUtils.intToByteArray(0)))
        }
    }
}