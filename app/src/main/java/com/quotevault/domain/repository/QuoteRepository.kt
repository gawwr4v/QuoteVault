package com.quotevault.domain.repository

import com.quotevault.domain.model.Quote

interface QuoteRepository {
    suspend fun getQuotes(page: Int, pageSize: Int = 20, searchQuery: String? = null, category: String? = null, author: String? = null): Result<List<Quote>>
    suspend fun getAuthors(): Result<List<String>>
    suspend fun toggleFavorite(quote: Quote): Result<Unit>
    suspend fun getFavorites(): Result<List<Quote>>
    suspend fun getFavoritesCount(): Result<Int>
    suspend fun getCategories(): Result<List<String>>
    
    // Collections
    suspend fun createCollection(name: String): Result<Unit>
    suspend fun getCollections(): Result<List<com.quotevault.domain.model.QuoteCollection>>
    suspend fun addQuoteToCollection(collectionId: String, quoteId: String): Result<Unit>
    suspend fun removeQuoteFromCollection(collectionId: String, quoteId: String): Result<Unit>
    suspend fun deleteCollection(collectionId: String): Result<Unit>
    suspend fun renameCollection(collectionId: String, newName: String): Result<Unit>
    
    suspend fun getDailyQuote(categories: List<String> = emptyList()): Result<Quote>
    suspend fun getQuote(id: String): Result<Quote>
    suspend fun getCollectionQuotes(collectionId: String): Result<List<Quote>>
    suspend fun getCollection(collectionId: String): Result<com.quotevault.domain.model.QuoteCollection?>
    suspend fun getAllSavedQuoteIds(): Result<Set<String>>
}
