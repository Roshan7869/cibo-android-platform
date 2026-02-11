package com.cebo.bus.service

import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed
import com.cebo.bus.core.tracking.TrackingEngine
import com.cebo.bus.core.tracking.TrackingState
import com.cebo.bus.gnss.GnssManager
import com.cebo.bus.gnss.GnssQualityMonitor
import com.cebo.bus.route.Route
import com.cebo.bus.route.RouteMatcher
import com.cebo.bus.route.RouteValidator
import com.cebo.bus.sensors.SensorManagerWrapper
import com.cebo.bus.sensors.SensorState
import com.cebo.bus.storage.repository.LocationRepository
import com.cebo.bus.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Edge tracking orchestrator:
 * GNSS + adaptive sampling + route snapping + persistence + sync trigger.
 */
class TrackingCoordinator(
    private val gnssManager: GnssManager,
    private val trackingEngine: TrackingEngine,
    private val sensorManager: SensorManagerWrapper,
    private val locationRepository: LocationRepository,
    private val syncManager: SyncManager,
    private val route: Route,
    private val routeMatcher: RouteMatcher,
    private val routeValidator: RouteValidator
) : GnssManager.Listener, SensorManagerWrapper.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var previousSegmentIndex: Int? = null
    private var lastTrackingState: TrackingState? = null
    private var isMoving: Boolean = false

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
        scope.cancel()
    }

    override fun onMotionChanged(moving: Boolean) {
        isMoving = moving
        gnssManager.setUpdateInterval(if (moving) 1000L else 5000L)
    }

    override fun onSensorState(state: SensorState) {
        // ready hook for IMU fusion tuning in future iterations
    }

    override fun onSamplingModeChanged(mode: SensorManagerWrapper.SamplingMode) {
        // optional hook for telemetry
    }

    override fun onLocationUpdate(
        rawPoint: GeoPoint,
        accuracy: Accuracy,
        speed: Speed,
        heading: Heading,
        timestampMillis: Long,
        quality: GnssQualityMonitor.QualitySnapshot
    ) {
        val effectiveAccuracy = if (quality.healthScore < 0.4 && isMoving) {
            Accuracy.fromMeters((accuracy.meters * 2.0).coerceAtLeast(accuracy.meters))
        } else {
            accuracy
        }

        val fusedState = trackingEngine.update(
            measuredPosition = rawPoint,
            measuredAccuracy = effectiveAccuracy,
            speed = speed,
            heading = heading,
            timestampMillis = timestampMillis
        )

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
        scope.launch {
            locationRepository.insertTrackingState(state)
            syncManager.onNewTrackingState()
        }
    }
}
