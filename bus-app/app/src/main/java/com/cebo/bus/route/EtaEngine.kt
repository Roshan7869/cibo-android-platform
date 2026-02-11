package com.cebo.bus.route

import com.cebo.bus.core.model.GeoPoint
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.atan2

/**
 * EtaEngine
 *
 * High-performance ETA calculator.
 *
 * Uses precomputed cumulative route distances
 * to compute remaining distance in O(1).
 */
class EtaEngine(
    private val minimumOperationalSpeedMps: Double = 1.5 // fallback if bus slow/stopped
) {

    data class EtaResult(
        val distanceRemainingMeters: Double,
        val etaSeconds: Long
    )

    /**
     * Calculate ETA from current route position.
     *
     * @param matchResult Snapped route match result
     * @param speedMps Current speed in meters per second
     * @param route Route with cumulative distances
     */
    fun calculate(
        matchResult: RouteMatchResult,
        speedMps: Double,
        route: Route
    ): EtaResult {

        val segmentIndex = matchResult.segmentIndex

        if (segmentIndex >= route.cumulativeDistancesMeters.lastIndex) {
            return EtaResult(0.0, 0L)
        }

        val totalDistance = route.totalDistanceMeters
        // Distance from start of route to start of current segment
        val distanceToSegmentStart = route.cumulativeDistancesMeters[segmentIndex]
        
        // Distance from start of current segment to current snapped position
        val segmentStartPoint = route.points[segmentIndex]
        val distanceAlongSegment = distanceMeters(segmentStartPoint, matchResult.snappedPoint)

        val distanceTraveled = distanceToSegmentStart + distanceAlongSegment
        val distanceRemaining = max(0.0, totalDistance - distanceTraveled)

        val effectiveSpeed = max(speedMps, minimumOperationalSpeedMps)

        val etaSeconds = (distanceRemaining / effectiveSpeed).toLong()

        return EtaResult(
            distanceRemainingMeters = distanceRemaining,
            etaSeconds = etaSeconds
        )
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val earthRadius = 6371000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val aVal = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(aVal), sqrt(1 - aVal))
        return earthRadius * c
    }
}
