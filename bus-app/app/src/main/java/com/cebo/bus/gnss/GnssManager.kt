package com.cebo.bus.gnss

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Fleet-grade GNSS controller with adaptive interval + quality monitoring.
 */
class GnssManager(
    context: Context,
    private val qualityMonitor: GnssQualityMonitor
) {

    interface Listener {
        fun onLocationUpdate(
            geoPoint: GeoPoint,
            accuracy: Accuracy,
            speed: Speed,
            heading: Heading,
            timestampMillis: Long,
            quality: GnssQualityMonitor.QualitySnapshot
        )
    }

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val listeners = CopyOnWriteArraySet<Listener>()

    @Volatile
    private var currentInterval: Long = 1000L

    @Volatile
    private var isStarted = false

    private var startTimestampMillis: Long = 0L
    private var lastFixTimestampMillis: Long = 0L
    private var locationRequest: LocationRequest? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleLocation(location)
        }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isStarted) return
        isStarted = true
        startTimestampMillis = System.currentTimeMillis()
        rebuildRequest()
        fusedClient.requestLocationUpdates(
            locationRequest!!,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stop() {
        if (!isStarted) return
        fusedClient.removeLocationUpdates(locationCallback)
        isStarted = false
    }

    @SuppressLint("MissingPermission")
    fun setUpdateInterval(intervalMillis: Long) {
        if (intervalMillis == currentInterval) return
        currentInterval = intervalMillis

        if (isStarted) {
            fusedClient.removeLocationUpdates(locationCallback)
            rebuildRequest()
            fusedClient.requestLocationUpdates(
                locationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun rebuildRequest() {
        val priority = determinePriority(currentInterval)

        locationRequest = LocationRequest.Builder(priority, currentInterval)
            .setMinUpdateIntervalMillis(currentInterval)
            .setWaitForAccurateLocation(priority == Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    private fun determinePriority(interval: Long): Int = when {
        interval <= 1500L -> Priority.PRIORITY_HIGH_ACCURACY
        interval <= 4000L -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        else -> Priority.PRIORITY_LOW_POWER
    }

    private fun handleLocation(location: Location) {
        val now = System.currentTimeMillis()
        if (lastFixTimestampMillis == 0L) {
            qualityMonitor.setLastGoodFixAge(0.0)
        } else {
            val ageSeconds = (now - lastFixTimestampMillis).coerceAtLeast(0L) / 1000.0
            qualityMonitor.setLastGoodFixAge(ageSeconds)
        }
        lastFixTimestampMillis = now

        val statusSnapshot = GnssStatusSnapshot(
            satellitesInView = 0,
            satellitesUsed = 0,
            navicDetected = false,
            constellationCount = emptyMap(),
            medianSnr = 0.0
        )
        qualityMonitor.updateStatus(statusSnapshot, now)
        val quality = qualityMonitor.currentSnapshot()

        val geoPoint = GeoPoint(
            latitude = location.latitude,
            longitude = location.longitude
        )

        val accuracy = Accuracy.fromMeters(location.accuracy.toDouble())
        val speed = Speed.fromMetersPerSecond(location.speed.toDouble().coerceAtLeast(0.0))
        val heading = Heading.fromDegrees(location.bearing.toDouble())
        val timestampMillis = location.time.takeIf { it > 0L } ?: now

        for (listener in listeners) {
            try {
                listener.onLocationUpdate(geoPoint, accuracy, speed, heading, timestampMillis, quality)
            } catch (_: Exception) {
            }
        }
    }
}
