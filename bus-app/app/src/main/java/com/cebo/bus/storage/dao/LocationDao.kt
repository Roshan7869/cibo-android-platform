package com.cebo.bus.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cebo.bus.storage.database.LocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * LocationDao
 *
 * Defines all database operations for the "locations" table.
 *
 * No business logic.
 * No conversion logic.
 * Pure persistence contract.
 */
@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocationEntity>)

    @Query(
        """
        SELECT * FROM locations
        WHERE synced = 0
        ORDER BY timestampMillis ASC
        LIMIT :limit
        """
    )
    suspend fun getUnSynced(limit: Int): List<LocationEntity>

    @Query(
        """
        UPDATE locations
        SET synced = 1
        WHERE id IN (:ids)
        """
    )
    suspend fun markAsSynced(ids: List<Long>)

    @Query(
        """
        SELECT * FROM locations
        ORDER BY timestampMillis DESC
        LIMIT 1
        """
    )
    fun observeLatest(): Flow<LocationEntity?>

    @Query(
        """
        DELETE FROM locations
        WHERE timestampMillis < :cutoff
        """
    )
    suspend fun deleteOlderThan(cutoff: Long)
}
