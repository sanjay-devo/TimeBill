package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.Client
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SessionDetailsViewModel(private val repository: FirebaseRepository) : ViewModel() {

    fun getSession(sessionId: String): Flow<Session?> = repository.getSession(sessionId)

    fun getProfile(): Flow<UserProfile?> = repository.getProfile()

    fun getClients(): Flow<List<Client>> = repository.getClients()

    fun updateSessionDetails(sessionId: String, updates: Map<String, Any?>) {
        viewModelScope.launch {
            repository.updateSessionDetails(sessionId, updates)
        }
    }

    fun updateSessionStatus(sessionId: String, status: String, invoiceNumber: String) {
        viewModelScope.launch {
            repository.updateSessionStatus(sessionId, status, invoiceNumber)
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