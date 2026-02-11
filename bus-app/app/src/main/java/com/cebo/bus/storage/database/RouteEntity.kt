package com.cebo.bus.storage.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.route.Route
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "routes")
@TypeConverters(RouteConverters::class)
data class RouteEntity(
    @PrimaryKey val id: String,
    val pointsJson: String,
    val cumulativeDistancesJson: String,
    val totalDistanceMeters: Double
) {
    fun toDomain(): Route {
        val gson = Gson()
        val pointsType = object : TypeToken<List<GeoPoint>>() {}.type
        val distancesType = object : TypeToken<List<Double>>() {}.type
        
        return Route(
            id = id,
            points = gson.fromJson(pointsJson, pointsType),
            cumulativeDistancesMeters = gson.fromJson(cumulativeDistancesJson, distancesType),
            totalDistanceMeters = totalDistanceMeters
        )
    }

    companion object {
        fun fromDomain(route: Route): RouteEntity {
            val gson = Gson()
            return RouteEntity(
                id = route.id,
                pointsJson = gson.toJson(route.points),
                cumulativeDistancesJson = gson.toJson(route.cumulativeDistancesMeters),
                totalDistanceMeters = route.totalDistanceMeters
            )
        }
    }
}

class RouteConverters {
    // No explicit converters needed if we store as Json String directly in entity
    // But keeping class for potential expansion
}
