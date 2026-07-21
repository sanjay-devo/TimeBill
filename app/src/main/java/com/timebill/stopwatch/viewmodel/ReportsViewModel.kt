package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

sealed class ReportsState {
    object Loading : ReportsState()
    data class Success(
        val totalHours: String,
        val totalEarnings: String,
        val totalSessions: String,
        val todayEarnings: String
    ) : ReportsState()
    object Empty : ReportsState()
    data class Error(val message: String) : ReportsState()
}

class ReportsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsState>(ReportsState.Loading)
    val uiState: StateFlow<ReportsState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            try {
                repository.getSessions().collectLatest { sessions ->
                    if (sessions.isEmpty()) {
                        _uiState.value = ReportsState.Empty
                    } else {
                        _uiState.value = calculateReports(sessions)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ReportsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun calculateReports(sessions: List<Session>): ReportsState {
        var totalMillis = 0L
        var totalEarnings = 0.0
        var todayEarnings = 0.0
        val totalSessions = sessions.size

        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        for (session in sessions) {
            totalMillis += session.durationMillis ?: 0L
            totalEarnings += session.earnings ?: 0.0
            
            val sessionTime = session.timestamp ?: 0L
            calendar.timeInMillis = sessionTime
            if (calendar.get(java.util.Calendar.DAY_OF_YEAR) == currentDay &&
                calendar.get(java.util.Calendar.YEAR) == currentYear) {
                todayEarnings += session.earnings ?: 0.0
            }
        }

        val totalHours = totalMillis / (1000 * 60 * 60)
        val totalMinutes = (totalMillis / (1000 * 60)) % 60
        
        val totalHoursWorkedStr = String.format(Locale.getDefault(), "%dh %dm", totalHours, totalMinutes)
        val totalEarningsStr = String.format(Locale.getDefault(), "₹%,.2f", totalEarnings)
        val totalSessionsStr = totalSessions.toString()
        val todayEarningsStr = String.format(Locale.getDefault(), "₹%,.2f", todayEarnings)

        return ReportsState.Success(
            totalHoursWorkedStr,
            totalEarningsStr,
            totalSessionsStr,
            todayEarningsStr
        )
    }
}