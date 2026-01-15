package com.quotevault.domain.model

data class QuoteCollection(
    val id: String,
    val name: String,
    val userId: String,
    val description: String? = null
)
