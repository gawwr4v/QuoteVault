package com.quotevault.ui.screens.home

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
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.quotevault.domain.repository.SettingsRepository
import com.quotevault.widget.QuoteWidget
import androidx.glance.appwidget.updateAll

@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val descriptionPrefs = context.getSharedPreferences("collection_descriptions", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // only load basic categories and authors that don't depend on user
        loadCategories()
        loadAuthors()
        loadDailyQuote()
        
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        // user logged in: load everything
                        loadQuotes(reset = true)
                        loadCollections()
                        loadSavedQuoteIds()
                    }
                    is AuthState.Unauthenticated -> {
                        // user logged out: load public quotes, clear user data
                        loadQuotes(reset = true)
                        _uiState.update { it.copy(userCollections = emptyList(), favoritesCount = 0) }
                    }
                    AuthState.Loading -> {
                        // Wait
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun loadQuotes(reset: Boolean = false) {
        if (reset) {
            _uiState.update { it.copy(page = 1, quotes = emptyList(), isLoading = true, error = null) }
        } else {
             _uiState.update { it.copy(isLoading = true, error = null) }
        }

        viewModelScope.launch {
            val currentState = _uiState.value
            val result = quoteRepository.getQuotes(
                page = currentState.page,
                searchQuery = currentState.searchQuery,
                category = currentState.selectedCategory,
                author = currentState.selectedAuthor
            )

            if (result.isSuccess) {
                val fetchedQuotes = result.getOrDefault(emptyList())
                // Shuffle if refreshing main feed to give a random feel
                val newQuotes = if (reset && currentState.searchQuery.isBlank() && currentState.selectedCategory == null && currentState.selectedAuthor == null) {
                    fetchedQuotes.shuffled() 
                } else {
                    fetchedQuotes
                }
                
                _uiState.update { 
                    it.copy(
                        quotes = if (reset) newQuotes else it.quotes + newQuotes,
                        isLoading = false,
                        page = it.page + 1,
                        endReached = newQuotes.isEmpty()
                    ) 
                }
            } else {
                val error = result.exceptionOrNull()
                _uiState.update { it.copy(isLoading = false, error = error?.message) }
            }
        }
    }
    
    fun loadAuthors() {
        viewModelScope.launch {
            val result = quoteRepository.getAuthors()
            if (result.isSuccess) {
                _uiState.update { it.copy(authors = result.getOrDefault(emptyList())) }
            }
        }
    }
    
    fun loadDailyQuote() {
        viewModelScope.launch {
            val result = quoteRepository.getDailyQuote()
            if (result.isSuccess) {
                val quote = result.getOrNull()
                _uiState.update { it.copy(dailyQuote = quote) }
                
                // sync to widget/settings so it's not empty
                if (quote != null) {
                    try {
                        settingsRepository.updateDailyQuote(quote.id, quote.content, quote.author)
                        QuoteWidget().updateAll(context)
                    } catch (e: Exception) {
                        android.util.Log.e("QuoteVM", "failed to sync widget", e)
                    }
                }
            }
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            val result = quoteRepository.getCategories()
             if (result.isSuccess) {
                _uiState.update { it.copy(categories = result.getOrDefault(emptyList())) }
            }
        }
    }

    fun loadCollections() {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCollections = true) }
            
            val result = quoteRepository.getCollections()
            if (result.isSuccess) {
                // Merge with locally stored descriptions
                val collections = result.getOrDefault(emptyList()).map { collection ->
                    val localDescription = descriptionPrefs.getString("desc_${collection.id}", null)
                    collection.copy(description = localDescription)
                }
                _uiState.update { it.copy(userCollections = collections) }
            }
            // Also fetch favorites count
            val favResult = quoteRepository.getFavoritesCount()
            if (favResult.isSuccess) {
                _uiState.update { it.copy(favoritesCount = favResult.getOrDefault(0)) }
            }
            
            _uiState.update { it.copy(isLoadingCollections = false) }
        }
    }

    fun loadSavedQuoteIds() {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val result = quoteRepository.getAllSavedQuoteIds()
            if (result.isSuccess) {
                _uiState.update { it.copy(savedQuoteIds = result.getOrDefault(emptySet())) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadQuotes(reset = true)
    }

    fun onCategorySelected(category: String?) {
        val newCategory = if (_uiState.value.selectedCategory == category) null else category
        _uiState.update { it.copy(selectedCategory = newCategory, selectedAuthor = null) }
        loadQuotes(reset = true)
    }

    fun onAuthorSelected(author: String?) {
        val newAuthor = if (_uiState.value.selectedAuthor == author) null else author
        _uiState.update { it.copy(selectedAuthor = newAuthor, selectedCategory = null) } // Clear category if author selected? Or allow both? Let's clear to avoid empty results for now
        loadQuotes(reset = true)
    }

    fun createCollection(name: String) {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val result = quoteRepository.createCollection(name)
            if (result.isSuccess) {
                loadCollections()
            } else {
                 _uiState.update { it.copy(error = "Failed to create collection: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun addQuoteToCollection(collectionId: String, quoteId: String) {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val result = quoteRepository.addQuoteToCollection(collectionId, quoteId)
            if (result.isSuccess) {
                // Track that this quote is now saved to a collection
                _uiState.update { it.copy(savedQuoteIds = it.savedQuoteIds + quoteId) }
            } else {
                _uiState.update { it.copy(error = "Failed to add to collection: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        if (authRepository.authState.value !is AuthState.Authenticated) return

        viewModelScope.launch {
            val result = quoteRepository.deleteCollection(collectionId)
            if (result.isSuccess) {
                loadCollections()
            } else {
                _uiState.update { it.copy(error = "Failed to delete collection: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun toggleFavorite(quote: Quote) {
        if (authRepository.authState.value !is AuthState.Authenticated) {
            _uiState.update { it.copy(error = "Please sign in to add favorites") }
            return
        }

        viewModelScope.launch {
            val originalIsFavorite = quote.isFavorite
            val targetIsFavorite = !originalIsFavorite
            val quoteId = quote.id
            
            // 1. optimistic update
            _uiState.update { state ->
                state.copy(quotes = state.quotes.map { 
                    if (it.id == quoteId) it.copy(isFavorite = targetIsFavorite) else it 
                })
            }
            
            // 2. network call
            val result = quoteRepository.toggleFavorite(quote)
            
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error is RepoError.AuthRequired) {
                     _uiState.update { it.copy(error = "Please sign in to add favorites") }
                } else {
                    android.util.Log.e("QuoteVM", "toggleFavorite failed for $quoteId", error)
                     _uiState.update { it.copy(error = "Failed to update favorite") }
                }

                // 3. revert on failure
                _uiState.update { state ->
                    state.copy(quotes = state.quotes.map { 
                        if (it.id == quoteId) it.copy(isFavorite = originalIsFavorite) else it 
                    }) 
                }
            } else {
                android.util.Log.d("QuoteVM", "toggleFavorite success for $quoteId")
                loadCollections() // Refresh fav count if needed, or separate call
            }
        }
    }
}

data class HomeUiState(
    val quotes: List<Quote> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingCollections: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val page: Int = 1,
    val endReached: Boolean = false,
    val userCollections: List<com.quotevault.domain.model.QuoteCollection> = emptyList(),
    val favoritesCount: Int = 0,
    val savedQuoteIds: Set<String> = emptySet(),
    val authors: List<String> = emptyList(),
    val selectedAuthor: String? = null,
    val dailyQuote: Quote? = null
)
