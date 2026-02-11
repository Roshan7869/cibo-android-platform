package com.cebo.bus.gnss

import kotlin.math.exp

/**
 * Pure logic GNSS health evaluation engine.
 */
class GnssQualityMonitor(
    private val config: HealthConfig = HealthConfig()
) {

    private val satUsedBuffer = RollingBuffer<Int>(config.rollingWindowSize)
    private val medianSnrBuffer = RollingBuffer<Double>(config.rollingWindowSize)
    private val navicBuffer = RollingBuffer<Boolean>(config.rollingWindowSize)
    private val dualFreqBuffer = RollingBuffer<Boolean>(config.rollingWindowSize)
    private val rawBuffer = RollingBuffer<Boolean>(config.rollingWindowSize)

    private var lastSnapshot: QualitySnapshot = emptySnapshot()
    private var lastGoodFixAgeSeconds: Double = 0.0

    fun updateStatus(status: GnssStatusSnapshot, timestampMillis: Long) {
        satUsedBuffer.add(status.satellitesUsed)
        navicBuffer.add(status.navicDetected)
        medianSnrBuffer.add(status.medianSnr)
        computeSnapshot(status, timestampMillis)
    }

    fun updateMeasurement(measurement: GnssMeasurementSnapshot, timestampMillis: Long) {
        dualFreqBuffer.add(measurement.dualFrequencySupported)
        rawBuffer.add(measurement.rawMeasurementsAvailable)
        computeSnapshot(lastSnapshot.toStatusSnapshot(), timestampMillis)
    }

    fun setLastGoodFixAge(seconds: Double) {
        lastGoodFixAgeSeconds = seconds
        computeSnapshot(lastSnapshot.toStatusSnapshot(), lastSnapshot.timestampMillis)
    }

    fun currentSnapshot(): QualitySnapshot = lastSnapshot

    private fun computeSnapshot(status: GnssStatusSnapshot, timestampMillis: Long) {
        val rollingSatAvg = satUsedBuffer.average()
        val rollingSnrMedian = medianSnrBuffer.median()

        val navicDetected = navicBuffer.fractionTrue() >= config.navicPresenceThreshold
        val dualFreq = dualFreqBuffer.fractionTrue() > 0.0
        val rawAvailable = rawBuffer.fractionTrue() > 0.0

        val satScore = normalize(rollingSatAvg, config.minSatUsed.toDouble(), config.optimalSatUsed.toDouble())
        val snrScore = 1 - exp(-rollingSnrMedian / config.snrDecayFactor)
        val dualScore = if (dualFreq) config.dualFrequencyWeight else 0.0
        val navicScore = if (navicDetected) config.navicBonus else 0.0
        val freshnessPenalty = if (lastGoodFixAgeSeconds > config.staleThresholdSeconds) config.stalePenalty else 0.0

        val healthScore = (
            config.satelliteWeight * satScore +
                config.snrWeight * snrScore +
                dualScore + navicScore - freshnessPenalty
            ).coerceIn(0.0, 1.0)

        val qualified = healthScore >= config.qualificationThreshold &&
            rollingSatAvg >= config.minSatUsed &&
            rollingSnrMedian >= config.minMedianSnr

        lastSnapshot = QualitySnapshot(
            timestampMillis = timestampMillis,
            satellitesInView = status.satellitesInView,
            satellitesUsed = status.satellitesUsed,
            constellationCount = status.constellationCount,
            medianSnr = rollingSnrMedian,
            meanSnr = medianSnrBuffer.average(),
            navicDetected = navicDetected,
            dualFrequencySupported = dualFreq,
            rawMeasurementsAvailable = rawAvailable,
            rollingSatUsedAvg = rollingSatAvg,
            rollingMedianSnr = rollingSnrMedian,
            healthScore = healthScore,
            qualifiedForDeployment = qualified,
            lastGoodFixAgeSeconds = lastGoodFixAgeSeconds
        )
    }

    private fun normalize(value: Double, min: Double, max: Double): Double {
        if (value <= min) return 0.0
        if (value >= max) return 1.0
        return (value - min) / (max - min)
    }

    private fun emptySnapshot(): QualitySnapshot = QualitySnapshot()

    data class HealthConfig(
        val rollingWindowSize: Int = 10,
        val minSatUsed: Int = 3,
        val optimalSatUsed: Int = 8,
        val minMedianSnr: Double = 18.0,
        val snrDecayFactor: Double = 20.0,
        val satelliteWeight: Double = 0.45,
        val snrWeight: Double = 0.35,
        val dualFrequencyWeight: Double = 0.1,
        val navicBonus: Double = 0.05,
        val navicPresenceThreshold: Double = 0.5,
        val staleThresholdSeconds: Double = 30.0,
        val stalePenalty: Double = 0.2,
        val qualificationThreshold: Double = 0.6
    )

    data class QualitySnapshot(
        val timestampMillis: Long = 0L,
        val satellitesInView: Int = 0,
        val satellitesUsed: Int = 0,
        val constellationCount: Map<Int, Int> = emptyMap(),
        val medianSnr: Double = 0.0,
        val meanSnr: Double = 0.0,
        val navicDetected: Boolean = false,
        val dualFrequencySupported: Boolean = false,
        val rawMeasurementsAvailable: Boolean = false,
        val rollingSatUsedAvg: Double = 0.0,
        val rollingMedianSnr: Double = 0.0,
        val healthScore: Double = 0.0,
        val qualifiedForDeployment: Boolean = false,
        val lastGoodFixAgeSeconds: Double = 0.0
    )
}

class RollingBuffer<T : Any>(
    private val capacity: Int
) {
    private val data = ArrayDeque<T>()

    fun add(value: T) {
        if (data.size == capacity) data.removeFirst()
        data.addLast(value)
    }

    fun average(): Double {
        if (data.isEmpty()) return 0.0
        return data.mapNotNull { (it as? Number)?.toDouble() }.average()
    }

    fun median(): Double {
        val nums = data.mapNotNull { (it as? Number)?.toDouble() }
        if (nums.isEmpty()) return 0.0
        val sorted = nums.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }

    fun fractionTrue(): Double {
        if (data.isEmpty()) return 0.0
        val trueCount = data.count { it == true }
        return trueCount.toDouble() / data.size
    }
}

private fun GnssQualityMonitor.QualitySnapshot.toStatusSnapshot(): GnssStatusSnapshot =
    GnssStatusSnapshot(
        satellitesInView = satellitesInView,
        satellitesUsed = satellitesUsed,
        navicDetected = navicDetected,
        constellationCount = constellationCount,
        medianSnr = medianSnr
    )
