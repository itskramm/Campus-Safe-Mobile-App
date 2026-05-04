package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.utils.SwipeNavigationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        bindCurrentUser()
        setupNavigation()
        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.HOME)
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun setupNavigation() {
        // grid menu
        findViewById<View>(R.id.gridReportHazards).setOnClickListener { navigateTo(ReportHazardActivity::class.java) }
        findViewById<View>(R.id.gridNearbyReports).setOnClickListener { navigateTo(NearbyReportsActivity::class.java) }
        findViewById<View>(R.id.gridMyReports).setOnClickListener { navigateTo(MyReportsActivity::class.java) }
        findViewById<View>(R.id.gridSafetyTips).setOnClickListener { navigateTo(SafetyTipsActivity::class.java) }

        // bottom bar navigation
        findViewById<View>(R.id.navReports).setOnClickListener { navigateTo(MyReportsActivity::class.java) }
        findViewById<View>(R.id.navAdd).setOnClickListener { navigateTo(ReportHazardActivity::class.java) }
        findViewById<View>(R.id.navMaps).setOnClickListener { navigateTo(NearbyReportsActivity::class.java) }
        findViewById<View>(R.id.navSettings).setOnClickListener { navigateTo(SettingsActivity::class.java) }

        // top bar theme mode toggle
        findViewById<View>(R.id.btnThemeToggle).setOnClickListener { toggleThemeMode() }

        // home (bottom navigation bar)
        findViewById<View>(R.id.navHome).setOnClickListener { }
    }

    private fun toggleThemeMode() {
        val currentMode = sessionManager.getThemeMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        sessionManager.setThemeMode(newMode)
    }

    private fun bindCurrentUser() {
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)

        val name = sessionManager.getUserName()
        val email = sessionManager.getUserEmail()

        if (!name.isNullOrEmpty()) {
            tvWelcome.text = "Welcome, $name!"
        }
        if (!email.isNullOrEmpty()) {
            tvUserEmail.text = email
        }
    }
}
