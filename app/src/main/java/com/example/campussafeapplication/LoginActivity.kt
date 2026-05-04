package com.example.campussafeapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.campussafeapplication.utils.SessionManager
import com.example.campussafeapplication.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)
        
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCreateAccount = findViewById<Button>(R.id.btnCreateAccount)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)
        
        // Show biometric button if enabled in settings
        btnBiometric.visibility = if (sessionManager.isBiometricEnabled()) android.view.View.VISIBLE else android.view.View.GONE

        btnBiometric.setOnClickListener {
            startActivity(Intent(this, BiometricActivity::class.java))
        }
        
        // Observe auth state
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthViewModel.AuthState.Loading -> {
                        btnLogin.isEnabled = false
                    }
                    is AuthViewModel.AuthState.Success -> {
                        btnLogin.isEnabled = true
                        sessionManager.saveUserSession(
                            state.user.id ?: "",
                            state.user.email,
                            state.user.fullName ?: ""
                        )
                        Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthViewModel.AuthState.Error -> {
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is AuthViewModel.AuthState.PasswordResetEmailSent -> {
                        btnLogin.isEnabled = true
                        Toast.makeText(
                            this@LoginActivity,
                            "Password reset email sent.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        btnLogin.isEnabled = true
                    }
                }
            }
        }
        
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.signIn(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        
        btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(mGoogleSignInClient.signInIntent)
        }
        
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
            } else {
                authViewModel.resetPassword(email)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update biometric button visibility when returning to screen
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)
        btnBiometric.visibility = if (sessionManager.isBiometricEnabled()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
