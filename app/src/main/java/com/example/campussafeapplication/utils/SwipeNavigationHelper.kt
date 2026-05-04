package com.example.campussafeapplication.utils

import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.campussafeapplication.MainActivity
import com.example.campussafeapplication.MyReportsActivity
import com.example.campussafeapplication.NearbyReportsActivity
import com.example.campussafeapplication.ReportHazardActivity
import com.example.campussafeapplication.SettingsActivity
import com.example.campussafeapplication.SafetyTipsActivity
import kotlin.math.abs

object SwipeNavigationHelper {
    private const val SWIPE_DISTANCE_THRESHOLD = 100
    private const val SWIPE_VELOCITY_THRESHOLD = 100

    private val NAVIGATION_ORDER = arrayOf(
        MainActivity::class.java,
        MyReportsActivity::class.java,
        ReportHazardActivity::class.java,
        NearbyReportsActivity::class.java,
        SettingsActivity::class.java,
        SafetyTipsActivity::class.java
    )

    enum class Screen(val index: Int) {
        HOME(0),
        REPORTS(1),
        ADD(2),
        MAPS(3),
        SETTINGS(4),
        SAFETY_TIPS(5)
    }

    @JvmStatic
    fun attach(activity: AppCompatActivity, currentScreen: Screen) {
        val rootContent = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val detector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Horizontal swipe detection
                if (abs(deltaX) > abs(deltaY) && 
                    abs(deltaX) > SWIPE_DISTANCE_THRESHOLD && 
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    
                    val targetIndex = if (deltaX < 0) currentScreen.index + 1 else currentScreen.index - 1
                    return navigateTo(activity, targetIndex, currentScreen.index)
                }
                return false
            }
        })

        val touchListener = View.OnTouchListener { v, event ->
            val handled = detector.onTouchEvent(event)
            if (handled && event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            false // Return false to let the view (like ScrollView) handle the event as well
        }

        // Apply to root
        rootContent.setOnTouchListener(touchListener)
        
        // Deeply search for scrollable views that might intercept touches
        findAndAttachToScrollables(rootContent, touchListener)
    }

    private fun findAndAttachToScrollables(view: View, listener: View.OnTouchListener) {
        if (view is ScrollView || view is HorizontalScrollView || view is ListView || view is RecyclerView) {
            view.setOnTouchListener(listener)
        }
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAndAttachToScrollables(view.getChildAt(i), listener)
            }
        }
    }

    private fun navigateTo(activity: AppCompatActivity, targetIndex: Int, currentIndex: Int): Boolean {
        if (targetIndex < 0 || targetIndex >= NAVIGATION_ORDER.size || targetIndex == currentIndex) {
            return false
        }

        val destination = NAVIGATION_ORDER[targetIndex]
        if (activity.javaClass == destination) {
            return false
        }

        val intent = Intent(activity, destination)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        activity.startActivity(intent)
        return true
    }
}
