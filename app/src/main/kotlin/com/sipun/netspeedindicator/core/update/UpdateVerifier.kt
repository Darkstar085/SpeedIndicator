package com.sipun.netspeedindicator.core.update

import java.io.File
import java.security.MessageDigest

object UpdateVerifier {
    fun verifyDigest(file: File, expected: String): Boolean {
        val expectedHash = expected.substringAfter("sha256:", "").trim().lowercase()
        if (expectedHash.length != 64 || expectedHash.any { it !in "0123456789abcdef" }) {
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == expectedHash
    }
}
