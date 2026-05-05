package com.example.campussafeapplication

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.campussafeapplication.models.HazardReport
import com.example.campussafeapplication.utils.SwipeNavigationHelper
import com.example.campussafeapplication.viewmodels.HazardReportViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class NearbyReportsActivity : AppCompatActivity() {

    private lateinit var reportViewModel: HazardReportViewModel

    private lateinit var filterAllButton: Button
    private lateinit var filterFloor1Button: Button
    private lateinit var filterFloor2Button: Button

    private lateinit var titleViews: List<TextView>
    private lateinit var locationViews: List<TextView>
    private lateinit var reporterViews: List<TextView>
    private lateinit var statusViews: List<TextView>
    private lateinit var timeViews: List<TextView>
    private lateinit var iconViews: List<ImageView>

    private var allReports: List<HazardReport> = emptyList()
    private var displayedReports: List<HazardReport> = emptyList()
    private var activeFilter: FloorFilter = FloorFilter.ALL

    private enum class FloorFilter { ALL, FLOOR_1, FLOOR_2 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_reports)

        reportViewModel = ViewModelProvider(this)[HazardReportViewModel::class.java]

        bindViews()
        setupNavigation()
        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.MAPS)
        setupFilters()
        setupActionButtons()
        observeReports()
        reportViewModel.getAllReports()
    }

    private fun bindViews() {
        filterAllButton = findViewById(R.id.btnFilterAllFloors)
        filterFloor1Button = findViewById(R.id.btnFilterFloor1)
        filterFloor2Button = findViewById(R.id.btnFilterFloor2)

        titleViews = listOf(
            findViewById(R.id.tvReportTitle1),
            findViewById(R.id.tvReportTitle2),
            findViewById(R.id.tvReportTitle3)
        )
        locationViews = listOf(
            findViewById(R.id.tvReportLocation1),
            findViewById(R.id.tvReportLocation2),
            findViewById(R.id.tvReportLocation3)
        )
        reporterViews = listOf(
            findViewById(R.id.tvReportReporter1),
            findViewById(R.id.tvReportReporter2),
            findViewById(R.id.tvReportReporter3)
        )
        statusViews = listOf(
            findViewById(R.id.tvReportStatus1),
            findViewById(R.id.tvReportStatus2),
            findViewById(R.id.tvReportStatus3)
        )
        timeViews = listOf(
            findViewById(R.id.tvReportTime1),
            findViewById(R.id.tvReportTime2),
            findViewById(R.id.tvReportTime3)
        )
        iconViews = listOf(
            findViewById(R.id.icon1),
            findViewById(R.id.icon2),
            findViewById(R.id.icon3)
        )
    }

    private fun setupNavigation() {
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

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

        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupFilters() {
        filterAllButton.setOnClickListener {
            activeFilter = FloorFilter.ALL
            applyFilter()
        }
        filterFloor1Button.setOnClickListener {
            activeFilter = FloorFilter.FLOOR_1
            applyFilter()
        }
        filterFloor2Button.setOnClickListener {
            activeFilter = FloorFilter.FLOOR_2
            applyFilter()
        }
    }

    private fun setupActionButtons() {
        findViewById<Button>(R.id.btnNearbyView).setOnClickListener {
            val report = displayedReports.firstOrNull()
            if (report == null) {
                Toast.makeText(this, "No report to view.", Toast.LENGTH_SHORT).show()
            } else {
                val description = report.description.orEmpty()
                val reportTitle = description.substringBefore(":").ifBlank { "REPORT" }.uppercase()
                val reportDetails = description.substringAfter(":", description).trim()
                val statusText = report.status.orEmpty().ifBlank { "Unknown" }
                val reporterName = report.reporterName.orEmpty().ifBlank { "Campus Reporter" }
                AlertDialog.Builder(this)
                    .setTitle(reportTitle)
                    .setMessage(
                        "Location: ${report.location.orEmpty()}\nReported by: $reporterName\nStatus: $statusText\n\nDetails:\n$reportDetails"
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        findViewById<Button>(R.id.btnNearbyEdit).setOnClickListener {
            val report = displayedReports.firstOrNull()
            val id = report?.id
            if (id == null) {
                Toast.makeText(this, "No report to edit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val description = report.description.orEmpty()
            val input = android.widget.EditText(this).apply {
                setText(description.substringAfter(":", description).trim())
            }
            AlertDialog.Builder(this)
                .setTitle("Edit Report Description")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val updated = input.text.toString().trim()
                    if (updated.isEmpty()) {
                        Toast.makeText(this, "Description cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val prefix = description.substringBefore(":").ifBlank { "Report" }
                    val newValue = "$prefix: $updated"
                    reportViewModel.updateReport(id, mapOf("description" to newValue))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnNearbyDelete).setOnClickListener {
            val id = displayedReports.firstOrNull()?.id
            if (id == null) {
                Toast.makeText(this, "No report to delete.", Toast.LENGTH_SHORT).show()
            } else {
                reportViewModel.deleteReport(id)
            }
        }
    }

    private fun observeReports() {
        lifecycleScope.launch {
            reportViewModel.reports.collect { reports ->
                allReports = reports
                applyFilter()
            }
        }

        lifecycleScope.launch {
            reportViewModel.reportState.collect { state ->
                if (state is HazardReportViewModel.ReportState.Success) {
                    reportViewModel.getAllReports()
                } else if (state is HazardReportViewModel.ReportState.Error) {
                    Toast.makeText(this@NearbyReportsActivity, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFilter() {
        displayedReports = when (activeFilter) {
            FloorFilter.ALL -> allReports
            FloorFilter.FLOOR_1 -> allReports.filter { matchesFloor1(it.location.orEmpty()) }
            FloorFilter.FLOOR_2 -> allReports.filter { matchesFloor2(it.location.orEmpty()) }
        }

        for (index in 0 until 3) {
            val report = displayedReports.getOrNull(index)
            if (report == null) {
                titleViews[index].text = "NO REPORT"
                locationViews[index].text = "No nearby report found for this filter."
                reporterViews[index].text = ""
                statusViews[index].text = "Status: N/A"
                statusViews[index].backgroundTintList = ColorStateList.valueOf(0xFF757575.toInt())
                timeViews[index].text = ""
                continue
            }

            val description = report.description.orEmpty()
            val status = report.status.orEmpty().ifBlank { "Unknown" }
            val reporterName = report.reporterName.orEmpty().ifBlank { "Campus Reporter" }
            titleViews[index].text = description.substringBefore(":").ifBlank { "REPORT" }.uppercase()
            locationViews[index].text = report.location.orEmpty()
            reporterViews[index].text = "Reported by: $reporterName"
            statusViews[index].text = "Status: $status"
            statusViews[index].backgroundTintList = ColorStateList.valueOf(statusColor(status))
            timeViews[index].text = formatTimeAgo(report.createdAt)

            if (!report.imageUrl.isNullOrBlank()) {
                Glide.with(this)
                    .load(report.imageUrl)
                    .into(iconViews[index])
                iconViews[index].imageTintList = null
            } else {
                // Fallback to default icons if no image
                val defaultIcon = when (report.hazardType.orEmpty().lowercase()) {
                    "fire" -> R.drawable.ic_fire
                    else -> android.R.drawable.stat_sys_warning
                }
                iconViews[index].setImageResource(defaultIcon)
                // Set some default tint if needed, or keep it original
            }
        }
    }

    private fun matchesFloor1(location: String): Boolean {
        val value = location.lowercase()
        return "floor 1" in value || "ground floor" in value || "first floor" in value
    }

    private fun matchesFloor2(location: String): Boolean {
        val value = location.lowercase()
        return "floor 2" in value || "second floor" in value
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
