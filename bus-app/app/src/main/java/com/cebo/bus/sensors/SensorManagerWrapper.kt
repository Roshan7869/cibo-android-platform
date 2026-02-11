package com.cebo.bus.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.sqrt

/**
 * High-performance SensorManagerWrapper
 *
 * Optimized for:
 * - Low allocation
 * - Minimal branching
 * - Deterministic motion detection
 * - Adaptive sampling
 *
 * No Android main-thread usage.
 */
class SensorManagerWrapper(
    context: Context,
    private val autoAdaptive: Boolean = true
) {

    enum class SamplingMode { HIGH, NORMAL, LOW, OFF }

    interface Listener {
        fun onSensorState(state: SensorState)
        fun onMotionChanged(isMoving: Boolean)
        fun onSamplingModeChanged(mode: SamplingMode)
    }

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val linearAccelSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val accelSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gyroSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val listeners = CopyOnWriteArraySet<Listener>()

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile
    private var samplingMode = SamplingMode.NORMAL

    @Volatile
    private var isMoving = false

    private var aboveThresholdCount = 0
    private var belowThresholdCount = 0

    private var smoothedAccel = 0.0

    private val alpha = 0.25
    private val motionStartThreshold = 0.6
    private val motionStopThreshold = 0.25
    private val startSamples = 3
    private val stopSamples = 35

    private val highRateUs = 20000   // 50Hz
    private val normalRateUs = 40000 // 25Hz
    private val lowRateUs = 200000   // 5Hz

    private val sensorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {

            val now = System.currentTimeMillis()

            when (event.sensor.type) {

                Sensor.TYPE_LINEAR_ACCELERATION,
                Sensor.TYPE_ACCELEROMETER -> {

                    val ax = event.values[0].toDouble()
                    val ay = event.values[1].toDouble()
                    val az = event.values[2].toDouble()

                    val mag = sqrt(ax * ax + ay * ay + az * az)

                    smoothedAccel = alpha * mag + (1.0 - alpha) * smoothedAccel

                    if (smoothedAccel >= motionStartThreshold) {
                        aboveThresholdCount++
                        belowThresholdCount = 0
                    } else if (smoothedAccel <= motionStopThreshold) {
                        belowThresholdCount++
                        aboveThresholdCount = 0
                    }

                    if (!isMoving && aboveThresholdCount >= startSamples) {
                        isMoving = true
                        notifyMotionChanged(true)
                        if (autoAdaptive) setSamplingMode(SamplingMode.HIGH)
                    }

                    if (isMoving && belowThresholdCount >= stopSamples) {
                        isMoving = false
                        notifyMotionChanged(false)
                        if (autoAdaptive) setSamplingMode(SamplingMode.LOW)
                    }

                    val state = SensorState(
                        accelerationX = ax,
                        accelerationY = ay,
                        accelerationZ = az,
                        rotationX = 0.0,
                        rotationY = 0.0,
                        rotationZ = 0.0,
                        timestampMillis = now
                    )

                    dispatchState(state)
                }

                Sensor.TYPE_GYROSCOPE -> {

                    val state = SensorState(
                        accelerationX = 0.0,
                        accelerationY = 0.0,
                        accelerationZ = 0.0,
                        rotationX = event.values[0].toDouble(),
                        rotationY = event.values[1].toDouble(),
                        rotationZ = event.values[2].toDouble(),
                        timestampMillis = now
                    )

                    dispatchState(state)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun start() {
        if (handlerThread != null) return

        val thread = HandlerThread("cebo-sensor-thread")
        thread.start()
        handlerThread = thread
        handler = Handler(thread.looper)

        registerSensorsForMode(samplingMode)
    }

    fun stop() {
        unregisterSensors()
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun setSamplingMode(mode: SamplingMode) {
        if (samplingMode == mode) return
        samplingMode = mode
        registerSensorsForMode(mode)
        notifySamplingModeChanged(mode)
    }

    private fun registerSensorsForMode(mode: SamplingMode) {

        unregisterSensors()

        if (mode == SamplingMode.OFF) return

        val rate = when (mode) {
            SamplingMode.HIGH -> highRateUs
            SamplingMode.NORMAL -> normalRateUs
            SamplingMode.LOW -> lowRateUs
            SamplingMode.OFF -> Int.MAX_VALUE
        }

        val accel = linearAccelSensor ?: accelSensor
        accel?.let {
            sensorManager.registerListener(sensorListener, it, rate, handler)
        }

        gyroSensor?.let {
            sensorManager.registerListener(sensorListener, it, rate, handler)
        }
    }

    private fun unregisterSensors() {
        try {
            sensorManager.unregisterListener(sensorListener)
        } catch (_: Exception) {}
    }

    private fun dispatchState(state: SensorState) {
        for (listener in listeners) {
            try {
                listener.onSensorState(state)
            } catch (_: Exception) {}
        }
    }

    private fun notifyMotionChanged(moving: Boolean) {
        for (listener in listeners) {
            try {
                listener.onMotionChanged(moving)
            } catch (_: Exception) {}
        }
    }

    private fun notifySamplingModeChanged(mode: SamplingMode) {
        for (listener in listeners) {
            try {
                listener.onSamplingModeChanged(mode)
            } catch (_: Exception) {}
        }
    }
}
