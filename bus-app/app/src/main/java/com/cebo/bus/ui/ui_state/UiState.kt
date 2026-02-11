package com.cebo.bus.ui.ui_state

import com.cebo.bus.core.model.Confidence

data class UiState(
    val isTracking: Boolean = false,
    val isGpsLocked: Boolean = false,
    val satelliteCount: Int = 0,
    val isNavicVisible: Boolean = false,
    val confidence: Confidence = Confidence(0f),
    val errorMessage: String? = null
)
