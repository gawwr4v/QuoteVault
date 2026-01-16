package com.quotevault.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quotevault.ui.components.CategoryChips
import com.quotevault.ui.components.QuoteCard
import com.quotevault.ui.components.QuoteCardStyle

@Composable
fun SearchScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToShare: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus on entry
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Collection Selection Dialog
    var showCollectionDialog by remember { mutableStateOf(false) }
    var selectedQuoteForCollection by remember { mutableStateOf<com.quotevault.domain.model.Quote?>(null) }
    var newCollectionName by remember { mutableStateOf("") }
    var showCreateCollectionInput by remember { mutableStateOf(false) }

    if (showCollectionDialog && selectedQuoteForCollection != null) {
        val quote = selectedQuoteForCollection!!
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Collection") },
            text = {
                Column {
                    if (showCreateCollectionInput) {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            label = { Text("Collection Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(uiState.availableCollections) { collection ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToCollection(quote, collection.id)
                                            showCollectionDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(collection.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            if (uiState.availableCollections.isEmpty()) {
                                item { 
                                    Text("No collections found.", modifier = Modifier.padding(12.dp)) 
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showCreateCollectionInput) {
                            if (newCollectionName.isNotBlank()) {
                                viewModel.createCollection(newCollectionName)
                                showCreateCollectionInput = false
                                newCollectionName = ""
                            }
                        } else {
                            showCreateCollectionInput = true
                        }
                    }
                ) {
                    Text(if (showCreateCollectionInput) "Create" else "New Collection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCollectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                SearchHeader(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onQueryChange,
                    onClearClick = { viewModel.onQueryChange("") },
                    focusRequester = focusRequester
                )
                
                // Optional Filter Chips
                if (uiState.availableCategories.isNotEmpty()) {
                    CategoryChips(
                        categories = uiState.availableCategories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = viewModel::onCategorySelected,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.searchQuery.isBlank() && uiState.selectedCategory == null) {
                // Empty State
                EmptySearchState()
            } else if (uiState.searchResults.isEmpty() && !uiState.isLoading) {
                // No Results State
                NoResultsState(query = uiState.searchQuery)
            } else {
                // Results List
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 16.dp, 
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.searchResults, key = { it.id }) { quote ->
                         QuoteCard(
                             quote = quote,
                             onFavoriteClick = { viewModel.toggleFavorite(quote) },
                             onShareClick = { onNavigateToShare(quote.id) },
                             onAddToCollectionClick = { 
                                 selectedQuoteForCollection = quote
                                 showCollectionDialog = true
                             },
                             onItemClick = { onNavigateToDetail(quote.id) },
                             style = QuoteCardStyle.Standard
                         )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("Search quotes, authors...", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search, 
            contentDescription = null,
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "Discover Quotes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Type to search by text or author",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoResultsState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No results found for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
