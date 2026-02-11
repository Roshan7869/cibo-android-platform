package com.cebo.bus.gnss

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.cebo.bus.core.model.GeoPoint
import com.google.android.gms.location.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * GnssManager v2
 *
 * Fleet-grade GNSS controller:
 * - Dynamic interval switching
 * - Power-aware priority control
 * - Quality monitoring
 * - Safe restart logic
 */
class GnssManager(
    context: Context,
    private val qualityMonitor: GnssQualityMonitor
) {

    interface Listener {
        fun onLocationUpdate(
            geoPoint: GeoPoint,
            quality: GnssQualityMonitor.QualitySnapshot
        )
    }

    private val appContext = context.applicationContext
    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val listeners = CopyOnWriteArraySet<Listener>()

    @Volatile
    private var currentInterval: Long = 1000L

    @Volatile
    private var isStarted = false

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

        locationRequest = LocationRequest.Builder(
            priority,
            currentInterval
        )
            .setMinUpdateIntervalMillis(currentInterval)
            .setWaitForAccurateLocation(priority == Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    private fun determinePriority(interval: Long): Int {
        return when {
            interval <= 1500L -> Priority.PRIORITY_HIGH_ACCURACY
            interval <= 4000L -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            else -> Priority.PRIORITY_LOW_POWER
        }
    }

    private fun handleLocation(location: Location) {

        val geoPoint = GeoPoint(
            latitude = location.latitude,
            longitude = location.longitude
        )

        val quality = qualityMonitor.update(location)

        for (listener in listeners) {
            try {
                listener.onLocationUpdate(geoPoint, quality)
            } catch (_: Exception) {}
        }
    }
}
