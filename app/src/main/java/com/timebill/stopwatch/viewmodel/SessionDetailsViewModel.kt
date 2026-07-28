package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.Client
import com.timebill.stopwatch.model.Session
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.EmailRepository
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SessionDetailsViewModel(
    private val repository: FirebaseRepository,
    private val emailRepository: EmailRepository = EmailRepository()
) : ViewModel() {

    private val _emailStatus = MutableLiveData<EmailStatus>()
    val emailStatus: LiveData<EmailStatus> = _emailStatus

    sealed class EmailStatus {
        object Idle : EmailStatus()
        object Loading : EmailStatus()
        data class Success(val message: String) : EmailStatus()
        data class Error(val message: String) : EmailStatus()
    }

    fun sendInvoiceEmail(session: Session, profile: UserProfile, pdfFile: File) {
        _emailStatus.value = EmailStatus.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                emailRepository.sendInvoiceEmail(session, profile, pdfFile)
            }
            result.onSuccess {
                _emailStatus.value = EmailStatus.Success(it)
            }.onFailure {
                _emailStatus.value = EmailStatus.Error(it.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetEmailStatus() {
        _emailStatus.value = EmailStatus.Idle
    }

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