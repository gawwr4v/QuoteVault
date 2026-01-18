package com.quotevault.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val preferences: UserPreferences
)

data class UserPreferences(
    val theme: String = "system",
    val fontScale: Float = 1.0f,
    val dailyQuoteId: String? = null,
    val dailyQuoteContent: String? = null,
    val dailyQuoteAuthor: String? = null,
    val notificationTime: String = "08:00", // Default 8 AM for daily notifications
    val notificationMode: String = "daily", // "daily" or "frequency"
    val notificationInterval: Int = 24, // hours, used if mode is "frequency"
    val accentColor: String = "purple", // purple, blue, green, orange
    val notificationCategories: List<String> = emptyList() // motivation, love, life, etc.
)
