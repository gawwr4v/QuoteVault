package com.quotevault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quotevault.domain.model.UserPreferences
import com.quotevault.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
    private val DAILY_QUOTE_ID_KEY = stringPreferencesKey("daily_quote_id")
    private val DAILY_QUOTE_CONTENT_KEY = stringPreferencesKey("daily_quote_content")
    private val DAILY_QUOTE_AUTHOR_KEY = stringPreferencesKey("daily_quote_author")
    private val NOTIFICATION_TIME_KEY = stringPreferencesKey("notification_time")
    private val NOTIFICATION_MODE_KEY = stringPreferencesKey("notification_mode")
    private val NOTIFICATION_INTERVAL_KEY = androidx.datastore.preferences.core.intPreferencesKey("notification_interval")
    private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
    private val NOTIFICATION_CATEGORIES_KEY = androidx.datastore.preferences.core.stringSetPreferencesKey("notification_categories")

    override val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                theme = preferences[THEME_KEY] ?: "system",
                fontScale = preferences[FONT_SCALE_KEY] ?: 1.0f,
                dailyQuoteId = preferences[DAILY_QUOTE_ID_KEY],
                dailyQuoteContent = preferences[DAILY_QUOTE_CONTENT_KEY],
                dailyQuoteAuthor = preferences[DAILY_QUOTE_AUTHOR_KEY],
                notificationTime = preferences[NOTIFICATION_TIME_KEY] ?: "08:00",
                notificationMode = preferences[NOTIFICATION_MODE_KEY] ?: "daily",
                notificationInterval = preferences[NOTIFICATION_INTERVAL_KEY] ?: 24,
                accentColor = preferences[ACCENT_COLOR_KEY] ?: "purple",
                notificationCategories = preferences[NOTIFICATION_CATEGORIES_KEY]?.toList() ?: emptyList()
            )
        }

    override suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    override suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE_KEY] = scale
        }
    }
    
    override suspend fun updateDailyQuote(id: String?, content: String, author: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[DAILY_QUOTE_ID_KEY] = id
            } else {
                preferences.remove(DAILY_QUOTE_ID_KEY)
            }
            preferences[DAILY_QUOTE_CONTENT_KEY] = content
            if (author != null) {
                preferences[DAILY_QUOTE_AUTHOR_KEY] = author
            } else {
                preferences.remove(DAILY_QUOTE_AUTHOR_KEY)
            }
        }
    }

    override suspend fun setNotificationTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIME_KEY] = time
        }
    }

    override suspend fun setNotificationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_MODE_KEY] = mode
        }
    }

    override suspend fun setNotificationInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_INTERVAL_KEY] = interval
        }
    }

    override suspend fun setAccentColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_COLOR_KEY] = color
        }
    }

    override suspend fun updateNotificationCategories(categories: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_CATEGORIES_KEY] = categories.toSet()
        }
    }
}
