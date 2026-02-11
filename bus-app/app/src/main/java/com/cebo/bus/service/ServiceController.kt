package com.cebo.bus.service

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceController {

    fun startService(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java)
        context.stopService(intent)
    }
}
