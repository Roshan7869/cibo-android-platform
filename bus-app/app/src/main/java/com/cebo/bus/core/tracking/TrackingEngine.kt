package com.cebo.bus.core.tracking

import com.cebo.bus.core.fusion.ConfidenceCalculator
import com.cebo.bus.core.fusion.FusionEngine
import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.Confidence
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed

/**
 * TrackingEngine
 *
 * Central coordinator of the Core Engine.
 *
 * Responsibilities:
 * - Maintain last known tracking state
 * - Compute time deltas
 * - Orchestrate prediction, fusion, and confidence calculation
 * - Emit immutable TrackingState
 *
 * This class is the ONLY entry point to the core engine.
 */
class TrackingEngine(
    initialPosition: GeoPoint,
    initialSpeed: Speed,
    initialHeading: Heading,
    initialAccuracy: Accuracy,
    initialTimestampMillis: Long
) {

    private var lastState: TrackingState = TrackingState(
        position = initialPosition,
        speed = initialSpeed,
        heading = initialHeading,
        accuracy = initialAccuracy,
        confidence = Confidence.FULL,
        timestampMillis = initialTimestampMillis
    )

    private val fusionEngine = FusionEngine(
        initialPosition = initialPosition,
        initialAccuracy = initialAccuracy
    )

    private val confidenceCalculator = ConfidenceCalculator()

    /**
     * Updates the tracking engine with a new measurement.
     *
     * @param measuredPosition GNSS-measured position
     * @param measuredAccuracy GNSS-reported accuracy
     * @param speed Current speed
     * @param heading Current heading
     * @param timestampMillis Timestamp of measurement (ms)
     *
     * @return Updated TrackingState
     */
    fun update(
        measuredPosition: GeoPoint,
        measuredAccuracy: Accuracy,
        speed: Speed,
        heading: Heading,
        timestampMillis: Long
    ): TrackingState {

        val deltaTimeSeconds =
            ((timestampMillis - lastState.timestampMillis).coerceAtLeast(0L)) / 1000.0

        val (filteredPosition, fusedAccuracy) = fusionEngine.fuse(
            lastKnownPosition = lastState.position,
            speed = speed,
            heading = heading,
            deltaTimeSeconds = deltaTimeSeconds,
            measuredPosition = measuredPosition,
            measuredAccuracy = measuredAccuracy
        )

        val confidence = confidenceCalculator.calculate(
            accuracy = fusedAccuracy,
            secondsSinceLastFix = deltaTimeSeconds
        )

        val newState = TrackingState(
            position = filteredPosition,
            speed = speed,
            heading = heading,
            accuracy = fusedAccuracy,
            confidence = confidence,
            timestampMillis = timestampMillis
        )

        lastState = newState
        return newState
    }

    /**
     * Returns the most recent tracking state.
     */
    fun currentState(): TrackingState =
        lastState
}