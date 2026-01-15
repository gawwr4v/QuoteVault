package com.quotevault.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotevault.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is com.quotevault.domain.repository.AuthState.Authenticated -> {
                        _uiState.value = AuthUiState.Success
                    }
                    is com.quotevault.domain.repository.AuthState.Loading -> {
                        _uiState.value = AuthUiState.Loading
                    }
                    else -> {
                        // Keep Idle or Error state if not authenticated, don't force Error
                        if (_uiState.value is AuthUiState.Loading) {
                             _uiState.value = AuthUiState.Idle
                        }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        android.util.Log.d("AuthVM", "signIn called with email=$email")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signIn(email, password)
            android.util.Log.d("AuthVM", "signIn result: isSuccess=${result.isSuccess}")
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signUp(email, password, fullName)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.PasswordResetSent
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    object PasswordResetSent : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
