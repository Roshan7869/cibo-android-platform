package com.cebo.bus.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cebo.bus.ui.ui_state.UiState
import com.cebo.bus.ui.views.SimpleStatusView
import com.cebo.bus.service.ServiceController

class StatusFragment : Fragment() {

    private lateinit var statusView: SimpleStatusView
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout (programmatic layout for simplicity in this file)
        // In real app, use R.layout.fragment_status
        return View(context) // Placeholder
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Assume finding views
        // statusView = view.findViewById(...)
        // statusText = view.findViewById(...)
        
        // Initialize Service
        context?.let { ServiceController.startService(it) }
    }

    fun updateState(state: UiState) {
        val allGood = state.isTracking && state.isGpsLocked && state.errorMessage == null
        
        // statusView.setStatus(allGood)
        
        if (allGood) {
            // statusText.text = "Tracking ON" // In local language
        } else {
            // statusText.text = state.errorMessage ?: "Signal Lost"
        }
    }
}
