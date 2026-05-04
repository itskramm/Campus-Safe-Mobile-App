package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.campussafeapplication.models.SafetyTip
import com.example.campussafeapplication.utils.SwipeNavigationHelper
import com.example.campussafeapplication.viewmodels.SafetyTipViewModel
import kotlinx.coroutines.launch

class SafetyTipsActivity : AppCompatActivity() {

    private lateinit var tipViewModel: SafetyTipViewModel
    private lateinit var tvTipTitle1: TextView
    private lateinit var tvTipDesc1: TextView
    private lateinit var tvTipTitle2: TextView
    private lateinit var tvTipDesc2: TextView

    private lateinit var containerSafetyTips: LinearLayout
    private lateinit var containerHotlines: LinearLayout
    private lateinit var ivChevronTips: ImageView
    private lateinit var ivChevronHotlines: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_tips)

        tipViewModel = ViewModelProvider(this)[SafetyTipViewModel::class.java]

        tvTipTitle1 = findViewById(R.id.tvTipTitle1)
        tvTipDesc1 = findViewById(R.id.tvTipDesc1)
        tvTipTitle2 = findViewById(R.id.tvTipTitle2)
        tvTipDesc2 = findViewById(R.id.tvTipDesc2)

        containerSafetyTips = findViewById(R.id.containerSafetyTips)
        containerHotlines = findViewById(R.id.containerHotlines)
        ivChevronTips = findViewById(R.id.ivChevronTips)
        ivChevronHotlines = findViewById(R.id.ivChevronHotlines)

        setupDropdowns()
        setupNavigation()
        observeTips()
        tipViewModel.getAllSafetyTips()
        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.SAFETY_TIPS)
    }

    private fun setupDropdowns() {
        findViewById<RelativeLayout>(R.id.headerSafetyTips).setOnClickListener {
            toggleSection(containerSafetyTips, ivChevronTips)
        }

        findViewById<RelativeLayout>(R.id.headerHotlines).setOnClickListener {
            toggleSection(containerHotlines, ivChevronHotlines)
        }
    }

    private fun toggleSection(container: View, chevron: ImageView) {
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            chevron.animate().rotation(0f).start()
        } else {
            container.visibility = View.VISIBLE
            chevron.animate().rotation(180f).start()
        }
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
        findViewById<android.view.View>(R.id.navMaps).setOnClickListener {
            startActivity(Intent(this, NearbyReportsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeTips() {
        lifecycleScope.launch {
            tipViewModel.safetyTips.collect { tips ->
                bindTips(tips)
            }
        }
        lifecycleScope.launch {
            tipViewModel.tipState.collect { state ->
                if (state is SafetyTipViewModel.TipState.Error) {
                    Toast.makeText(this@SafetyTipsActivity, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindTips(tips: List<SafetyTip>) {
        val first = tips.getOrNull(0)
        val second = tips.getOrNull(1)

        if (first != null) {
            tvTipTitle1.text = first.title.uppercase()
            tvTipDesc1.text = first.description
        }
        if (second != null) {
            tvTipTitle2.text = second.title.uppercase()
            tvTipDesc2.text = second.description
        }
    }
}
