package com.quotevault.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import com.quotevault.domain.model.Quote
import com.quotevault.ui.components.*
import com.quotevault.ui.screens.settings.SettingsViewModel

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onNavigateToShare: (String) -> Unit,
    viewModel: QuoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by settingsViewModel.userProfile.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showCollectionDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedQuoteForCollection by remember { mutableStateOf<Quote?>(null) }
    var selectedQuoteForShare by remember { mutableStateOf<Quote?>(null) }
    var newCollectionName by remember { mutableStateOf("") }
    
    // pagination check
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
                (lastVisibleItem.index + 1 == layoutInfo.totalItemsCount) &&
                (lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
            }
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !uiState.isLoading && !uiState.endReached) {
            viewModel.loadQuotes()
        }
    }
    
    // dialogs (share / collection) - keeping basic logic/ui for now
    if (showShareDialog && selectedQuoteForShare != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Quote") },
            text = { Text("How would you like to share this quote?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareQuote(context, selectedQuoteForShare!!.content, selectedQuoteForShare!!.author)
                        showShareDialog = false
                    }
                ) {
                    Text("Share Text", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onNavigateToShare(selectedQuoteForShare!!.id)
                        showShareDialog = false
                    }
                ) {
                    Text("Share Image", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Collection Dialog
    if (showCollectionDialog && selectedQuoteForCollection != null) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Collection") },
            text = {
                Column {
                    Text("Select a collection:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(uiState.userCollections) { collection ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addQuoteToCollection(collection.id, selectedQuoteForCollection!!.id)
                                        showCollectionDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(collection.name, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Create new collection:", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("Collection Name") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName)
                            newCollectionName = ""
                        }
                    }
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCollectionDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Filter Dialog (Author Selection)
    var showFilterDialog by remember { mutableStateOf(false) }
    
    if (showFilterDialog) {
         AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter by Author") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onAuthorSelected(null)
                                    showFilterDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.selectedAuthor == null,
                                onClick = null 
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All Authors", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    items(uiState.authors) { author ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onAuthorSelected(author)
                                    showFilterDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             RadioButton(
                                selected = uiState.selectedAuthor == author,
                                onClick = null 
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(author ?: "Unknown", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Row {
                    TextButton(onClick = { 
                        viewModel.onAuthorSelected(null) 
                        showFilterDialog = false 
                    }) {
                        Text("Clear Filter", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    HomeScreenContent(
        uiState = uiState,
        listState = listState,
        onProfileClick = onProfileClick,
        onQueryChange = viewModel::onSearchQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onFavoriteClick = viewModel::toggleFavorite,
        onAddToCollectionClick = { 
            selectedQuoteForCollection = it
            showCollectionDialog = true
        },
        onShareClick = { 
            selectedQuoteForShare = it
            showShareDialog = true
        },
        onRefresh = { viewModel.loadQuotes(reset = true) },
        onFilterClick = { showFilterDialog = true },
        avatarUrl = userProfile?.avatarUrl
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onProfileClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onFavoriteClick: (Quote) -> Unit,
    onAddToCollectionClick: (Quote) -> Unit,
    onShareClick: (Quote) -> Unit,
    onRefresh: () -> Unit,
    onFilterClick: () -> Unit,
    avatarUrl: String? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Sticky Header Container matching HTML design
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                    .statusBarsPadding()
            ) {
                HomeTopBar(
                    onProfileClick = onProfileClick,
                    avatarUrl = avatarUrl
                )
                // search ui component used in the top bar (keywords: search ui, search bar)
                QuoteSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    onFilterClick = onFilterClick
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Category Chips (Non-sticky in HTML design)
            CategoryChips(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.error != null && uiState.quotes.isEmpty()) { // Only show error if no quotes loaded
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Something went wrong", color = MaterialTheme.colorScheme.error)
                        Text(text = uiState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                // pull to refresh container (keywords: refresh logic)
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Quote of the Day
                        if (uiState.dailyQuote != null) {
                            item {
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text(
                                        text = "Quote of the Day",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    QuoteCard(
                                        quote = uiState.dailyQuote,
                                        onFavoriteClick = { onFavoriteClick(uiState.dailyQuote) },
                                        onItemClick = { /* Detail view */ },
                                        onShareClick = { onShareClick(uiState.dailyQuote) },
                                        onAddToCollectionClick = { onAddToCollectionClick(uiState.dailyQuote) },
                                        style = QuoteCardStyle.Featured
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }
                        }

                        items(uiState.quotes) { quote ->
                             val style = when (quote.id.hashCode() % 3) {
                                 0 -> QuoteCardStyle.Minimal
                                 1 -> QuoteCardStyle.Typography
                                 else -> QuoteCardStyle.Featured
                             }
    
                            QuoteCard(
                                quote = quote,
                                onFavoriteClick = { onFavoriteClick(quote) },
                                onItemClick = { /* Detail view if needed */ },
                                onShareClick = { onShareClick(quote) },
                                onAddToCollectionClick = { onAddToCollectionClick(quote) },
                                style = style
                            )
                        }
                        
                        // We use PullToRefresh indicator for loading now, but if pagination happens we might need bottom loader
                        if (uiState.isLoading && uiState.quotes.isNotEmpty()) {
                             // Pull indicator handles top loading. Bottom loading for pagination could still be here.
                        }
                    }
                }
            }
        }
    }
}

private fun shareQuote(context: Context, content: String, author: String?) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "\"$content\"\n- ${author ?: "Unknown"}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
