package com.kiko.kadblib

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
