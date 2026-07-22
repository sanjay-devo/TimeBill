package com.timebill.stopwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebill.stopwatch.model.UserProfile
import com.timebill.stopwatch.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _event = MutableSharedFlow<EditProfileEvent>()
    val event: SharedFlow<EditProfileEvent> = _event

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getProfile().collectLatest { profile ->
                _profile.value = profile
                _isLoading.value = false
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveProfile(profile)
                _event.emit(EditProfileEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(EditProfileEvent.Error(e.message ?: "Failed to save profile"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class EditProfileEvent {
        object SaveSuccess : EditProfileEvent()
        data class Error(val message: String) : EditProfileEvent()
    }
}