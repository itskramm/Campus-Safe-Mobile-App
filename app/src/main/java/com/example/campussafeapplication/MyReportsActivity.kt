package com.example.campussafeapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.campussafeapplication.models.HazardReport
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.utils.SwipeNavigationHelper
import com.example.campussafeapplication.viewmodels.HazardReportViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class MyReportsActivity : AppCompatActivity() {

    private lateinit var reportViewModel: HazardReportViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var statusViews: List<TextView>
    private lateinit var titleViews: List<TextView>
    private lateinit var locationViews: List<TextView>
    private lateinit var timeViews: List<TextView>
    private lateinit var imageViews: List<ImageView>

    private lateinit var cardContainers: List<View>

    private val displayedReports: MutableList<HazardReport?> = MutableList(4) { null }
    private var isRefreshingAfterAction = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        reportViewModel = ViewModelProvider(this)[HazardReportViewModel::class.java]
        sessionManager = SessionManager(this)

        bindViews()
        setupNavigation()
        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.REPORTS)
        setupActionButtons()
        observeReports()
        loadReports()
    }

    private fun bindViews() {
        cardContainers = listOf(
            findViewById(R.id.cardReport1),
            findViewById(R.id.cardReport2),
            findViewById(R.id.cardReport3),
            findViewById(R.id.cardReport4)
        )
        statusViews = listOf(
            findViewById(R.id.status1),
            findViewById(R.id.status2),
            findViewById(R.id.status3),
            findViewById(R.id.status4)
        )
        titleViews = listOf(
            findViewById(R.id.title1),
            findViewById(R.id.title2),
            findViewById(R.id.title3),
            findViewById(R.id.title4)
        )
        locationViews = listOf(
            findViewById(R.id.loc1),
            findViewById(R.id.loc2),
            findViewById(R.id.loc3),
            findViewById(R.id.loc4)
        )
        timeViews = listOf(
            findViewById(R.id.time1),
            findViewById(R.id.time2),
            findViewById(R.id.time3),
            findViewById(R.id.time4)
        )
        imageViews = listOf(
            findViewById(R.id.imgReport1),
            findViewById(R.id.imgReport2),
            findViewById(R.id.imgReport3),
            findViewById(R.id.imgReport4)
        )
    }

    private fun setupNavigation() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        findViewById<android.view.View>(R.id.navAdd).setOnClickListener {
            startActivity(Intent(this, ReportHazardActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navMaps).setOnClickListener {
            startActivity(Intent(this, NearbyReportsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupActionButtons() {
        val markButtons = listOf(
            findViewById<android.view.View>(R.id.btnMarkFixed1),
            findViewById(R.id.btnMarkFixed2),
            findViewById(R.id.btnMarkFixed3),
            findViewById(R.id.btnMarkFixed4)
        )
        val editButtons = listOf(
            findViewById<android.view.View>(R.id.btnEditReport1),
            findViewById(R.id.btnEditReport2),
            findViewById(R.id.btnEditReport3),
            findViewById(R.id.btnEditReport4)
        )
        val deleteButtons = listOf(
            findViewById<android.view.View>(R.id.btnDeleteReport1),
            findViewById(R.id.btnDeleteReport2),
            findViewById(R.id.btnDeleteReport3),
            findViewById(R.id.btnDeleteReport4)
        )

        markButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val report = displayedReports[index] ?: return@setOnClickListener
                updateReportStatus(report, "Resolved")
            }
        }
        editButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val report = displayedReports[index] ?: return@setOnClickListener
                showEditDialog(report)
            }
        }
        deleteButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val report = displayedReports[index] ?: return@setOnClickListener
                deleteReport(report)
            }
        }
    }

    private fun observeReports() {
        lifecycleScope.launch {
            reportViewModel.reports.collect { reports ->
                bindReportCards(reports)
            }
        }

        lifecycleScope.launch {
            reportViewModel.reportState.collect { state ->
                when (state) {
                    is HazardReportViewModel.ReportState.Error -> {
                        Toast.makeText(this@MyReportsActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    HazardReportViewModel.ReportState.Success -> {
                        if (isRefreshingAfterAction) {
                            isRefreshingAfterAction = false
                            loadReports()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun loadReports() {
        val userId = sessionManager.getUserId()
        if (userId.isBlank()) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        reportViewModel.getUserReports(userId)
    }

    private fun bindReportCards(reports: List<HazardReport>) {
        for (index in 0 until 4) {
            val report = reports.getOrNull(index)
            displayedReports[index] = report

            if (report == null) {
                cardContainers[index].visibility = View.GONE
                continue
            }

            cardContainers[index].visibility = View.VISIBLE
            statusViews[index].text = report.status
            titleViews[index].text = report.description.substringBefore(":").uppercase()
            locationViews[index].text = report.location
            timeViews[index].text = formatTimeAgo(report.createdAt)
            statusViews[index].backgroundTintList = ColorStateList.valueOf(statusColor(report.status))

            if (!report.imageUrl.isNullOrBlank()) {
                imageViews[index].visibility = View.VISIBLE
                Glide.with(this)
                    .load(report.imageUrl)
                    .into(imageViews[index])
            } else {
                imageViews[index].visibility = View.GONE
            }
        }
    }

    private fun showEditDialog(report: HazardReport) {
        val input = android.widget.EditText(this).apply {
            setText(report.description.substringAfter(":", report.description).trim())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Report Description")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newDescription = input.text.toString().trim()
                if (newDescription.isEmpty()) {
                    Toast.makeText(this, "Description cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updated = "${report.description.substringBefore(":")}: $newDescription"
                isRefreshingAfterAction = true
                report.id?.let {
                    reportViewModel.updateReport(it, mapOf("description" to updated))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateReportStatus(report: HazardReport, status: String) {
        val id = report.id ?: return
        isRefreshingAfterAction = true
        reportViewModel.updateReport(id, mapOf("status" to status))
    }

    private fun deleteReport(report: HazardReport) {
        val id = report.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report?")
            .setPositiveButton("Delete") { _, _ ->
                isRefreshingAfterAction = true
                reportViewModel.deleteReport(id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun statusColor(status: String): Int {
        return when (status.lowercase()) {
            "resolved" -> 0xFF4CAF50.toInt()
            "in progress" -> 0xFFFBC02D.toInt()
            "pending" -> 0xFFFF5252.toInt()
            else -> 0xFF757575.toInt()
        }
    }

    private fun formatTimeAgo(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val diff = Duration.between(Instant.parse(iso), Instant.now())
            when {
                diff.toMinutes() < 1 -> "Just now"
                diff.toHours() < 1 -> "${diff.toMinutes()} mins ago"
                diff.toDays() < 1 -> "${diff.toHours()} hours ago"
                else -> "${diff.toDays()} days ago"
            }
        } catch (_: Exception) {
            iso
        }
    }
}
