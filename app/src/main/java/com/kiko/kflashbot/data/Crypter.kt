package com.kiko.kflashbot.data

import android.content.Context
import android.util.Log
import com.kiko.kadblib.AdbBase64
import com.kiko.kadblib.AdbCrypto
import com.kiko.kflashbot.utils.MyAdbBase64
import java.io.File

class Crypter {
    fun getAdbCrypto(context: Context): AdbCrypto? {
        val base64: AdbBase64 = MyAdbBase64()
        var adbCrypto: AdbCrypto? = null
        try {
            adbCrypto = AdbCrypto.loadAdbKeyPair(
                base64,
                File(context.filesDir, "private_key"),
                File(context.filesDir, "public_key")
            )
        } catch (e: Exception) {
        }

        if (adbCrypto == null) {
            try {
                adbCrypto = AdbCrypto.generateAdbKeyPair(base64)
                adbCrypto.saveAdbKeyPair(
                    File(context.filesDir, "private_key"),
                    File(context.filesDir, "public_key")
                )
            } catch (e: Exception) {
                Log.w(Consts.TAG, "fail to generate and save key-pair", e)
            }
        }
        return adbCrypto
    }
}