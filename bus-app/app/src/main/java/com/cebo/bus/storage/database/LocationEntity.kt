package com.cebo.bus.storage.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * LocationEntity
 *
 * Persistent representation of a TrackingState snapshot.
 * This entity is designed for:
 * - Offline-first storage
 * - Efficient sync batching
 * - Fast time-based pruning
 *
 * No business logic.
 * No Android-specific types.
 */
@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["synced"])
    ]
)
data class LocationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val latitude: Double,
    val longitude: Double,

    val speedMps: Double,
    val headingDegrees: Double,

    val accuracyMeters: Double,
    val confidence: Double,

    val timestampMillis: Long,

    val synced: Boolean = false,

    val createdAt: Long
)
