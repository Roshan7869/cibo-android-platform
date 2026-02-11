package com.cebo.bus.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroscopeReader(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private var latestValues = FloatArray(3)
    private var lastTimestamp: Long = 0

    fun start() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun getReadings(): FloatArray = latestValues

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                latestValues = it.values.clone()
                lastTimestamp = it.timestamp
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
