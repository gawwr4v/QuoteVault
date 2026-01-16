package com.quotevault.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotevault.domain.model.Quote
import com.quotevault.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val searchResults: List<Quote> = emptyList(),
    val availableCollections: List<com.quotevault.domain.model.QuoteCollection> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: QuoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    
    // separate flow for debouncing
    private val _searchDebouncer = MutableStateFlow<Pair<String, String?>>("" to null)

    init {
        loadCategories()
        loadCollections()
        setupSearchSubscription()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val result = repository.getCategories()
            if (result.isSuccess) {
                _uiState.update { it.copy(availableCategories = result.getOrNull() ?: emptyList()) }
            }
            // Add "All" or similar is not needed as null = all
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchSubscription() {
        _searchDebouncer
            .debounce(350L) // 350ms debounce
            .distinctUntilChanged()
            .onEach { (query, category) ->
                performSearch(query, category)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchDebouncer.value = query to _uiState.value.selectedCategory
    }

    fun onCategorySelected(category: String?) {
        val newCategory = if (category == _uiState.value.selectedCategory) null else category
        _uiState.update { it.copy(selectedCategory = newCategory) }
        _searchDebouncer.value = _uiState.value.searchQuery to newCategory
    }
    
    // Actions from Cards (Delegate to repository, but need to update local state optimistically or re-fetch)
    fun toggleFavorite(quote: Quote) {
        viewModelScope.launch {
            // Optimistic update
            val currentList = _uiState.value.searchResults
            val updatedList = currentList.map { 
                if (it.id == quote.id) it.copy(isFavorite = !it.isFavorite) else it
            }
            _uiState.update { it.copy(searchResults = updatedList) }
            
            val result = repository.toggleFavorite(quote)
            if (result.isFailure) {
                // Revert
                _uiState.update { it.copy(searchResults = currentList) }
            }
        }
    }
    
    // Collections Logic
    fun loadCollections() {
        viewModelScope.launch {
            repository.getCollections()
                .onSuccess { collections ->
                    _uiState.update { it.copy(availableCollections = collections) }
                }
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            repository.createCollection(name)
                .onSuccess {
                    loadCollections() // Refresh
                }
        }
    }
    
    fun addToCollection(quote: Quote, collectionId: String) {
        viewModelScope.launch {
            repository.addQuoteToCollection(collectionId, quote.id)
            // Optional: Show snackbar or feedback
        }
    }

    private suspend fun performSearch(query: String, category: String?) {
        if (query.isBlank() && category == null) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        // Pass query and category to repository
        // Note: getQuotes supports filtering. We'll use getQuotes with search param.
        val result = repository.getQuotes(
            page = 1, 
            pageSize = 50, // Reasonable limit for search results
            searchQuery = query.takeIf { it.isNotBlank() },
            category = category,
            author = null // we search author via text query
        )

        _uiState.update { 
            it.copy(
                isLoading = false,
                searchResults = result.getOrElse { emptyList() },
                error = result.exceptionOrNull()?.message
            )
        }
    }
}
