package com.cebo.bus.gnss

import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint

/**
 * Thin adapter layer between Android GNSS callbacks and CEBO domain events.
 */
class GnssCallback(
    private val listener: Listener
) {

    interface Listener {
        fun onLocationMeasured(measurement: MeasuredLocation)
        fun onGnssStatusUpdated(status: GnssStatusSnapshot)
        fun onGnssMeasurementsUpdated(measurements: GnssMeasurementSnapshot)
    }

    fun onLocation(location: Location) {
        val geoPoint = GeoPoint(
            latitude = location.latitude,
            longitude = location.longitude
        )

        val accuracy = Accuracy.fromMeters(location.accuracy.toDouble())

        listener.onLocationMeasured(
            MeasuredLocation(
                position = geoPoint,
                accuracy = accuracy,
                speedMps = location.speed.toDouble(),
                bearingDegrees = location.bearing.toDouble(),
                timestampMillis = location.time
            )
        )
    }

    fun onGnssStatusChanged(status: GnssStatus) {
        var satellitesUsed = 0
        var navicDetected = false
        val constellationCount = mutableMapOf<Int, Int>()
        val snrValues = mutableListOf<Double>()

        for (i in 0 until status.satelliteCount) {
            if (status.usedInFix(i)) satellitesUsed++
            val constellation = status.constellationType(i)
            constellationCount[constellation] = (constellationCount[constellation] ?: 0) + 1
            if (constellation == GnssStatus.CONSTELLATION_IRNSS) navicDetected = true
            snrValues.add(status.cn0DbHz(i).toDouble())
        }

        val medianSnr = snrValues.sorted().let { sorted ->
            if (sorted.isEmpty()) 0.0
            else if (sorted.size % 2 == 0) (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            else sorted[sorted.size / 2]
        }

        listener.onGnssStatusUpdated(
            GnssStatusSnapshot(
                satellitesInView = status.satelliteCount,
                satellitesUsed = satellitesUsed,
                navicDetected = navicDetected,
                constellationCount = constellationCount,
                medianSnr = medianSnr
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
        var dualFrequency = false

        for (measurement in event.measurements) {
            if (measurement.hasCarrierFrequencyHz()) {
                dualFrequency = true
                break
            }
        }

        listener.onGnssMeasurementsUpdated(
            GnssMeasurementSnapshot(
                rawMeasurementsAvailable = true,
                dualFrequencySupported = dualFrequency
            )
        )
    }
}

data class MeasuredLocation(
    val position: GeoPoint,
    val accuracy: Accuracy,
    val speedMps: Double,
    val bearingDegrees: Double,
    val timestampMillis: Long
)

data class GnssStatusSnapshot(
    val satellitesInView: Int,
    val satellitesUsed: Int,
    val navicDetected: Boolean,
    val constellationCount: Map<Int, Int> = emptyMap(),
    val medianSnr: Double = 0.0
)

data class GnssMeasurementSnapshot(
    val rawMeasurementsAvailable: Boolean,
    val dualFrequencySupported: Boolean
)
