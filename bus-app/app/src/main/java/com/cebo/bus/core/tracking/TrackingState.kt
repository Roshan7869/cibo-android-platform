package com.cebo.bus.core.tracking

import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.Confidence
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed

/**
 * TrackingState
 *
 * Immutable snapshot representing the complete tracking state of the bus
 * at a specific moment in time.
 *
 * This is the ONLY object that higher layers (service, storage, sync, UI)
 * should consume from the core engine.
 */
data class TrackingState(
    val position: GeoPoint,
    val speed: Speed,
    val heading: Heading,
    val accuracy: Accuracy,
    val confidence: Confidence,
    val timestampMillis: Long
)