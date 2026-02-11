package com.cebo.bus.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * NetworkMonitor
 *
 * Provides connectivity status for SyncManager.
 *
 * Responsibilities:
 * - Determine whether device has active internet capability
 * - Lightweight check (no long-running callbacks)
 *
 * Uses modern NetworkCapabilities API.
 */
class NetworkMonitor(
    context: Context
) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Returns true if device has validated internet connectivity.
     */
    fun isConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
