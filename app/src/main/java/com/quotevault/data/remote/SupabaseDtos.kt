package com.quotevault.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String?,
    @SerialName("full_name") val fullName: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    val preferences: ProfilePreferences? = null
)

@Serializable
data class ProfilePreferences(
    val theme: String? = "system",
    @SerialName("font_scale") val fontScale: Float? = 1.0f
)

@Serializable
data class QuoteDto(
    val id: String,
    val text: String, // DB column is 'text' now, not 'content'
    val author: String?,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("categories") val categoryData: CategoryDto? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CategoryDto(
    val id: String? = null,
    val name: String
)

@Serializable
data class UserFavoriteDto(
    val id: String? = null, // Auto-generated UUID
    @SerialName("user_id") val userId: String,
    @SerialName("quote_id") val quoteId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CollectionDto(
    val id: String? = null, // Auto-generated UUID
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CollectionQuoteDto(
    val id: String? = null,
    @SerialName("list_id") val listId: String,
    @SerialName("quote_id") val quoteId: String
)
