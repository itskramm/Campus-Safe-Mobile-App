package com.example.campussafeapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.campussafeapplication.models.HazardReport
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.utils.SwipeNavigationHelper
import com.example.campussafeapplication.viewmodels.HazardReportViewModel
import com.google.android.gms.location.LocationServices
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ReportHazardActivity : AppCompatActivity() {

    private lateinit var reportViewModel: HazardReportViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var spinnerBuilding: Spinner
    private lateinit var spinnerFloor: Spinner
    private lateinit var etRoomNumber: EditText
    private lateinit var etHazardTitle: EditText
    private lateinit var etHazardDescription: EditText
    private lateinit var cbUseGps: CheckBox
    private lateinit var btnSubmitReport: Button
    private lateinit var ivSelectedPhoto: ImageView

    private var currentLocation: Location? = null
    private var selectedImageUri: String? = null
    private var pendingCameraImageUri: Uri? = null

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // Campus location constants
    private val STI_LAT = 14.5523065
    private val STI_LON = 121.0562232
    private val RADIUS_METERS = 200.0

    // Gemini constants
    private val GEMINI_API_KEY = "AIzaSyDfHmgaBOCkyu2xBLJDXoUMy0PpuGHLUko"
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                ivSelectedPhoto.setImageURI(uri)
                selectedImageUri = uri.toString()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                val imageUri = pendingCameraImageUri
                if (imageUri != null) {
                    ivSelectedPhoto.setImageURI(imageUri)
                    selectedImageUri = imageUri.toString()
                }
                Toast.makeText(this, "Photo captured.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera capture canceled.", Toast.LENGTH_SHORT).show()
            }
            pendingCameraImageUri = null
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadLastKnownLocation()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_hazard)

        reportViewModel = ViewModelProvider(this)[HazardReportViewModel::class.java]
        sessionManager = SessionManager(this)

        bindViews()
        setupNavigation()
        SwipeNavigationHelper.attach(this, SwipeNavigationHelper.Screen.ADD)
        setupSpinners()
        setupButtons()
        observeReportState()
        requestLocationIfNeeded()
    }

    private fun bindViews() {
        spinnerBuilding = findViewById(R.id.spinnerBuilding)
        spinnerFloor = findViewById(R.id.spinnerFloor)
        etRoomNumber = findViewById(R.id.etRoomNumber)
        etHazardTitle = findViewById(R.id.etHazardTitle)
        etHazardDescription = findViewById(R.id.etHazardDescription)
        cbUseGps = findViewById(R.id.cbUseGps)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        ivSelectedPhoto = findViewById(R.id.ivSelectedPhoto)
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
        findViewById<android.view.View>(R.id.navReports).setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navMaps).setOnClickListener {
            startActivity(Intent(this, NearbyReportsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupSpinners() {
        val buildingAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.building_array,
            R.layout.spinner_item
        )
        buildingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBuilding.adapter = buildingAdapter

        val floorAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.floor_array,
            R.layout.spinner_item
        )
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFloor.adapter = floorAdapter

        findViewById<android.view.View>(R.id.layoutBuildingContainer).setOnClickListener { spinnerBuilding.performClick() }
        findViewById<android.view.View>(R.id.layoutFloorContainer).setOnClickListener { spinnerFloor.performClick() }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(this, "No camera found on this device.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ensureCameraPermissionAndCapture()
        }
        findViewById<Button>(R.id.btnAttachPhoto).setOnClickListener {
            galleryLauncher.launch("image/*")
        }
        btnSubmitReport.setOnClickListener {
            submitReport()
        }
    }

    private fun observeReportState() {
        lifecycleScope.launch {
            reportViewModel.reportState.collect { state ->
                when (state) {
                    is HazardReportViewModel.ReportState.Loading -> {
                        btnSubmitReport.isEnabled = false
                    }
                    is HazardReportViewModel.ReportState.Success -> {
                        btnSubmitReport.isEnabled = true
                        Toast.makeText(this@ReportHazardActivity, "Report submitted.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is HazardReportViewModel.ReportState.Error -> {
                        btnSubmitReport.isEnabled = true
                        Toast.makeText(this@ReportHazardActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    HazardReportViewModel.ReportState.Idle -> {
                        btnSubmitReport.isEnabled = true
                    }
                }
            }
        }
    }

    private fun submitReport() {
        val userId = sessionManager.getUserId()
        if (userId.isBlank()) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etHazardTitle.text.toString().trim()
        val description = etHazardDescription.text.toString().trim()
        val room = etRoomNumber.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || room.isEmpty()) {
            Toast.makeText(this, "Please complete all report fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (cbUseGps.isChecked) {
            val loc = currentLocation
            if (loc == null) {
                Toast.makeText(this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show()
                loadLastKnownLocation()
                return
            }
            if (!isWithinCampus(loc.latitude, loc.longitude)) {
                Toast.makeText(this, "You must be within STI College Global City to report.", Toast.LENGTH_LONG).show()
                return
            }
        }

        btnSubmitReport.isEnabled = false
        Toast.makeText(this, "Validating report with AI...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val isValid = validateWithGemini(title, description, selectedImageUri)
            if (isValid) {
                val building = spinnerBuilding.selectedItem?.toString().orEmpty()
                val floor = spinnerFloor.selectedItem?.toString().orEmpty()
                val location = "$building, $floor, Room $room"
                val hazardType = detectHazardType("$title $description")
                val latitude = if (cbUseGps.isChecked) currentLocation?.latitude ?: 0.0 else 0.0
                val longitude = if (cbUseGps.isChecked) currentLocation?.longitude ?: 0.0 else 0.0

                val report = HazardReport(
                    userId = userId,
                    title = title,
                    building = building,
                    floor = floor,
                    room = room,
                    hazardType = hazardType,
                    description = "$title: $description",
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = selectedImageUri
                )
                reportViewModel.createReport(report)
            } else {
                btnSubmitReport.isEnabled = true
            }
        }
    }

    private suspend fun validateWithGemini(title: String, description: String, imageUriStr: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    You are an AI hazard validator for STI College Global City.
                    Analyze if the following report is a valid campus hazard.
                    A valid hazard is something that poses a threat to safety (fire, leak, broken structure, etc.).
                    Spam, jokes, or non-hazard content should be rejected.
                    
                    Title: $title
                    Description: $description
                    
                    Respond ONLY with this JSON schema:
                    { "isValid": boolean, "reason": "string" }
                """.trimIndent()

                val inputContent = content {
                    imageUriStr?.let { uriStr ->
                        try {
                            val uri = Uri.parse(uriStr)
                            val inputStream: InputStream? = contentResolver.openInputStream(uri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                image(bitmap)
                            }
                        } catch (e: Exception) {
                            // Ignore image error and proceed with text
                        }
                    }
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text ?: ""
                val json = JSONObject(responseText)
                val isValid = json.getBoolean("isValid")
                val reason = json.getString("reason")

                withContext(Dispatchers.Main) {
                    if (!isValid) {
                        Toast.makeText(this@ReportHazardActivity, "Rejected: $reason", Toast.LENGTH_LONG).show()
                    }
                }
                isValid
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReportHazardActivity, "Validation Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                // If AI fails, we might want to allow the report or fail safe. Let's allow but notify.
                true 
            }
        }
    }

    private fun isWithinCampus(lat: Double, lon: Double): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(lat, lon, STI_LAT, STI_LON, results)
        return results[0] <= RADIUS_METERS
    }

    private fun detectHazardType(content: String): String {
        val lower = content.lowercase()
        return when {
            "fire" in lower || "smoke" in lower || "burn" in lower -> "Fire"
            "flood" in lower || "water" in lower || "leak" in lower -> "Flood"
            "medical" in lower || "injury" in lower || "blood" in lower || "faint" in lower -> "Medical"
            "security" in lower || "threat" in lower || "theft" in lower || "suspicious" in lower -> "Security"
            "structural" in lower || "ceiling" in lower || "wall" in lower || "elevator" in lower -> "Structural"
            else -> "Other"
        }
    }

    private fun ensureCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraCapture() {
        val uri = createCameraImageUri()
        if (uri == null) {
            Toast.makeText(this, "Unable to create image file.", Toast.LENGTH_SHORT).show()
            return
        }
        pendingCameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun createCameraImageUri(): Uri? {
        val directory = File(cacheDir, "hazard_images")
        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }

        val imageFile = File(directory, "hazard_${System.currentTimeMillis()}.jpg")

        return try {
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun requestLocationIfNeeded() {
        if (!cbUseGps.isChecked) return
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadLastKnownLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun loadLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                currentLocation = location
            }
    }
}
