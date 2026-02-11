package com.cebo.bus.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cebo.bus.storage.dao.LocationDao
import com.cebo.bus.storage.dao.RouteDao

/**
 * Central Room database definition for CEBO platform.
 */
@Database(
    entities = [LocationEntity::class, RouteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CeboDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun routeDao(): RouteDao

    companion object {

        @Volatile
        private var INSTANCE: CeboDatabase? = null

        private const val DATABASE_NAME = "cebo_database"

        fun getInstance(context: Context): CeboDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CeboDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
