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
import com.cebo.bus.R
import com.cebo.bus.core.model.Accuracy
import com.cebo.bus.core.model.GeoPoint
import com.cebo.bus.core.model.Heading
import com.cebo.bus.core.model.Speed
import com.cebo.bus.core.tracking.TrackingEngine
import com.cebo.bus.gnss.GnssManager
import com.cebo.bus.gnss.GnssQualityMonitor
import com.cebo.bus.route.Route
import com.cebo.bus.route.RouteMatcher
import com.cebo.bus.route.RouteValidator
import com.cebo.bus.sensors.SensorManagerWrapper
import com.cebo.bus.storage.database.CeboDatabase
import com.cebo.bus.storage.repository.LocationRepository
import com.cebo.bus.sync.NetworkMonitor
import com.cebo.bus.sync.PayloadCompressor
import com.cebo.bus.sync.SyncManager

/**
 * Foreground service that owns runtime dependencies and tracking lifecycle.
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

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

        val initialPoint = GeoPoint(12.9716, 77.5946)
        val initialSpeed = Speed.ZERO
        val initialHeading = Heading.NORTH
        val initialAccuracy = Accuracy.fromMeters(50.0)
        val initialTimestamp = System.currentTimeMillis()

        val trackingEngine = TrackingEngine(
            initialPosition = initialPoint,
            initialSpeed = initialSpeed,
            initialHeading = initialHeading,
            initialAccuracy = initialAccuracy,
            initialTimestampMillis = initialTimestamp
        )

        val qualityMonitor = GnssQualityMonitor()
        val gnssManager = GnssManager(applicationContext, qualityMonitor)
        val sensorManager = SensorManagerWrapper(applicationContext)

        val bootRoute = Route(
            id = "default_boot_route",
            points = listOf(
                GeoPoint(12.9716, 77.5946),
                GeoPoint(12.9726, 77.5960)
            ),
            cumulativeDistancesMeters = listOf(0.0, 170.0),
            totalDistanceMeters = 170.0
        )

        trackingCoordinator = TrackingCoordinator(
            gnssManager = gnssManager,
            trackingEngine = trackingEngine,
            sensorManager = sensorManager,
            locationRepository = repository,
            syncManager = syncManager,
            route = bootRoute,
            routeMatcher = RouteMatcher(),
            routeValidator = RouteValidator()
        )
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_active_title))
            .setContentText(getString(R.string.tracking_active_description))
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
