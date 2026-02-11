package com.cebo.bus.route

import com.cebo.bus.core.model.GeoPoint
import kotlin.math.*

/**
 * RouteMatcher
 *
 * Responsible for snapping a given position to the closest segment of a route.
 *
 * Pure geometric computation.
 * No Android dependencies.
 */
class RouteMatcher {

    fun match(
        currentPosition: GeoPoint,
        route: Route
    ): RouteMatchResult {

        require(route.points.size >= 2) { "Route must contain at least two points." }

        var minDistance = Double.MAX_VALUE
        var closestPoint = route.points.first()
        var closestSegmentIndex = 0

        for (i in 0 until route.points.size - 1) {
            val a = route.points[i]
            val b = route.points[i + 1]

            val projection = projectPointToSegment(currentPosition, a, b)
            val distance = distanceMeters(currentPosition, projection)

            if (distance < minDistance) {
                minDistance = distance
                closestPoint = projection
                closestSegmentIndex = i
            }
        }

        return RouteMatchResult(
            snappedPoint = closestPoint,
            distanceToRouteMeters = minDistance,
            segmentIndex = closestSegmentIndex
        )
    }

    /**
     * Projects point P onto line segment AB and clamps within segment.
     */
    private fun projectPointToSegment(
        p: GeoPoint,
        a: GeoPoint,
        b: GeoPoint
    ): GeoPoint {

        val ax = a.longitude
        val ay = a.latitude
        val bx = b.longitude
        val by = b.latitude
        val px = p.longitude
        val py = p.latitude

        val dx = bx - ax
        val dy = by - ay

        if (dx == 0.0 && dy == 0.0) return a

        val t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)

        val clampedT = t.coerceIn(0.0, 1.0)

        return GeoPoint(
            latitude = ay + clampedT * dy,
            longitude = ax + clampedT * dx
        )
    }

    /**
     * Calculates distance between two GeoPoints in meters using Haversine formula.
     */
    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val earthRadius = 6371000.0

        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)

        val sinLat = sin(deltaLat / 2)
        val sinLon = sin(deltaLon / 2)

        val c = 2 * atan2(
            sqrt(sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon),
            sqrt(1 - (sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon))
        )

        return earthRadius * c
    }
}

/**
 * Result of route matching.
 */
data class RouteMatchResult(
    val snappedPoint: GeoPoint,
    val distanceToRouteMeters: Double,
    val segmentIndex: Int
)
