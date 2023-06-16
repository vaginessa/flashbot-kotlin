package com.kiko.kadblib.adbbase

import java.io.Closeable
import java.io.IOException

/**
 * Created by xudong on 2/21/14.
 */
interface AdbChannel : Closeable {
    @Throws(IOException::class)
    fun readx(buffer: ByteArray?, length: Int)

    @Throws(IOException::class)
    fun writex(message: AdbMessage?)
}
