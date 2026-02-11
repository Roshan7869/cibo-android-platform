package com.cebo.bus.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cebo.bus.R
import com.cebo.bus.service.ServiceController
import com.cebo.bus.ui.ui_state.UiState
import com.cebo.bus.ui.views.SimpleStatusView

class StatusFragment : Fragment() {

    private lateinit var statusView: SimpleStatusView
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusView = view.findViewById(R.id.statusView)
        statusText = view.findViewById(R.id.statusText)

        context?.let { ServiceController.startService(it) }
        updateState(UiState(isTracking = true, isGpsLocked = true))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.let { ServiceController.stopService(it) }
    }

    fun updateState(state: UiState) {
        val allGood = state.isTracking && state.isGpsLocked && state.errorMessage == null
        statusView.setStatus(allGood)

        statusText.text = if (allGood) {
            getString(R.string.status_tracking_on)
        } else {
            state.errorMessage ?: getString(R.string.status_signal_lost)
        }
    }
}
