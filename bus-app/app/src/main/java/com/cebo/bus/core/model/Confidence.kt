package com.cebo.bus.core.model

/**
 * Confidence
 *
 * Represents normalized trust level of the tracking state.
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

    fun isHigh(): Boolean = value >= HIGH_CONFIDENCE_THRESHOLD

    fun isMedium(): Boolean = value in MEDIUM_CONFIDENCE_RANGE

    fun isLow(): Boolean = value < LOW_CONFIDENCE_THRESHOLD

    fun adjustForRoute(isDeviation: Boolean): Confidence {
        val adjusted = if (isDeviation) value * DEVIATION_PENALTY else value
        return fromNormalized(adjusted.coerceIn(0.0, 1.0))
    }

    companion object {
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.75
        private const val LOW_CONFIDENCE_THRESHOLD = 0.4
        private val MEDIUM_CONFIDENCE_RANGE = LOW_CONFIDENCE_THRESHOLD..HIGH_CONFIDENCE_THRESHOLD
        private const val DEVIATION_PENALTY = 0.6

        fun fromNormalized(value: Double): Confidence = Confidence(value)

        val NONE: Confidence = Confidence(0.0)
        val FULL: Confidence = Confidence(1.0)
    }
}
