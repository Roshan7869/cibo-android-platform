package com.cebo.bus.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cebo.bus.ui.status.StatusFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, StatusFragment())
                .commitNow()
        }
    }
}
