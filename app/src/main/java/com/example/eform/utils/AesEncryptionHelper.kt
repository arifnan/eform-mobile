package com.example.eform.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesEncryptionHelper {
    private const val SECRET_KEY = "1234567890abcdef" // 16 karakter = 128 bit
    private const val INIT_VECTOR = "abcdef9876543210"

    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key: SecretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        val iv = IvParameterSpec(INIT_VECTOR.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key: SecretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        val iv = IvParameterSpec(INIT_VECTOR.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
        return String(original)
    }
}