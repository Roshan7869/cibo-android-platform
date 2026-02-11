package com.cebo.bus.service

import com.cebo.bus.core.fusion.FusionEngine
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.tracking.TrackingEngine
import com.cebo.bus.core.tracking.TrackingState
import com.cebo.bus.gnss.GnssManager
import com.cebo.bus.gnss.GnssQualityMonitor
import com.cebo.bus.route.Route
import com.cebo.bus.route.RouteMatcher
import com.cebo.bus.route.RouteValidator
import com.cebo.bus.sensors.SensorManagerWrapper
import com.cebo.bus.storage.repository.LocationRepository
import com.cebo.bus.sync.SyncManager

/**
 * TrackingCoordinator v2
 *
 * Edge intelligence orchestrator.
 *
 * Responsibilities:
 * - GNSS + IMU integration
 * - Adaptive GNSS control
 * - Route snapping
 * - Route validation
 * - Tunnel fallback
 * - Persistence + sync
 */
class TrackingCoordinator(
    private val gnssManager: GnssManager,
    private val trackingEngine: TrackingEngine,
    private val fusionEngine: FusionEngine,
    private val sensorManager: SensorManagerWrapper,
    private val locationRepository: LocationRepository,
    private val syncManager: SyncManager,
    private val route: Route,
    private val routeMatcher: RouteMatcher,
    private val routeValidator: RouteValidator
) : GnssManager.Listener,
    SensorManagerWrapper.Listener {

    private var previousSegmentIndex: Int? = null
    private var lastTrackingState: TrackingState? = null
    private var isMoving: Boolean = false
    private var tunnelMode: Boolean = false

    fun start() {
        sensorManager.addListener(this)
        sensorManager.start()
        gnssManager.addListener(this)
        gnssManager.start()
    }

    fun stop() {
        gnssManager.removeListener(this)
        gnssManager.stop()
        sensorManager.removeListener(this)
        sensorManager.stop()
    }

    // -------------------------
    // Sensor callbacks
    // -------------------------

    override fun onMotionChanged(moving: Boolean) {
        isMoving = moving

        if (moving) {
            gnssManager.setUpdateInterval(1000L) // 1 second
        } else {
            gnssManager.setUpdateInterval(5000L) // 5 seconds
        }
    }

    override fun onSensorState(state: com.cebo.bus.sensors.SensorState) {
        // Future: integrate IMU into fusion
    }

    override fun onSamplingModeChanged(mode: SensorManagerWrapper.SamplingMode) {
        // Optional: logging or metrics
    }

    // -------------------------
    // GNSS callbacks
    // -------------------------

    override fun onLocationUpdate(rawPoint: GeoPoint, quality: GnssQualityMonitor.QualitySnapshot) {

        tunnelMode = quality.healthScore < 0.4 && isMoving

        val fusedState = if (!tunnelMode) {
            trackingEngine.update(rawPoint)
        } else {
            lastTrackingState?.let {
                fusionEngine.predict(it)
            }
        }

        fusedState ?: return

        val match = routeMatcher.match(fusedState.position, route)
        val validation = routeValidator.validate(match, previousSegmentIndex)

        previousSegmentIndex = match.segmentIndex

        val enhancedState = fusedState.copy(
            position = match.snappedPoint,
            confidence = fusedState.confidence.adjustForRoute(validation.isDeviation),
            timestampMillis = System.currentTimeMillis()
        )

        lastTrackingState = enhancedState

        persist(enhancedState)
    }

    private fun persist(state: TrackingState) {
        // Non-blocking persistence
        Thread {
            locationRepository.insertTrackingState(state)
            syncManager.onNewTrackingState()
        }.start()
    }
}
