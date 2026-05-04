package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.utils.SwipeNavigationHelper
import com.example.campussafeapplication.viewmodels.AuthViewModel
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var authViewModel: AuthViewModel

    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var switchNotifications: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        sessionManager = SessionManager(this)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        switchBiometric = findViewById(R.id.switchBiometric)
        switchBiometric.isChecked = sessionManager.isBiometricEnabled()

        switchNotifications = findViewById(R.id.switchNotifications)
        switchNotifications.isChecked = sessionManager.isNotificationsEnabled()

        // Top Bar
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        
        // Settings Options
        findViewById<android.view.View>(R.id.btnNotificationToggle).setOnClickListener {
            val enabled = !sessionManager.isNotificationsEnabled()
            sessionManager.setNotificationsEnabled(enabled)
            switchNotifications.isChecked = enabled
            Toast.makeText(
                this,
                if (enabled) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<android.view.View>(R.id.btnAccount).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnBiometricToggle).setOnClickListener {
            val enabled = !sessionManager.isBiometricEnabled()
            sessionManager.setBiometricEnabled(enabled)
            authViewModel.updateBiometricSetting(enabled)
            switchBiometric.isChecked = enabled
            Toast.makeText(
                this,
                if (enabled) "Biometric login enabled" else "Biometric login disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<android.view.View>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacySecurityActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnLogout).setOnClickListener {
            authViewModel.signOut()
            sessionManager.clearSession()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
            
        // Bottom Navigation
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.navReports).setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navAdd).setOnClickListener {
            startActivity(Intent(this, ReportHazardActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navMaps).setOnClickListener {
            startActivity(Intent(this, NearbyReportsActivity::class.java))
        }

        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.SETTINGS)
    }
}
