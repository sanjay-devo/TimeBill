package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SessionDetailsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    fun getSession(sessionId: String): Flow<Session?> = repository.getSession(sessionId)

    fun getProfile(): Flow<UserProfile?> = repository.getProfile()

    fun updateSessionStatus(sessionId: String, status: String, receiptNumber: String) {
        viewModelScope.launch {
            repository.updateSessionStatus(sessionId, status, receiptNumber)
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}