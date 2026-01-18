package com.quotevault.domain.repository

import com.quotevault.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setTheme(theme: String)
    suspend fun setFontScale(scale: Float)
    suspend fun updateDailyQuote(id: String?, content: String, author: String?)
    suspend fun setNotificationTime(time: String)
    suspend fun setNotificationMode(mode: String)
    suspend fun setNotificationInterval(interval: Int)
    suspend fun setAccentColor(color: String)
    suspend fun updateNotificationCategories(categories: List<String>)
}
