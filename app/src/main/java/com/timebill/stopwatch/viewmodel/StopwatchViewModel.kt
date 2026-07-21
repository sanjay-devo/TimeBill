package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class StopwatchViewModel(private val repository: FirebaseRepository) : ViewModel() {

    private val _timerText = MutableStateFlow("00:00:00")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    private val _earningsText = MutableStateFlow("₹0.00")
    val earningsText: StateFlow<String> = _earningsText.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _defaultHourlyRate = MutableStateFlow<Double?>(null)
    val defaultHourlyRate: StateFlow<Double?> = _defaultHourlyRate.asStateFlow()

    private var timerJob: Job? = null
    private var startTime = 0L
    private var accumulatedTime = 0L
    private var hourlyRate = 0.0
    var clientName = ""

    init {
        loadDefaultHourlyRate()
    }

    private fun loadDefaultHourlyRate() {
        viewModelScope.launch {
            repository.getDefaultHourlyRate().collect { rate ->
                _defaultHourlyRate.value = rate
            }
        }
    }

    fun updateDefaultHourlyRate(rate: Double) {
        if (_defaultHourlyRate.value == rate) return

        viewModelScope.launch {
            try {
                repository.saveDefaultHourlyRate(rate)
            } catch (_: Exception) {
                // Log error or handle it
            }
        }
    }

    fun toggleStartPause(rate: Double, name: String) {
        if (!_isRunning.value) {
            startTimer(rate, name)
        } else {
            if (_isPaused.value) {
                resumeTimer()
            } else {
                pauseTimer()
            }
        }
    }

    fun startTimer(rate: Double, name: String) {
        if (_isRunning.value && !_isPaused.value) return
        
        hourlyRate = rate
        clientName = name
        
        if (!_isPaused.value) {
            startTime = System.currentTimeMillis()
            accumulatedTime = 0L
        } else {
            startTime = System.currentTimeMillis()
        }
        
        _isRunning.value = true
        _isPaused.value = false
        
        runTimer()
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isRunning.value && !_isPaused.value) {
                val currentDuration = accumulatedTime + (System.currentTimeMillis() - startTime)
                _timerText.value = formatDuration(currentDuration)
                _earningsText.value = formatEarnings(currentDuration)
                delay(1000)
            }
        }
    }

    fun pauseTimer() {
        if (!_isRunning.value || _isPaused.value) return
        
        accumulatedTime += (System.currentTimeMillis() - startTime)
        _isPaused.value = true
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (!_isRunning.value || !_isPaused.value) return
        startTime = System.currentTimeMillis()
        _isPaused.value = false
        runTimer()
    }

    fun stopTimer(): Session {
        val finalDuration = if (_isPaused.value) accumulatedTime else accumulatedTime + (System.currentTimeMillis() - startTime)
        val finalEarnings = (finalDuration / 3600000.0) * hourlyRate
        
        val session = Session(
            clientName = clientName,
            hourlyRate = hourlyRate,
            startTime = System.currentTimeMillis() - finalDuration,
            durationMillis = finalDuration,
            earnings = finalEarnings
        )
        
        resetTimer()
        return session
    }

    private fun resetTimer() {
        _isRunning.value = false
        _isPaused.value = false
        timerJob?.cancel()
        _timerText.value = "00:00:00"
        _earningsText.value = "₹0.00"
        accumulatedTime = 0L
    }

    fun saveSession(session: Session) {
        viewModelScope.launch {
            repository.saveSession(session)
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatEarnings(millis: Long): String {
        val hours = millis / 3600000.0
        return String.format(Locale.getDefault(), "₹%.2f", hours * hourlyRate)
    }
}