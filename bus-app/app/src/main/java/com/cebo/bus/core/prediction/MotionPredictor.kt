package com.cebo.bus.core.prediction

import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed
import kotlin.math.cos
import kotlin.math.sin

/**
 * MotionPredictor
 *
 * Predicts short-term future position using a constant-velocity model.
 *
 * This component:
 * - Does NOT use GNSS directly
 * - Does NOT store state
 * - Does NOT apply filtering
 *
 * It exists only to bridge gaps when GNSS is weak or unavailable.
 */
class MotionPredictor {

    /**
     * Predicts the next geographic position.
     *
     * @param lastPosition Last known valid position
     * @param speed Current speed (m/s)
     * @param heading Current heading (degrees)
     * @param deltaTimeSeconds Time elapsed since last position (seconds)
     *
     * @return Predicted GeoPoint
     */
    fun predict(
        lastPosition: GeoPoint,
        speed: Speed,
        heading: Heading,
        deltaTimeSeconds: Double
    ): GeoPoint {

        // Guard conditions — prediction must be safe
        if (deltaTimeSeconds <= 0.0) return lastPosition
        if (!speed.isMoving()) return lastPosition

        val distanceMeters = speed.metersPerSecond * deltaTimeSeconds
        if (distanceMeters <= 0.0) return lastPosition

        val headingRad = heading.toRadians()

        // Vehicle motion decomposition
        val deltaNorth = distanceMeters * cos(headingRad)
        val deltaEast = distanceMeters * sin(headingRad)

        return lastPosition.movedBy(
            deltaNorthMeters = deltaNorth,
            deltaEastMeters = deltaEast
        )
    }
}