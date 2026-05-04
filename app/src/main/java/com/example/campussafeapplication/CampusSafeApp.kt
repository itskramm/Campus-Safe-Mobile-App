package com.example.campussafeapplication

import android.app.Application
import com.example.campussafeapplication.utils.SessionManager

class CampusSafeApp : Application() {
   override fun onCreate() {
       super.onCreate()
        val sessionManager = SessionManager(this)
       sessionManager.applyTheme(sessionManager.getThemeMode())
   }
}
