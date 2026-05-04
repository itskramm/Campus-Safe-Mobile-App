package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)
        
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthViewModel.AuthState.Loading -> {
                        btnSignUp.isEnabled = false
                    }
                    is AuthViewModel.AuthState.Success -> {
                        btnSignUp.isEnabled = true
                        sessionManager.saveUserSession(
                            state.user.id ?: "",
                            state.user.email,
                            state.user.fullName ?: ""
                        )
                        Toast.makeText(this@CreateAccountActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CreateAccountActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthViewModel.AuthState.Error -> {
                        btnSignUp.isEnabled = true
                        Toast.makeText(this@CreateAccountActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        btnSignUp.isEnabled = true
                    }
                }
            }
        }
        
        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            authViewModel.signUp(email, password, fullName)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
        
        tvLoginLink.setOnClickListener {
            finish() // Go back to Login
        }
    }
}
