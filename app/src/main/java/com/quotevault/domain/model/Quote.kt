package com.quotevault.domain.model

data class Quote(
    val id: String,
    val content: String,
    val author: String?,
    val category: String,
    val isFavorite: Boolean = false
)
