package com.cebo.bus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cebo.bus.core.tracking.TrackingEngine
import com.cebo.bus.gnss.GnssManager
import com.cebo.bus.gnss.GnssQualityMonitor
import com.cebo.bus.storage.database.CeboDatabase
import com.cebo.bus.storage.repository.LocationRepository
import com.cebo.bus.sync.NetworkMonitor
import com.cebo.bus.sync.PayloadCompressor
import com.cebo.bus.sync.SyncManager

/**
 * TrackingForegroundService
 *
 * Foreground service responsible for:
 * - Initializing dependency graph
 * - Starting tracking coordinator
 * - Keeping tracking alive in background
 *
 * No business logic here.
 */
class TrackingForegroundService : Service() {

    private lateinit var trackingCoordinator: TrackingCoordinator

    override fun onCreate() {
        super.onCreate()

        initializeDependencies()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        trackingCoordinator.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        trackingCoordinator.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeDependencies() {
        val database = CeboDatabase.getInstance(applicationContext)
        val repository = LocationRepository(database.locationDao())

        val networkMonitor = NetworkMonitor(applicationContext)
        val compressor = PayloadCompressor()
        val syncManager = SyncManager(repository, networkMonitor, compressor)

        val trackingEngine = TrackingEngine()
        val qualityMonitor = GnssQualityMonitor()
        val gnssManager = GnssManager(applicationContext, qualityMonitor)

        trackingCoordinator = TrackingCoordinator(
            gnssManager = gnssManager,
            trackingEngine = trackingEngine,
            locationRepository = repository,
            syncManager = syncManager
        )
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CEBO Bus Tracking Active")
            .setContentText("Tracking bus location in real-time")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CEBO Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "cebo_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
