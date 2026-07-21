package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.repository.FirebaseRepository
import com.timebill.stopwatch.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private var repository: FirebaseRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _guestId = MutableStateFlow(preferenceManager.getGuestId())
    val guestId: StateFlow<String> = _guestId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _event = MutableSharedFlow<ProfileEvent>()
    val event: SharedFlow<ProfileEvent> = _event

    fun deleteAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteAllSessions()
                _event.emit(ProfileEvent.DataDeleted)
            } catch (e: Exception) {
                _event.emit(ProfileEvent.Error(e.message ?: "Failed to delete data"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGuestAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Delete from Firebase
                repository.deleteAccount()
                
                // 2. Clear SharedPreferences
                preferenceManager.clearAllData()
                
                // 3. Generate new Guest ID (happens on getGuestId)
                val newGuestId = preferenceManager.getGuestId()
                _guestId.value = newGuestId
                
                // 4. Update repository with new guestId
                repository = FirebaseRepository(newGuestId)
                
                // 5. Automatically create profile node
                repository.saveDefaultHourlyRate(0.0) // Minimal profile initialization
                
                _event.emit(ProfileEvent.AccountDeleted)
            } catch (e: Exception) {
                _event.emit(ProfileEvent.Error(e.message ?: "Failed to delete account"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class ProfileEvent {
        object DataDeleted : ProfileEvent()
        object AccountDeleted : ProfileEvent()
        data class Error(val message: String) : ProfileEvent()
    }
}