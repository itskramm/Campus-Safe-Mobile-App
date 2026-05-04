package com.example.campussafeapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campussafeapplication.models.User
import com.example.campussafeapplication.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState
    
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        object PasswordResetEmailSent : AuthState()
        data class Error(val message: String) : AuthState()
    }

    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        data class Success(val user: User) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
    
    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.signUp(email, password, fullName)
            
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Google sign in failed")
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.signIn(email, password)
            
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign in failed")
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Idle
        }
    }
    
    fun getCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
        }
    }
    
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.resetPassword(email)
            
            result.onSuccess {
                _authState.value = AuthState.PasswordResetEmailSent
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Password reset failed")
            }
        }
    }

    fun updateProfile(fullName: String, email: String, phoneNumber: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            val result = authRepository.updateProfile(fullName, email, phoneNumber)

            result.onSuccess { user ->
                _currentUser.value = user
                _profileState.value = ProfileState.Success(user)
            }.onFailure { error ->
                _profileState.value = ProfileState.Error(error.message ?: "Profile update failed")
            }
        }
    }

    fun updateBiometricSetting(enabled: Boolean) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            val result = authRepository.updateBiometricSetting(enabled)

            result.onSuccess { user ->
                _currentUser.value = user
                _profileState.value = ProfileState.Success(user)
            }.onFailure { error ->
                _profileState.value = ProfileState.Error(error.message ?: "Biometric update failed")
            }
        }
    }
}
