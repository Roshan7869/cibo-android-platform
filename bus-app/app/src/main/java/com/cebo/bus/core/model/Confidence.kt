package com.cebo.bus.core.model

/**
 * Confidence
 *
 * Represents normalized trust level of the tracking state.
 *
 * Range:
 * - 0.0 → no confidence
 * - 1.0 → maximum confidence
 *
 * Design principles:
 * - Immutable
 * - Normalized
 * - Human-centric abstraction
 */
@JvmInline
value class Confidence private constructor(
    val value: Double
) {

    init {
        require(value in 0.0..1.0) {
            "Confidence must be between 0.0 and 1.0. Provided: $value"
        }
    }

    /**
     * Returns true if confidence is considered high.
     * Suitable for strong UI indication (green state).
     */
    fun isHigh(): Boolean =
        value >= HIGH_CONFIDENCE_THRESHOLD

    /**
     * Returns true if confidence is considered medium.
     * Tracking is usable but not perfect.
     */
    fun isMedium(): Boolean =
        value in MEDIUM_CONFIDENCE_RANGE

    /**
     * Returns true if confidence is considered low.
     * UI should warn the user.
     */
    fun isLow(): Boolean =
        value < LOW_CONFIDENCE_THRESHOLD

    companion object {

        private const val HIGH_CONFIDENCE_THRESHOLD = 0.75
        private const val LOW_CONFIDENCE_THRESHOLD = 0.4
        private val MEDIUM_CONFIDENCE_RANGE = LOW_CONFIDENCE_THRESHOLD..HIGH_CONFIDENCE_THRESHOLD

        /**
         * Factory method for normalized confidence value.
         */
        fun fromNormalized(value: Double): Confidence =
            Confidence(value)

        /**
         * Represents no confidence.
         */
        val NONE: Confidence = Confidence(0.0)

        /**
         * Represents full confidence.
         */
        val FULL: Confidence = Confidence(1.0)
    }
}