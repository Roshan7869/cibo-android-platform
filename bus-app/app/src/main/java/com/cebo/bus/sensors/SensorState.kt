package com.cebo.bus.sensors

/**
 * SensorState
 *
 * Represents a snapshot of IMU sensor readings.
 *
 * Pure data model.
 * No Android dependencies.
 */
data class SensorState(

    // Linear acceleration components (m/s^2)
    val accelerationX: Double,
    val accelerationY: Double,
    val accelerationZ: Double,

    // Angular velocity components (rad/s)
    val rotationX: Double,
    val rotationY: Double,
    val rotationZ: Double,

    // Timestamp of sensor reading (milliseconds)
    val timestampMillis: Long
)
