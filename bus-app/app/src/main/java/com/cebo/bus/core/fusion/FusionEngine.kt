package com.cebo.bus.core.fusion

import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Speed
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.prediction.MotionPredictor

/**
 * FusionEngine
 *
 * Orchestrates motion prediction and Kalman filtering to produce
 * a stable, filtered geographic position.
 *
 * Responsibilities:
 * - Apply prediction using motion model
 * - Apply correction using Kalman filter
 * - Expose filtered GeoPoint and updated Accuracy
 *
 * This class contains NO Android dependencies.
 */
class FusionEngine(
    initialPosition: GeoPoint,
    initialAccuracy: Accuracy
) {

    private val kalmanFilter = KalmanFilter(
        initialLatitude = initialPosition.latitude,
        initialLongitude = initialPosition.longitude,
        initialVariance = initialAccuracy.meters * initialAccuracy.meters
    )

    private val motionPredictor = MotionPredictor()

    /**
     * Fuses prediction and measurement into a single filtered position.
     *
     * @param lastKnownPosition Last filtered position
     * @param speed Current speed
     * @param heading Current heading
     * @param deltaTimeSeconds Time since last update
     * @param measuredPosition New measured position (GNSS)
     * @param measuredAccuracy Accuracy of measurement
     *
     * @return Pair of filtered GeoPoint and updated Accuracy
     */
    fun fuse(
        lastKnownPosition: GeoPoint,
        speed: Speed,
        heading: Heading,
        deltaTimeSeconds: Double,
        measuredPosition: GeoPoint,
        measuredAccuracy: Accuracy
    ): Pair<GeoPoint, Accuracy> {

        // Prediction step (uncertainty grows with time)
        kalmanFilter.predict(deltaTimeSeconds)

        val predictedPosition = motionPredictor.predict(
            lastPosition = lastKnownPosition,
            speed = speed,
            heading = heading,
            deltaTimeSeconds = deltaTimeSeconds
        )

        // Correction step with measurement
        kalmanFilter.update(
            measuredLat = measuredPosition.latitude,
            measuredLon = measuredPosition.longitude,
            measurementAccuracyMeters = measuredAccuracy.meters
        )

        val filteredPoint = GeoPoint(
            latitude = kalmanFilter.latitude(),
            longitude = kalmanFilter.longitude()
        )

        val fusedAccuracy = Accuracy.fromMeters(
            kotlin.math.sqrt(kalmanFilter.combinedVariance())
        )

        return filteredPoint to fusedAccuracy
    }
}