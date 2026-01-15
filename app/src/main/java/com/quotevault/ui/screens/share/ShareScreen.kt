package com.quotevault.ui.screens.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer // Moved to top
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quotevault.ui.components.ArtisticTemplate
import com.quotevault.ui.components.BoldTemplate
import com.quotevault.ui.components.MinimalTemplate
import com.quotevault.ui.theme.QuoteVaultPrimary

enum class ShareTemplate(val displayName: String) {
    MINIMAL("Minimal"),
    BOLD("Bold"),
    ARTISTIC("Artistic")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    quoteId: String,
    onBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(ShareTemplate.MINIMAL) }
    
    // Picture recording
    val picture = remember { Picture() }
    
    // Pending bitmap for permission callback
    var pendingSaveBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // SharedPreferences to track if user has approved storage access
    val prefs = remember { context.getSharedPreferences("quotevault_prefs", android.content.Context.MODE_PRIVATE) }
    val storageApprovedKey = "storage_access_approved"
    
    // Permission launcher for storage (needed for Android 9 and below)
    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Mark as approved
            prefs.edit().putBoolean(storageApprovedKey, true).apply()
            pendingSaveBitmap?.let { bitmap ->
                viewModel.saveToGallery(bitmap)
            }
        } else {
            android.widget.Toast.makeText(context, "Storage permission required to save images", android.widget.Toast.LENGTH_SHORT).show()
        }
        pendingSaveBitmap = null
    }
    
    LaunchedEffect(quoteId) {
        viewModel.loadQuote(quoteId)
    }

    LaunchedEffect(uiState.shareIntent) {
        uiState.shareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "Share Quote"))
            viewModel.clearShareIntent()
        }
    }
    
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }
    
    // Storage access confirmation dialog state
    var showStorageDialog by remember { mutableStateOf(false) }
    
    // Helper to save with permission check and dialog
    fun saveWithPermission(bitmap: android.graphics.Bitmap) {
        val hasApprovedBefore = prefs.getBoolean(storageApprovedKey, false)
        
        if (hasApprovedBefore) {
            // User already approved before, save directly
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                viewModel.saveToGallery(bitmap)
            } else {
                // Still check if system permission is granted (user might have revoked)
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.saveToGallery(bitmap)
                } else {
                    // Permission revoked, need to ask again
                    pendingSaveBitmap = bitmap
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else {
            // First time - show appropriate dialog
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+: Show in-app dialog (no system permission needed)
                pendingSaveBitmap = bitmap
                showStorageDialog = true
            } else {
                // Android 9 and below: Show system permission dialog
                pendingSaveBitmap = bitmap
                storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    // Storage Access Confirmation Dialog
    if (showStorageDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showStorageDialog = false
                pendingSaveBitmap = null
            },
            icon = {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Save to Gallery") },
            text = { 
                Text("QuoteVault would like to save this quote image to your device's gallery in the \"Quotes\" folder.\n\nAllow storage access?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStorageDialog = false
                        // Mark as approved in SharedPreferences
                        prefs.edit().putBoolean(storageApprovedKey, true).apply()
                        pendingSaveBitmap?.let { bitmap ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                viewModel.saveToGallery(bitmap)
                            } else {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.saveToGallery(bitmap)
                                } else {
                                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    return@Button // Don't clear bitmap yet, wait for permission result
                                }
                            }
                            pendingSaveBitmap = null
                        }
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showStorageDialog = false
                        pendingSaveBitmap = null
                    }
                ) {
                    Text("Deny")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Share Quote", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading || uiState.quote == null) {
                if (uiState.error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            if (uiState.quote != null) {
                val quote = uiState.quote!!
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Canvas Preview Frame
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp) // Fixed square ratio for simplicity
                            .drawWithCache {
                                val width = this.size.width.toInt()
                                val height = this.size.height.toInt()
                                onDrawWithContent {
                                    val pictureCanvas = picture.beginRecording(width, height)
                                    draw(this, this.layoutDirection, Canvas(pictureCanvas), this.size) {
                                        this@onDrawWithContent.drawContent()
                                    }
                                    picture.endRecording()
                                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
                                }
                            }
                    ) {
                         // Render Selected Template
                        when (selectedTemplate) {
                            ShareTemplate.MINIMAL -> MinimalTemplate(quote)
                            ShareTemplate.BOLD -> BoldTemplate(quote)
                            ShareTemplate.ARTISTIC -> ArtisticTemplate(quote)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Template Selector
                Text(
                    text = "CHOOSE STYLE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(ShareTemplate.values()) { template ->
                        TemplateSelectorItem(
                            name = template.displayName,
                            isSelected = selectedTemplate == template,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShareActionButton(
                        icon = Icons.Filled.ContentCopy,
                        label = "Copy",
                        onClick = { 
                            uiState.quote?.let { viewModel.copyToClipboard(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                             val bitmap = createBitmapFromPicture(picture)
                             viewModel.shareImage(bitmap)
                        },
                        enabled = uiState.quote != null,
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Image", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    ShareActionButton(
                        icon = Icons.Filled.Download,
                        label = "Save",
                        onClick = { 
                            val bitmap = createBitmapFromPicture(picture)
                            saveWithPermission(bitmap)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (uiState.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error loading quote for share", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TemplateSelectorItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShareActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Utility to add shadow easily
fun Modifier.shadow(elevation: androidx.compose.ui.unit.Dp, shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape) = 
    this.graphicsLayer(shadowElevation = elevation.value, shape = shape, clip = true)
    
fun createBitmapFromPicture(picture: Picture): Bitmap {
    if (picture.width <= 0 || picture.height <= 0) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE) // Ensure background isn't transparent if not drawn
    picture.draw(canvas)
    return bitmap
}
