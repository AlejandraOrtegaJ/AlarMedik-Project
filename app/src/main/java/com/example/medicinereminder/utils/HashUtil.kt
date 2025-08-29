package com.example.medicinereminder.utils

import java.security.MessageDigest

// Utilidad para hashear contraseñas con SHA-256
object HashUtil {
    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}