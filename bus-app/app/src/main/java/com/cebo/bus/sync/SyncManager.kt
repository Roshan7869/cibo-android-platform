package com.cebo.bus.sync

import com.cebo.bus.storage.database.LocationEntity
import com.cebo.bus.storage.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles offline-first batching and upload pipeline.
 */
class SyncManager(
    private val repository: LocationRepository,
    private val networkMonitor: NetworkMonitor,
    private val compressor: PayloadCompressor,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val syncEndpoint: String = DEFAULT_SYNC_ENDPOINT,
    private val apiTokenProvider: () -> String? = { null }
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val isSyncing = AtomicBoolean(false)

    fun onNewTrackingState() {
        if (!networkMonitor.isConnected()) return
        attemptSync()
    }

    fun triggerSync() {
        if (!networkMonitor.isConnected()) return
        attemptSync()
    }

    private fun attemptSync() {
        if (!isSyncing.compareAndSet(false, true)) return

        scope.launch {
            try {
                syncLoop()
            } finally {
                isSyncing.set(false)
            }
        }
    }

    private suspend fun syncLoop() {
        while (networkMonitor.isConnected()) {
            val batch = repository.getUnSyncedLocations(batchSize)
            if (batch.isEmpty()) break

            val payload = buildPayload(batch)
            val compressed = compressor.compress(payload)
            val uploadSuccess = upload(compressed)

            if (!uploadSuccess) break

            val ids = batch.map { it.id }
            repository.markAsSynced(ids)
        }
    }

    private fun buildPayload(batch: List<LocationEntity>): String {
        val builder = StringBuilder()
        batch.forEach { entity ->
            builder.append(
                "${entity.id},${entity.latitude},${entity.longitude}," +
                    "${entity.speedMps},${entity.headingDegrees}," +
                    "${entity.accuracyMeters},${entity.confidence}," +
                    "${entity.timestampMillis}\n"
            )
        }
        return builder.toString()
    }

    private suspend fun upload(data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            val url = URL(syncEndpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "text/plain")
                setRequestProperty("Content-Encoding", "gzip")
                setRequestProperty("Accept", "application/json")
                apiTokenProvider()?.takeIf { it.isNotBlank() }?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }

            try {
                connection.outputStream.use { output: OutputStream ->
                    output.write(data)
                    output.flush()
                }
                val code = connection.responseCode
                code in 200..299
            } catch (_: Exception) {
                false
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 50
        private const val DEFAULT_SYNC_ENDPOINT = "https://example.invalid/telemetry/batch"
    }
}
