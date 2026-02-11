package com.cebo.bus.sync

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

/**
 * PayloadCompressor
 *
 * Compresses sync payloads using GZIP to reduce bandwidth usage.
 *
 * Pure utility.
 * No Android dependencies.
 */
class PayloadCompressor {

    /**
     * Compress a UTF-8 string payload using GZIP.
     *
     * @param payload Raw string payload
     * @return Compressed byte array
     */
    fun compress(payload: String): ByteArray {
        if (payload.isEmpty()) return ByteArray(0)

        val byteArrayOutputStream = ByteArrayOutputStream()

        GZIPOutputStream(byteArrayOutputStream).use { gzipStream ->
            val inputBytes = payload.toByteArray(StandardCharsets.UTF_8)
            gzipStream.write(inputBytes)
        }

        return byteArrayOutputStream.toByteArray()
    }
}
