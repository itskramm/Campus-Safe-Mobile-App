package com.example.campussafeapplication

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var etProfileName: EditText
    private lateinit var etProfileEmail: EditText
    private lateinit var etProfilePassword: EditText
    private lateinit var etProfilePhone: EditText
    private lateinit var btnSaveChanges: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)

        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        etProfilePassword = findViewById(R.id.etProfilePassword)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        btnSaveChanges.setOnClickListener { saveProfile() }

        observeProfileData()
        authViewModel.getCurrentUser()
    }

    private fun observeProfileData() {
        lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                if (user != null) {
                    if (etProfileName.text.isNullOrBlank()) etProfileName.setText(user.fullName.orEmpty())
                    if (etProfileEmail.text.isNullOrBlank()) etProfileEmail.setText(user.email)
                    if (etProfilePhone.text.isNullOrBlank()) etProfilePhone.setText(user.phoneNumber.orEmpty())
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.profileState.collect { state ->
                when (state) {
                    is AuthViewModel.ProfileState.Loading -> {
                        btnSaveChanges.isEnabled = false
                    }
                    is AuthViewModel.ProfileState.Success -> {
                        btnSaveChanges.isEnabled = true
                        sessionManager.saveUserSession(
                            state.user.id.orEmpty(),
                            state.user.email,
                            state.user.fullName.orEmpty()
                        )
                        val passwordInput = etProfilePassword.text.toString().trim()
                        if (passwordInput.isNotEmpty()) {
                            authViewModel.resetPassword(state.user.email)
                            etProfilePassword.text?.clear()
                            Toast.makeText(
                                this@ProfileActivity,
                                "Profile updated. Password reset email sent.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Changes saved successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }
                    is AuthViewModel.ProfileState.Error -> {
                        btnSaveChanges.isEnabled = true
                        Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    AuthViewModel.ProfileState.Idle -> {
                        btnSaveChanges.isEnabled = true
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val name = etProfileName.text.toString().trim()
        val email = etProfileEmail.text.toString().trim()
        val phone = etProfilePhone.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address.", Toast.LENGTH_SHORT).show()
            return
        }

        authViewModel.updateProfile(name, email, phone)
    }
}
