package com.quotevault.ui.screens.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotevault.domain.model.Quote
import com.quotevault.domain.model.RepoError
import com.quotevault.domain.repository.AuthRepository
import com.quotevault.domain.repository.AuthState
import com.quotevault.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val authRepository: AuthRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: String = savedStateHandle["collectionId"] ?: "favorites"
    private val prefs = context.getSharedPreferences("collection_descriptions", android.content.Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    init {
        // Set initial title immediately based on collectionId
        if (collectionId == "favorites") {
            _uiState.update { it.copy(title = "Liked") }
        }
        
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    if (collectionId != "favorites") {
                        loadCollectionInfo()
                    }
                    loadQuotes()
                } else if (authState is AuthState.Unauthenticated) {
                    _uiState.update { it.copy(quotes = emptyList(), error = "Please sign in to view this collection") }
                }
            }
        }
    }

    private fun loadCollectionInfo() {
        if (collectionId == "favorites") {
            _uiState.update { it.copy(title = "Liked") }
            return
        }
        
        // Load description from local storage
        val savedDescription = prefs.getString("desc_$collectionId", null)
        _uiState.update { it.copy(description = savedDescription) }
        
        viewModelScope.launch {
            val result = quoteRepository.getCollection(collectionId)
            if (result.isSuccess) {
                val collection = result.getOrNull()
                _uiState.update { it.copy(title = collection?.name ?: "Collection") }
            }
        }
    }

    fun loadQuotes() {
        if (authRepository.authState.value !is AuthState.Authenticated) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (collectionId == "favorites") {
                quoteRepository.getFavorites()
            } else {
                quoteRepository.getCollectionQuotes(collectionId)
            }
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        quotes = result.getOrDefault(emptyList()),
                        isLoading = false
                    ) 
                }
            } else {
                val error = result.exceptionOrNull()
                val message = if (error is RepoError.AuthRequired) "Please sign in" else error?.message
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    // Toggle favorite status (heart button) - doesn't remove from custom collections
    fun toggleFavorite(quote: Quote) {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val result = quoteRepository.toggleFavorite(quote)
            if (result.isSuccess) {
                // Update the favorite status in UI
                _uiState.update { state ->
                    state.copy(quotes = state.quotes.map { q ->
                        if (q.id == quote.id) q.copy(isFavorite = !quote.isFavorite) else q
                    })
                }
                // If this is the Liked collection and we unfavorited, remove from list
                if (collectionId == "favorites" && quote.isFavorite) {
                    _uiState.update { state ->
                        state.copy(quotes = state.quotes.filter { it.id != quote.id })
                    }
                }
            }
        }
    }

    // Remove quote from this collection (not from favorites)
    fun removeFromCollection(quote: Quote) {
        if (authRepository.authState.value !is AuthState.Authenticated) return
        if (collectionId == "favorites") return // Use toggleFavorite for favorites

        viewModelScope.launch {
            val previousList = _uiState.value.quotes
            _uiState.update { it.copy(quotes = it.quotes.filter { item -> item.id != quote.id }) }
            
            val result = quoteRepository.removeQuoteFromCollection(collectionId, quote.id)
            if (result.isFailure) {
                _uiState.update { it.copy(quotes = previousList, error = "Failed to remove quote") }
            }
        }
    }

    fun getCollectionId(): String = collectionId
    
    fun isFavoritesCollection(): Boolean = collectionId == "favorites"

    fun updateCollection(newName: String, newDescription: String?) {
        if (authRepository.authState.value !is AuthState.Authenticated) return
        if (collectionId == "favorites") return

        viewModelScope.launch {
            val result = quoteRepository.renameCollection(collectionId, newName)
            if (result.isSuccess) {
                _uiState.update { it.copy(title = newName, description = newDescription) }
                // Save description locally
                prefs.edit().putString("desc_$collectionId", newDescription).apply()
            } else {
                _uiState.update { it.copy(error = "Failed to update collection") }
            }
        }
    }
    
    // Legacy function for backwards compatibility
    fun renameCollection(newName: String) {
        updateCollection(newName, _uiState.value.description)
    }

    fun deleteCollection(onDeleted: () -> Unit) {
        if (authRepository.authState.value !is AuthState.Authenticated) return
        if (collectionId == "favorites") return

        viewModelScope.launch {
            val result = quoteRepository.deleteCollection(collectionId)
            if (result.isSuccess) {
                onDeleted()
            } else {
                _uiState.update { it.copy(error = "Failed to delete collection") }
            }
        }
    }

    fun removeMultipleQuotes(quoteIds: Set<String>, onComplete: () -> Unit) {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val previousList = _uiState.value.quotes
            _uiState.update { it.copy(quotes = it.quotes.filter { q -> q.id !in quoteIds }) }

            var success = true
            for (quoteId in quoteIds) {
                val result = if (collectionId == "favorites") {
                    val quote = previousList.find { it.id == quoteId }
                    if (quote != null) quoteRepository.toggleFavorite(quote) else Result.success(Unit)
                } else {
                    quoteRepository.removeQuoteFromCollection(collectionId, quoteId)
                }
                if (result.isFailure) {
                    success = false
                    break
                }
            }

            if (!success) {
                _uiState.update { it.copy(quotes = previousList, error = "Failed to remove some quotes") }
            }
            onComplete()
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val sorted = when (order) {
                SortOrder.MOST_RECENT -> state.quotes.sortedByDescending { it.id } // Assuming newer = higher ID
                SortOrder.OLDEST -> state.quotes.sortedBy { it.id }
            }
            state.copy(quotes = sorted, sortOrder = order)
        }
    }
}

enum class SortOrder {
    MOST_RECENT,
    OLDEST
}

data class CollectionDetailUiState(
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val description: String? = null,
    val sortOrder: SortOrder = SortOrder.MOST_RECENT
)
