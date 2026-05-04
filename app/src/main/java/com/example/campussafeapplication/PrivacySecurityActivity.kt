package com.example.campussafeapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.campussafeapplication.utils.SessionManager
import com.google.android.material.switchmaterial.SwitchMaterial

class PrivacySecurityActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var switchLocation: SwitchMaterial
    private lateinit var tvDeviceId: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                sessionManager.setNotificationsEnabled(true) // Reusing as a general permission flag or specifically for location
                switchLocation.isChecked = true
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            } else {
                switchLocation.isChecked = false
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_security)

        sessionManager = SessionManager(this)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        switchLocation = findViewById(R.id.switchLocation)
        tvDeviceId = findViewById(R.id.tvDeviceId)

        btnBack.setOnClickListener { finish() }

        // Set current state
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        switchLocation.isChecked = hasLocationPermission

        findViewById<android.view.View>(R.id.btnLocationToggle).setOnClickListener {
            if (!switchLocation.isChecked) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                Toast.makeText(this, "Please disable location in System Settings if you wish to revoke access.", Toast.LENGTH_LONG).show()
                // We keep it checked because we can't programmatically revoke permission
                switchLocation.isChecked = true
            }
        }

        // Display Device Fingerprint (ANDROID_ID)
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        tvDeviceId.text = deviceId ?: "Unavailable"
    }
}
