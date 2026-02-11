package com.cebo.bus.core.model

import kotlin.math.*

/**
 * GeoPoint
 *
 * Represents an immutable geographic coordinate in WGS84 format.
 *
 * Design principles:
 * - Pure domain model (no Android dependency)
 * - Immutable
 * - Validated at creation time
 * - Safe for use in prediction, fusion, and tracking layers
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {

    init {
        require(latitude in LATITUDE_RANGE) {
            "Latitude must be between -90.0 and +90.0 degrees. Provided: $latitude"
        }
        require(longitude in LONGITUDE_RANGE) {
            "Longitude must be between -180.0 and +180.0 degrees. Provided: $longitude"
        }
    }

    /**
     * Calculates distance to another GeoPoint using the Haversine formula.
     *
     * @return distance in meters
     */
    fun distanceTo(other: GeoPoint): Double {
        val lat1Rad = latitude.toRadians()
        val lat2Rad = other.latitude.toRadians()
        val deltaLat = (other.latitude - latitude).toRadians()
        val deltaLon = (other.longitude - longitude).toRadians()

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Creates a new GeoPoint moved by small deltas in meters.
     * Used in motion prediction and dead reckoning.
     *
     * @param deltaNorthMeters movement towards north (+) or south (-)
     * @param deltaEastMeters movement towards east (+) or west (-)
     */
    fun movedBy(deltaNorthMeters: Double, deltaEastMeters: Double): GeoPoint {
        val deltaLat = deltaNorthMeters / EARTH_RADIUS_METERS
        val deltaLon = deltaEastMeters / (EARTH_RADIUS_METERS * cos(latitude.toRadians()))

        val newLat = latitude + deltaLat.toDegrees()
        val newLon = longitude + deltaLon.toDegrees()

        return GeoPoint(newLat, newLon)
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0
    }
}

/**
 * Extension functions kept private to the domain layer.
 */
private fun Double.toRadians(): Double = Math.toRadians(this)
private fun Double.toDegrees(): Double = Math.toDegrees(this)