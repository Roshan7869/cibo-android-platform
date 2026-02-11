package com.cebo.bus.gnss

import android.location.Location
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.os.Build
import androidx.annotation.RequiresApi
import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint

/**
 * GnssCallback
 *
 * Thin adapter layer between Android GNSS callbacks and CEBO domain events.
 * This class performs ONLY transformation, no validation, no filtering.
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

        val accuracy = Accuracy.fromMeters(
            location.accuracy.toDouble()
        )

        listener.onLocationMeasured(
            MeasuredLocation(
                position = geoPoint,
                accuracy = accuracy,
                timestampMillis = location.time
            )
        )
    }

    fun onGnssStatusChanged(status: GnssStatus) {
        var satellitesInView = status.satelliteCount
        var satellitesUsed = 0
        var navicDetected = false

        for (i in 0 until status.satelliteCount) {
            if (status.usedInFix(i)) {
                satellitesUsed++
            }
            if (status.constellationType(i) == GnssStatus.CONSTELLATION_IRNSS) {
                navicDetected = true
            }
        }

        listener.onGnssStatusUpdated(
            GnssStatusSnapshot(
                satellitesInView = satellitesInView,
                satellitesUsed = satellitesUsed,
                navicDetected = navicDetected
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

/**
 * Domain snapshots used internally by GNSS layer
 */

data class MeasuredLocation(
    val position: GeoPoint,
    val accuracy: Accuracy,
    val timestampMillis: Long
)

data class GnssStatusSnapshot(
    val satellitesInView: Int,
    val satellitesUsed: Int,
    val navicDetected: Boolean
)

data class GnssMeasurementSnapshot(
    val rawMeasurementsAvailable: Boolean,
    val dualFrequencySupported: Boolean
)