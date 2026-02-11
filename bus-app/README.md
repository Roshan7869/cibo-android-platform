# CEBO Bus App

This component is the core tracking intelligence installed in buses. It runs on a commodity Android phone (pinned to the dashboard) and provides:

1. **High-Precision Tracking**: Fusing GNSS (NavIC + GPS) with IMU (Accelerometer + Gyroscope) via a Kalman Filter.
2. **Offline-First Reliability**: Stores all data locally in Room DB and syncs when connectivity is available.
3. **Driver-Friendly UI**: Minimalist interface ("Green" = OK, "Red" = Issue) designed for low-literacy users.
4. **Robust Service**: A Foreground Service ensures tracking never stops, even if the screen is off or the app is minimized.

## Architecture

The app follows a strict layered architecture:

* **`core/`**: Pure Kotlin logic (No Android dependencies where possible). Contains the Physics Engine, Tracking Logic, and Sensor Fusion.
* **`gnss/`**: Handles interactions with the Android LocationManager (GPS/NavIC).
* **`sensors/`**: Handles interactions with the Android SensorManager (IMU).
* **`fusion/`**: Central brain that merges GNSS and Sensor data.
* **`route/`**: Snaps calculated positions to valid bus routes (Route Matching).
* **`storage/`**: Local persistence using Room Database.
* **`sync/`**: Bandwidth-optimized data upload engine.
* **`service/`**: The `TrackingForegroundService` that orchestrates everything.
* **`ui/`**: Dumb view layer responsible only for rendering state.

## Setup

1. Open in Android Studio.
2. Deploy to a physical Android device (NavIC support recommended).
3. Grant "Location Always" and "Physical Activity" permissions.
