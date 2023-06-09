package com.kiko.kadblib

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * This class encapsulates the ADB cryptography functions and provides
 * an interface for the storage and retrieval of keys.
 * @author Cameron Gutman
 */
class AdbCrypto {
    /** An RSA keypair encapsulated by the AdbCrypto object  */
    private var keyPair: KeyPair? = null

    /** The base 64 conversion interface to use  */
    private var base64: AdbBase64? = null

    /**
     * Signs the ADB SHA1 payload with the private key of this object.
     * @param payload SHA1 payload to sign
     * @return Signed SHA1 payload
     * @throws java.security.GeneralSecurityException If signing fails
     */
    @Throws(GeneralSecurityException::class)
    fun signAdbTokenPayload(payload: ByteArray?): ByteArray {
        val c = Cipher.getInstance("RSA/ECB/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, keyPair!!.private)
        c.update(SIGNATURE_PADDING)
        return c.doFinal(payload)
    }

    @get:Throws(IOException::class)
    val adbPublicKeyPayload: ByteArray
        /**
         * Gets the RSA public key in ADB format.
         * @return Byte array containing the RSA public key in ADB format.
         * @throws java.io.IOException If the key cannot be retrived
         */
        get() {
            val convertedKey = convertRsaPublicKeyToAdbFormat(
                keyPair!!.public as RSAPublicKey
            )
            val keyString = StringBuilder(720)

            /* The key is base64 encoded with a user@host suffix and terminated with a NUL */keyString.append(
                base64!!.encodeToString(convertedKey)
            )
            keyString.append(" unknown@unknown")
            keyString.append('\u0000')
            return keyString.toString().toByteArray(charset("UTF-8"))
        }

    /**
     * Saves the AdbCrypto's key pair to the specified files.
     * @param privateKey The file to store the encoded private key
     * @param publicKey The file to store the encoded public key
     * @throws java.io.IOException If the files cannot be written
     */
    @Throws(IOException::class)
    fun saveAdbKeyPair(privateKey: File?, publicKey: File?) {
        val privOut = FileOutputStream(privateKey)
        val pubOut = FileOutputStream(publicKey)
        privOut.write(keyPair!!.private.encoded)
        pubOut.write(keyPair!!.public.encoded)
        privOut.close()
        pubOut.close()
    }

    companion object {
        /** The ADB RSA key length in bits  */
        const val KEY_LENGTH_BITS = 2048

        /** The ADB RSA key length in bytes  */
        const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8

        /** The ADB RSA key length in words  */
        const val KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4

        /** The RSA signature padding as an int array  */
        val SIGNATURE_PADDING_AS_INT = intArrayOf(
            0x00, 0x01, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00,
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00,
            0x04, 0x14
        )

        /** The RSA signature padding as a byte array  */
        var SIGNATURE_PADDING: ByteArray

        init {
            SIGNATURE_PADDING = ByteArray(SIGNATURE_PADDING_AS_INT.size)
            for (i in SIGNATURE_PADDING.indices) SIGNATURE_PADDING[i] =
                SIGNATURE_PADDING_AS_INT[i].toByte()
        }

        /**
         * Converts a standard RSAPublicKey object to the special ADB format
         * @param pubkey RSAPublicKey object to convert
         * @return Byte array containing the converted RSAPublicKey object
         */
        private fun convertRsaPublicKeyToAdbFormat(pubkey: RSAPublicKey): ByteArray {
            /*
		 * ADB literally just saves the RSAPublicKey struct to a file.
		 * 
		 * typedef struct RSAPublicKey {
         * int len; // Length of n[] in number of uint32_t
         * uint32_t n0inv;  // -1 / n[0] mod 2^32
         * uint32_t n[RSANUMWORDS]; // modulus as little endian array
         * uint32_t rr[RSANUMWORDS]; // R^2 as little endian array
         * int exponent; // 3 or 65537
         * } RSAPublicKey;
		 */

            /* ------ This part is a Java-ified version of RSA_to_RSAPublicKey from adb_host_auth.c ------ */
            val r32: BigInteger
            val r: BigInteger
            var rr: BigInteger
            var rem: BigInteger
            var n: BigInteger
            val n0inv: BigInteger
            r32 = BigInteger.ZERO.setBit(32)
            n = pubkey.modulus
            r = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32)
            rr = r.modPow(BigInteger.valueOf(2), n)
            rem = n.remainder(r32)
            n0inv = rem.modInverse(r32)
            val myN = IntArray(KEY_LENGTH_WORDS)
            val myRr = IntArray(KEY_LENGTH_WORDS)
            var res: Array<BigInteger>
            for (i in 0 until KEY_LENGTH_WORDS) {
                res = rr.divideAndRemainder(r32)
                rr = res[0]
                rem = res[1]
                myRr[i] = rem.toInt()
                res = n.divideAndRemainder(r32)
                n = res[0]
                rem = res[1]
                myN[i] = rem.toInt()
            }

            /* ------------------------------------------------------------------------------------------- */
            val bbuf = ByteBuffer.allocate(524).order(
                ByteOrder.LITTLE_ENDIAN
            )
            bbuf.putInt(KEY_LENGTH_WORDS)
            bbuf.putInt(n0inv.negate().toInt())
            for (i in myN) bbuf.putInt(i)
            for (i in myRr) bbuf.putInt(i)
            bbuf.putInt(pubkey.publicExponent.toInt())
            return bbuf.array()
        }

        /**
         * Creates a new AdbCrypto object from a key pair loaded from files.
         * @param base64 Implementation of base 64 conversion interface required by ADB
         * @param privateKey File containing the RSA private key
         * @param publicKey File containing the RSA public key
         * @return New AdbCrypto object
         * @throws java.io.IOException If the files cannot be read
         * @throws java.security.NoSuchAlgorithmException If an RSA key factory cannot be found
         * @throws java.security.spec.InvalidKeySpecException If a PKCS8 or X509 key spec cannot be found
         */
        @Throws(IOException::class, NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun loadAdbKeyPair(base64: AdbBase64?, privateKey: File, publicKey: File): AdbCrypto {
            val crypto = AdbCrypto()
            val privKeyLength = privateKey.length().toInt()
            val pubKeyLength = publicKey.length().toInt()
            val privKeyBytes = ByteArray(privKeyLength)
            val pubKeyBytes = ByteArray(pubKeyLength)
            val privIn = FileInputStream(privateKey)
            val pubIn = FileInputStream(publicKey)
            privIn.read(privKeyBytes)
            pubIn.read(pubKeyBytes)
            privIn.close()
            pubIn.close()
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privKeyBytes)
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(pubKeyBytes)
            crypto.keyPair = KeyPair(
                keyFactory.generatePublic(publicKeySpec),
                keyFactory.generatePrivate(privateKeySpec)
            )
            crypto.base64 = base64
            return crypto
        }

        /**
         * Creates a new AdbCrypto object by generating a new key pair.
         * @param base64 Implementation of base 64 conversion interface required by ADB
         * @return A new AdbCrypto object
         * @throws java.security.NoSuchAlgorithmException If an RSA key factory cannot be found
         */
        @Throws(NoSuchAlgorithmException::class)
        fun generateAdbKeyPair(base64: AdbBase64?): AdbCrypto {
            val crypto = AdbCrypto()
            val rsaKeyPg = KeyPairGenerator.getInstance("RSA")
            rsaKeyPg.initialize(KEY_LENGTH_BITS)
            crypto.keyPair = rsaKeyPg.genKeyPair()
            crypto.base64 = base64
            return crypto
        }
    }
}
