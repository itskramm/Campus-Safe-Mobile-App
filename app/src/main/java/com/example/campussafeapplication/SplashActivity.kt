package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.campussafeapplication.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(this)
        
        val savedMode = sessionManager.getThemeMode()
        if (savedMode != -1) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for 3 seconds then decide which activity to start
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                sessionManager.isLoggedIn() -> {
                    if (sessionManager.isBiometricEnabled()) {
                        Intent(this, BiometricActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                }
                else -> Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, 3000)
    }
}
