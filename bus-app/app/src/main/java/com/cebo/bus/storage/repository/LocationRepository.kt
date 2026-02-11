package com.cebo.bus.storage.repository

import com.cebo.bus.core.tracking.TrackingState
import com.cebo.bus.storage.dao.LocationDao
import com.cebo.bus.storage.database.LocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * LocationRepository
 *
 * Bridge between Core TrackingState and persistent LocationEntity.
 *
 * Responsibilities:
 * - Convert TrackingState → LocationEntity
 * - Delegate persistence operations to DAO
 * - Provide sync batching support
 * - Provide latest location observation
 *
 * No Android dependencies.
 * No Room annotations here.
 */
class LocationRepository(
    private val dao: LocationDao
) {

    /**
     * Insert a new tracking snapshot into database.
     */
    suspend fun insertTrackingState(state: TrackingState) {
        dao.insert(state.toEntity())
    }

    /**
     * Retrieve unsynced records in ascending timestamp order.
     */
    suspend fun getUnSyncedLocations(limit: Int): List<LocationEntity> {
        return dao.getUnSynced(limit)
    }

    /**
     * Mark records as synced after successful upload.
     */
    suspend fun markAsSynced(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.markAsSynced(ids)
    }

    /**
     * Observe the most recent location entry.
     */
    fun observeLatestLocation(): Flow<LocationEntity?> {
        return dao.observeLatest()
    }

    /**
     * Delete records older than cutoff timestamp.
     */
    suspend fun deleteOldLocations(cutoffMillis: Long) {
        dao.deleteOlderThan(cutoffMillis)
    }

    /**
     * Conversion: TrackingState → LocationEntity
     */
    private fun TrackingState.toEntity(): LocationEntity {
        return LocationEntity(
            latitude = position.latitude,
            longitude = position.longitude,
            speedMps = speed.metersPerSecond,
            headingDegrees = heading.degrees,
            accuracyMeters = accuracy.meters,
            confidence = confidence.value,
            timestampMillis = timestampMillis,
            synced = false,
            createdAt = System.currentTimeMillis()
        )
    }
}
