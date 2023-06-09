package com.kiko.kadblib

import android.util.Base64

/**
 * This interface specifies the required functions for AdbCrypto to
 * perform Base64 encoding of its public key.
 *
 * @author Cameron Gutman
 */
interface AdbBase64 {
    /**
     * This function must encoded the specified data as a base 64 string, without
     * appending any extra newlines or other characters.
     *
     * @param data Data to encode
     * @return String containing base 64 encoded data
     */
    fun encodeToString(data: ByteArray?): String?
}

class StandardAdbBase64 : AdbBase64 {
    override fun encodeToString(data: ByteArray?): String? {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }
}