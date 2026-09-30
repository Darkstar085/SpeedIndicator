package com.sipun.netspeedindicator.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateVerifierTest {
    @Test
    fun verifyDigestAcceptsMatchingSha256Digest() {
        val file = File.createTempFile("update", ".apk")
        try {
            file.writeText("Speed Indicator update")
            assertTrue(
                UpdateVerifier.verifyDigest(
                    file,
                    "sha256:006b0d66531d95347dce284f9e7824505d111cd1f7b049c3516cb6b4a58c8ab0"
                )
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun verifyDigestRejectsInvalidOrMismatchedDigest() {
        val file = File.createTempFile("update", ".apk")
        try {
            file.writeText("Speed Indicator update")
            assertFalse(UpdateVerifier.verifyDigest(file, "sha256:invalid"))
            assertFalse(
                UpdateVerifier.verifyDigest(
                    file,
                    "sha256:0000000000000000000000000000000000000000000000000000000000000000"
                )
            )
        } finally {
            file.delete()
        }
    }
}
