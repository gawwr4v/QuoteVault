package com.quotevault.ui.screens.favorites

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quotevault.domain.model.Quote
import com.quotevault.ui.components.QuoteCard
import com.quotevault.ui.components.QuoteCardStyle
import com.quotevault.ui.screens.home.QuoteViewModel
import com.quotevault.ui.theme.QuoteVaultPrimary

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onNavigateToShare: (String) -> Unit = {},
    viewModel: CollectionDetailViewModel = hiltViewModel(),
    quoteViewModel: QuoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by quoteViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Menu and dialog states
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newName by remember(uiState.title) { mutableStateOf(uiState.title) }
    var newDescription by remember(uiState.description) { mutableStateOf(uiState.description ?: "") }
    
    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedQuotes by remember { mutableStateOf(setOf<String>()) }
    
    // Share dialog
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedQuoteForShare by remember { mutableStateOf<Quote?>(null) }
    
    // Add to collection dialog
    var showCollectionDialog by remember { mutableStateOf(false) }
    var selectedQuoteForCollection by remember { mutableStateOf<Quote?>(null) }
    
    val isFavorites = viewModel.isFavoritesCollection()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Load collections and saved quote IDs when screen opens
    LaunchedEffect(Unit) {
        quoteViewModel.loadCollections()
        quoteViewModel.loadSavedQuoteIds()
    }
    
    // Share dialog - with text and image options
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
                    Text("Share Text", color = QuoteVaultPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onNavigateToShare(selectedQuoteForShare!!.id)
                        showShareDialog = false
                    }
                ) {
                    Text("Share Image", color = QuoteVaultPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    // Add to Collection dialog
    if (showCollectionDialog && selectedQuoteForCollection != null) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Collection") },
            text = {
                Column {
                    if (homeUiState.userCollections.isEmpty()) {
                        Text("No collections yet. Create one first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Select a collection:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(homeUiState.userCollections) { collection ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            quoteViewModel.addQuoteToCollection(collection.id, selectedQuoteForCollection!!.id)
                                            showCollectionDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = QuoteVaultPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(collection.name, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCollectionDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    // Ambient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Glows
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .size(400.dp)
                .background(QuoteVaultPrimary.copy(alpha = 0.1f), CircleShape)
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .size(300.dp)
                .background(Color.Blue.copy(alpha = 0.05f), CircleShape)
                .blur(80.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Glass Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            IconButton(onClick = { 
                                isSelectionMode = false
                                selectedQuotes = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = "${selectedQuotes.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    if (selectedQuotes.isNotEmpty()) {
                                        viewModel.removeMultipleQuotes(selectedQuotes) {
                                            isSelectionMode = false
                                            selectedQuotes = emptySet()
                                        }
                                    }
                                },
                                enabled = selectedQuotes.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete Selected", 
                                    tint = if (selectedQuotes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Select quotes") },
                                        onClick = {
                                            showMenu = false
                                            isSelectionMode = true
                                        }
                                    )
                                    if (!isFavorites) {
                                        DropdownMenuItem(
                                            text = { Text("Edit list") },
                                            onClick = {
                                                showMenu = false
                                                newName = uiState.title
                                                showEditDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete list", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Filter Row
                var showSortMenu by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFavorites) "LIKED QUOTES" else "${uiState.quotes.size} QUOTES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Box {
                        Row(
                            modifier = Modifier.clickable { showSortMenu = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (uiState.sortOrder) {
                                    SortOrder.MOST_RECENT -> "Most Recent"
                                    SortOrder.OLDEST -> "Oldest"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Most Recent",
                                        color = if (uiState.sortOrder == SortOrder.MOST_RECENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.MOST_RECENT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Oldest",
                                        color = if (uiState.sortOrder == SortOrder.OLDEST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.OLDEST)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                if (uiState.isLoading && uiState.quotes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QuoteVaultPrimary)
                    }
                } else if (uiState.quotes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "No quotes yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFavorites) "Heart quotes to add them here." else "Add quotes to this collection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadQuotes() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.quotes, key = { it.id }) { quote ->
                                val isSelected = selectedQuotes.contains(quote.id)
                                
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (isSelectionMode && isSelected) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = QuoteVaultPrimary,
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                            } else Modifier
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedQuotes = if (isSelected) {
                                                        selectedQuotes - quote.id
                                                    } else {
                                                        selectedQuotes + quote.id
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    isSelectionMode = true
                                                    selectedQuotes = setOf(quote.id)
                                                }
                                            }
                                        )
                                ) {
                                    QuoteCard(
                                        quote = quote,
                                        onFavoriteClick = { 
                                            viewModel.toggleFavorite(quote) 
                                        },
                                        onItemClick = {
                                            if (isSelectionMode) {
                                                selectedQuotes = if (isSelected) {
                                                    selectedQuotes - quote.id
                                                } else {
                                                    selectedQuotes + quote.id
                                                }
                                            }
                                        },
                                        onShareClick = {
                                            selectedQuoteForShare = quote
                                            showShareDialog = true
                                        },
                                        onAddToCollectionClick = {
                                            selectedQuoteForCollection = quote
                                            showCollectionDialog = true
                                        },
                                        style = QuoteCardStyle.Minimal,
                                        isSaved = homeUiState.savedQuoteIds.contains(quote.id)
                                    )
                                    
                                    // Selection indicator overlay
                                    if (isSelectionMode && isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(QuoteVaultPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }
    
    // Edit List Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit List") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // List Name
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 30) newName = it },
                        label = { Text("List Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("${newName.length}/30", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                    
                    // Description
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { if (it.length <= 50) newDescription = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("${newDescription.length}/50", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateCollection(newName, newDescription.ifBlank { null })
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuoteVaultPrimary)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Collection") },
            text = {
                Text("Are you sure you want to delete \"${uiState.title}\"? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCollection { onBack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
