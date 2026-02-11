package com.cebo.bus.core.fusion

/**
 * KalmanFilter
 *
 * Lightweight 2D Kalman filter for geographic position smoothing.
 * State vector: [latitude, longitude]
 *
 * This implementation is intentionally minimal:
 * - No Android dependencies
 * - Tuned for vehicle tracking
 * - Deterministic and reusable
 */
class KalmanFilter(
    initialLatitude: Double,
    initialLongitude: Double,
    initialVariance: Double
) {

    private var lat: Double = initialLatitude
    private var lon: Double = initialLongitude

    // State covariance (P matrix simplified as diagonal)
    private var varianceLat: Double = initialVariance
    private var varianceLon: Double = initialVariance

    // Process noise (model uncertainty)
    private var processNoise: Double = DEFAULT_PROCESS_NOISE

    /**
     * Predict step.
     *
     * Increases uncertainty over time.
     *
     * @param deltaTimeSeconds Time elapsed since last update
     */
    fun predict(deltaTimeSeconds: Double) {
        if (deltaTimeSeconds <= 0.0) return

        val noise = processNoise * deltaTimeSeconds
        varianceLat += noise
        varianceLon += noise
    }

    /**
     * Update step using new measurement.
     *
     * @param measuredLat Measured latitude
     * @param measuredLon Measured longitude
     * @param measurementAccuracyMeters Measurement accuracy (meters)
     */
    fun update(
        measuredLat: Double,
        measuredLon: Double,
        measurementAccuracyMeters: Double
    ) {
        val measurementVariance = accuracyToVariance(measurementAccuracyMeters)

        // Kalman gain
        val gainLat = varianceLat / (varianceLat + measurementVariance)
        val gainLon = varianceLon / (varianceLon + measurementVariance)

        // State update
        lat += gainLat * (measuredLat - lat)
        lon += gainLon * (measuredLon - lon)

        // Variance update
        varianceLat *= (1.0 - gainLat)
        varianceLon *= (1.0 - gainLon)
    }

    /**
     * Returns the filtered latitude.
     */
    fun latitude(): Double = lat

    /**
     * Returns the filtered longitude.
     */
    fun longitude(): Double = lon

    /**
     * Returns combined positional variance.
     */
    fun combinedVariance(): Double =
        varianceLat + varianceLon

    private fun accuracyToVariance(accuracyMeters: Double): Double {
        val clamped = accuracyMeters.coerceAtLeast(MIN_ACCURACY_METERS)
        return clamped * clamped
    }

    companion object {
        private const val DEFAULT_PROCESS_NOISE = 1e-6
        private const val MIN_ACCURACY_METERS = 1.0
    }
}