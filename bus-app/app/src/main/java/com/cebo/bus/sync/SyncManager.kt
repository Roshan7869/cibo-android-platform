package com.cebo.bus.sync

import com.cebo.bus.storage.database.LocationEntity
import com.cebo.bus.storage.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SyncManager
 *
 * Handles offline-first batching and upload pipeline.
 *
 * Responsibilities:
 * - Fetch unsynced locations
 * - Compress payload
 * - Upload (stubbed transport layer)
 * - Mark as synced
 *
 * No direct DAO access.
 * No Android APIs.
 */
class SyncManager(
    private val repository: LocationRepository,
    private val networkMonitor: NetworkMonitor,
    private val compressor: PayloadCompressor,
    private val batchSize: Int = DEFAULT_BATCH_SIZE
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val isSyncing = AtomicBoolean(false)

    /**
     * Called whenever a new tracking state is stored.
     * Triggers background sync attempt if network available.
     */
    fun onNewTrackingState() {
        if (!networkMonitor.isConnected()) return
        attemptSync()
    }

    /**
     * Public manual trigger.
     */
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

    /**
     * Converts entities to raw payload format.
     * Keep minimal and flat for transport.
     */
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

    /**
     * Transport layer placeholder.
     * Replace with Retrofit/WebSocket implementation later.
     */
    private suspend fun upload(data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            // TODO: Implement actual API call
            true
        }
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 50
    }
}
