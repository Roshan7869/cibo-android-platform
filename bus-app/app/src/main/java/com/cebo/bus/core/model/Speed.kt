package com.cebo.bus.core.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Speed
 *
 * Represents scalar speed in meters per second (m/s).
 *
 * Design principles:
 * - Immutable
 * - Unit-safe (internal unit is always m/s)
 * - No direction (handled by Heading)
 * - Safe for prediction and fusion
 */
@JvmInline
value class Speed private constructor(
    val metersPerSecond: Double
) {

    init {
        require(metersPerSecond >= 0.0) {
            "Speed cannot be negative. Provided: $metersPerSecond m/s"
        }
    }

    /**
     * Converts speed to kilometers per hour.
     */
    fun toKilometersPerHour(): Double =
        metersPerSecond * MS_TO_KMH

    /**
     * Returns true if the object is considered moving.
     * Uses a small epsilon to avoid sensor noise issues.
     */
    fun isMoving(): Boolean =
        metersPerSecond > MOVING_EPSILON_MS

    /**
     * Returns a new Speed clamped to a maximum value.
     * Used to avoid unrealistic spikes during prediction.
     */
    fun clamp(maxSpeed: Speed): Speed =
        fromMetersPerSecond(
            minOf(metersPerSecond, maxSpeed.metersPerSecond)
        )

    companion object {
        private const val MS_TO_KMH = 3.6
        private const val MOVING_EPSILON_MS = 0.05 // ~0.18 km/h

        /**
         * Factory method using meters per second.
         */
        fun fromMetersPerSecond(value: Double): Speed =
            Speed(value)

        /**
         * Factory method using kilometers per hour.
         */
        fun fromKilometersPerHour(value: Double): Speed =
            Speed(value / MS_TO_KMH)

        /**
         * Represents zero speed (stationary).
         */
        val ZERO: Speed = Speed(0.0)
    }
}