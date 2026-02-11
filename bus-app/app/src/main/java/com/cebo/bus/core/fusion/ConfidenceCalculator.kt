package com.cebo.bus.core.fusion

import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.Confidence
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * ConfidenceCalculator
 *
 * Converts physical uncertainty and temporal stability into
 * a normalized confidence value [0.0 – 1.0].
 *
 * This class contains NO Android or GNSS dependencies.
 */
class ConfidenceCalculator {

    /**
     * Calculates confidence based on accuracy and signal freshness.
     *
     * @param accuracy Current positional accuracy
     * @param secondsSinceLastFix Time since last reliable GNSS fix
     *
     * @return Confidence value
     */
    fun calculate(
        accuracy: Accuracy,
        secondsSinceLastFix: Double
    ): Confidence {

        val accuracyScore = accuracyComponent(accuracy.meters)
        val freshnessScore = freshnessComponent(secondsSinceLastFix)

        val combined = accuracyScore * freshnessScore
        return Confidence.fromNormalized(combined.coerceIn(0.0, 1.0))
    }

    private fun accuracyComponent(accuracyMeters: Double): Double {
        val clamped = max(accuracyMeters, MIN_ACCURACY_METERS)
        return exp(-clamped / ACCURACY_DECAY_FACTOR)
    }

    private fun freshnessComponent(seconds: Double): Double {
        if (seconds <= 0.0) return 1.0
        val clamped = min(seconds, MAX_STALE_SECONDS)
        return exp(-clamped / TIME_DECAY_FACTOR)
    }

    companion object {
        private const val MIN_ACCURACY_METERS = 1.0
        private const val ACCURACY_DECAY_FACTOR = 25.0
        private const val TIME_DECAY_FACTOR = 30.0
        private const val MAX_STALE_SECONDS = 120.0
    }
}