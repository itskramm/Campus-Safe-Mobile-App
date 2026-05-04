package com.example.campussafeapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campussafeapplication.models.SafetyTip
import com.example.campussafeapplication.repository.SafetyTipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SafetyTipViewModel : ViewModel() {
    
    private val repository = SafetyTipRepository()
    
    private val _safetyTips = MutableStateFlow<List<SafetyTip>>(emptyList())
    val safetyTips: StateFlow<List<SafetyTip>> = _safetyTips
    
    private val _tipState = MutableStateFlow<TipState>(TipState.Idle)
    val tipState: StateFlow<TipState> = _tipState
    
    sealed class TipState {
        object Idle : TipState()
        object Loading : TipState()
        data class Error(val message: String) : TipState()
    }
    
    fun getAllSafetyTips() {
        viewModelScope.launch {
            _tipState.value = TipState.Loading
            
            val result = repository.getAllSafetyTips()
            
            result.onSuccess { tips ->
                _safetyTips.value = tips
                _tipState.value = TipState.Idle
            }.onFailure { error ->
                _tipState.value = TipState.Error(error.message ?: "Failed to load safety tips")
            }
        }
    }
    
    fun getSafetyTipsByCategory(category: String) {
        viewModelScope.launch {
            _tipState.value = TipState.Loading
            
            val result = repository.getSafetyTipsByCategory(category)
            
            result.onSuccess { tips ->
                _safetyTips.value = tips
                _tipState.value = TipState.Idle
            }.onFailure { error ->
                _tipState.value = TipState.Error(error.message ?: "Failed to load safety tips")
            }
        }
    }
}
