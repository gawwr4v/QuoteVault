package com.quotevault.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quotevault.domain.model.UserPreferences
import com.quotevault.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.quotevault.domain.model.UserProfile
import com.quotevault.domain.repository.AuthRepository
import com.quotevault.domain.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val workManager: WorkManager,
    private val quoteRepository: com.quotevault.domain.repository.QuoteRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories = _availableCategories.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        val user = authState.user
                        _userEmail.value = user.email ?: ""
                        _userName.value = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: ""
                        loadUserProfile()
                        loadCategories()
                    }
                    else -> {
                        _userEmail.value = "Not Signed In"
                        _userName.value = ""
                        _userProfile.value = null // Clear profile on logout
                        // Maybe clear preferences or set defaults
                    }
                }
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            val result = quoteRepository.getCategories()
            if (result.isSuccess) {
                 _availableCategories.value = result.getOrThrow()
            }
        }
    }

    // Load full profile for preferences etc (kept separate)
    private fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getCurrentProfile()
            if (result.isSuccess) {
                _userProfile.update { result.getOrThrow() }
            } else {
                 android.util.Log.e("SettingsVM", "Failed to load profile", result.exceptionOrNull())
            }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setFontScale(scale)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    suspend fun logoutAndWait() {
        authRepository.signOut()
    }
    
    fun updateFullName(newName: String) {
        _userProfile.update { current ->
            current?.copy(fullName = newName)
        }
        // Note: For full persistence, implement backend API call here
    }
    
    fun updateNotificationTime(hour: Int, minute: Int) {
        val timeString = String.format("%02d:%02d", hour, minute)
        viewModelScope.launch {
            settingsRepository.setNotificationTime(timeString)
            // Use current mode/interval
            val prefs = userPreferences.value
            rescheduleDailyQuoteWorker(hour, minute, prefs.notificationMode, prefs.notificationInterval)
        }
    }

    fun updateNotificationMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setNotificationMode(mode)
            val prefs = userPreferences.value
            // Parse time
            val timeParts = prefs.notificationTime.split(":")
            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            
            rescheduleDailyQuoteWorker(h, m, mode, prefs.notificationInterval)
        }
    }

    fun updateNotificationInterval(interval: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationInterval(interval)
            val prefs = userPreferences.value
            val timeParts = prefs.notificationTime.split(":")
            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            
            rescheduleDailyQuoteWorker(h, m, prefs.notificationMode, interval)
        }
    }
    
    fun updateNotificationCategories(categories: List<String>) {
         viewModelScope.launch {
            settingsRepository.updateNotificationCategories(categories)
            // No need to reschedule, logic happens at runtime. 
            // BUT if we want to fetch a quote NOW matching these, we might. 
            // For now, just save.
        }
    }

    fun updateAccentColor(color: String) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(color)
        }
    }

    private fun rescheduleDailyQuoteWorker(hour: Int, minute: Int, mode: String, interval: Int) {
        // Cancel old work
        workManager.cancelUniqueWork("DailyQuoteWork") // Old periodic
        workManager.cancelUniqueWork("DailyQuoteOneTime") // Cancel existing to replace
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.quotevault.worker.DailyQuoteWorker>()
        
        val delay: Long = if (mode == "frequency") {
             TimeUnit.HOURS.toMillis(interval.toLong())
        } else {
             val now = java.time.LocalDateTime.now()
             var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
             
             // If target time has already passed today, schedule for tomorrow
             // Use strict comparison: if target is before or equal to now, schedule tomorrow
             if (!target.isAfter(now)) {
                 target = target.plusDays(1)
             }
             java.time.Duration.between(now, target).toMillis()
        }

        android.util.Log.d("SettingsVM", "Scheduling notification: mode=$mode, hour=$hour, min=$minute, delay=${delay}ms (${delay / 60000} minutes)")
        
        workRequest.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        
        // Use REPLACE to ensure our new schedule takes effect
        workManager.enqueueUniqueWork(
            "DailyQuoteOneTime",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest.build()
        )
    }
}
