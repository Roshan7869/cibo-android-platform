package com.cebo.bus.core.model

/**
 * Accuracy
 *
 * Represents horizontal positional uncertainty in meters.
 *
 * Concept:
 * - Accuracy is a physical uncertainty radius
 * - Smaller value = higher trust in position
 *
 * Design principles:
 * - Immutable
 * - Unit-safe (meters only)
 * - Explicit and always present
 */
@JvmInline
value class Accuracy private constructor(
    val meters: Double
) {

    init {
        require(meters >= 0.0) {
            "Accuracy cannot be negative. Provided: $meters meters"
        }
    }

    /**
     * Returns true if accuracy is considered good enough
     * for reliable tracking and UI display.
     */
    fun isGood(thresholdMeters: Double = DEFAULT_GOOD_ACCURACY_METERS): Boolean =
        meters <= thresholdMeters

    /**
     * Combines two independent accuracy values conservatively.
     *
     * This follows the principle of combining independent uncertainties:
     * sqrt(a^2 + b^2)
     */
    fun combineWith(other: Accuracy): Accuracy =
        fromMeters(
            kotlin.math.sqrt(
                meters * meters + other.meters * other.meters
            )
        )

    companion object {
        private const val DEFAULT_GOOD_ACCURACY_METERS = 20.0

        /**
         * Factory method using meters.
         */
        fun fromMeters(value: Double): Accuracy =
            Accuracy(value)

        /**
         * Represents perfect accuracy (theoretical).
         */
        val ZERO: Accuracy = Accuracy(0.0)

        /**
         * Represents extremely poor accuracy (fallback state).
         */
        val VERY_POOR: Accuracy = Accuracy(10_000.0)
    }
}