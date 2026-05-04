package com.example.campussafeapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campussafeapplication.models.HazardReport
import com.example.campussafeapplication.repository.HazardReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HazardReportViewModel : ViewModel() {
    
    private val repository = HazardReportRepository()
    
    private val _reports = MutableStateFlow<List<HazardReport>>(emptyList())
    val reports: StateFlow<List<HazardReport>> = _reports
    
    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState
    
    sealed class ReportState {
        object Idle : ReportState()
        object Loading : ReportState()
        object Success : ReportState()
        data class Error(val message: String) : ReportState()
    }
    
    fun createReport(report: HazardReport) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            val result = repository.createReport(report)
            
            result.onSuccess {
                _reportState.value = ReportState.Success
            }.onFailure { error ->
                _reportState.value = ReportState.Error(error.message ?: "Failed to create report")
            }
        }
    }
    
    fun getUserReports(userId: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            val result = repository.getUserReports(userId)
            
            result.onSuccess { reportList ->
                _reports.value = reportList
                _reportState.value = ReportState.Idle
            }.onFailure { error ->
                _reportState.value = ReportState.Error(error.message ?: "Failed to load reports")
            }
        }
    }
    
    fun getAllReports() {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            val result = repository.getAllReports()
            
            result.onSuccess { reportList ->
                _reports.value = reportList
                _reportState.value = ReportState.Idle
            }.onFailure { error ->
                _reportState.value = ReportState.Error(error.message ?: "Failed to load reports")
            }
        }
    }
    
    fun updateReport(reportId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            val result = repository.updateReport(reportId, updates)
            
            result.onSuccess {
                _reportState.value = ReportState.Success
            }.onFailure { error ->
                _reportState.value = ReportState.Error(error.message ?: "Failed to update report")
            }
        }
    }
    
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            val result = repository.deleteReport(reportId)
            
            result.onSuccess {
                _reportState.value = ReportState.Success
            }.onFailure { error ->
                _reportState.value = ReportState.Error(error.message ?: "Failed to delete report")
            }
        }
    }
}
