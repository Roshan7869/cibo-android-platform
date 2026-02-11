package com.cebo.bus.route

import android.content.Context
import com.cebo.bus.core.model.GeoPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * RouteLoader
 *
 * Responsible for loading route geometry from local assets.
 *
 * - Loads JSON file from assets
 * - Parses into Route model
 * - Caches loaded routes
 *
 * No matching logic.
 * No tracking logic.
 */
class RouteLoader(
    private val context: Context
) {

    private val appContext = context.applicationContext
    private val routeCache = ConcurrentHashMap<String, Route>()

    /**
     * Load route by ID from assets.
     *
     * Expected file name format: routes/<routeId>.json
     */
    fun loadRoute(routeId: String): Route {
        return routeCache[routeId] ?: loadAndCache(routeId)
    }

    private fun loadAndCache(routeId: String): Route {
        val jsonString = readAssetFile("routes/$routeId.json")
        val route = parseRoute(jsonString)
        routeCache[routeId] = route
        return route
    }

    private fun readAssetFile(path: String): String {
        appContext.assets.open(path).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                return reader.readText()
            }
        }
    }

    private fun parseRoute(jsonString: String): Route {
        val jsonObject = JSONObject(jsonString)
        val id = jsonObject.getString("id")
        val pointsArray: JSONArray = jsonObject.getJSONArray("points")

        val points = mutableListOf<GeoPoint>()
        val cumulativeDistances = mutableListOf<Double>()
        var totalDistance = 0.0

        if (pointsArray.length() > 0) {
            // First point
            val firstObj = pointsArray.getJSONObject(0)
            points.add(GeoPoint(firstObj.getDouble("lat"), firstObj.getDouble("lng")))
            cumulativeDistances.add(0.0)

            for (i in 1 until pointsArray.length()) {
                val pointObj = pointsArray.getJSONObject(i)
                val lat = pointObj.getDouble("lat")
                val lng = pointObj.getDouble("lng")
                val currentPoint = GeoPoint(lat, lng)
                
                points.add(currentPoint)
                
                // Calculate distance from previous point
                val prevPoint = points[i - 1]
                val dist = distanceMeters(prevPoint, currentPoint)
                totalDistance += dist
                cumulativeDistances.add(totalDistance)
            }
        }

        return Route(
            id = id,
            points = points,
            cumulativeDistancesMeters = cumulativeDistances,
            totalDistanceMeters = totalDistance
        )
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val earthRadius = 6371000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val aVal = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(aVal), Math.sqrt(1 - aVal))
        return earthRadius * c
    }
}

/**
 * Route Model
 */
data class Route(
    val id: String,
    val points: List<GeoPoint>,
    val cumulativeDistancesMeters: List<Double>,
    val totalDistanceMeters: Double
)
