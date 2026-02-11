package com.cebo.bus.route

/**
 * RouteValidator
 *
 * Responsible for validating whether a matched position
 * is considered on-route, deviated, or reversing.
 *
 * Pure rule-based logic.
 * No Android dependencies.
 */
class RouteValidator(
    private val config: ValidationConfig = ValidationConfig()
) {

    /**
     * Validate current route match against previous segment state.
     *
     * @param matchResult Current match result from RouteMatcher
     * @param previousSegmentIndex Previously matched segment index (nullable for first sample)
     */
    fun validate(
        matchResult: RouteMatchResult,
        previousSegmentIndex: Int?
    ): RouteValidationResult {

        val isDeviation =
            matchResult.distanceToRouteMeters > config.maxDeviationMeters

        val isOnRoute = !isDeviation

        val isReverseMovement = previousSegmentIndex?.let {
            val delta = matchResult.segmentIndex - it
            delta < -config.reverseToleranceSegments
        } ?: false

        return RouteValidationResult(
            isOnRoute = isOnRoute,
            isDeviation = isDeviation,
            isReverseMovement = isReverseMovement
        )
    }

    /**
     * Configuration for validation thresholds.
     */
    data class ValidationConfig(
        val maxDeviationMeters: Double = 30.0,
        val reverseToleranceSegments: Int = 3
    )
}

/**
 * Result of route validation.
 */
data class RouteValidationResult(
    val isOnRoute: Boolean,
    val isDeviation: Boolean,
    val isReverseMovement: Boolean
)
