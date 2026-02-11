package com.cebo.bus.core.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Heading
 *
 * Represents direction of movement in degrees.
 *
 * Domain rules:
 * - Range is always [0, 360)
 * - Circular arithmetic (wrap-around safe)
 * - Immutable and unit-safe
 */
@JvmInline
value class Heading private constructor(
    val degrees: Double
) {

    init {
        require(degrees in 0.0..<360.0) {
            "Heading must be in range [0, 360). Provided: $degrees"
        }
    }

    /**
     * Converts heading to radians.
     * Used for trigonometric motion prediction.
     */
    fun toRadians(): Double =
        Math.toRadians(degrees)

    /**
     * Computes the smallest angular difference to another heading.
     * Result is always in range [-180, +180].
     */
    fun deltaTo(other: Heading): Double {
        val diff = other.degrees - degrees
        return ((diff + 540) % 360) - 180
    }

    /**
     * Smoothly interpolates between two headings.
     * Alpha must be in range [0.0, 1.0].
     */
    fun interpolateTo(other: Heading, alpha: Double): Heading {
        require(alpha in 0.0..1.0) {
            "Interpolation alpha must be between 0.0 and 1.0"
        }

        val delta = deltaTo(other)
        return fromDegrees(degrees + delta * alpha)
    }

    companion object {

        /**
         * Factory method with automatic normalization.
         */
        fun fromDegrees(value: Double): Heading {
            val normalized = ((value % 360) + 360) % 360
            return Heading(normalized)
        }

        /**
         * Computes heading from movement vector.
         * deltaX = east movement, deltaY = north movement.
         */
        fun fromVector(deltaEast: Double, deltaNorth: Double): Heading {
            val angleRad = atan2(deltaEast, deltaNorth)
            val angleDeg = Math.toDegrees(angleRad)
            return fromDegrees(angleDeg)
        }

        /**
         * Default heading (north).
         */
        val NORTH: Heading = Heading(0.0)
    }
}