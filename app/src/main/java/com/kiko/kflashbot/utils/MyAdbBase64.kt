package com.kiko.kflashbot.utils

import android.util.Base64
import com.kiko.kadblib.AdbBase64

/**
 * Created by xudong on 2/28/14.
 */
class MyAdbBase64 : AdbBase64 {
    override fun encodeToString(data: ByteArray?): String? {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }
}
