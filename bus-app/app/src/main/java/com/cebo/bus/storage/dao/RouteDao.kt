package com.cebo.bus.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cebo.bus.storage.database.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: String): RouteEntity?

    @Query("SELECT * FROM routes")
    fun getAllFlow(): Flow<List<RouteEntity>>
    
    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM routes")
    suspend fun deleteAll()
}
