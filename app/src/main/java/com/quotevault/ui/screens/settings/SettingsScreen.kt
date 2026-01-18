package com.quotevault.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.quotevault.domain.model.UserPreferences
import com.quotevault.ui.theme.SurfaceHighlight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAccentColors: () -> Unit,
    onNavigateToTextSize: () -> Unit = {},
    onNavigateToAccountManagement: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // profile card (keywords: pfp)
            ProfileCard(
                userProfile = userProfile,
                email = userEmail,
                name = userName
            )

            // appearance section (keywords: theme, accent color)
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = mapThemeToLabel(preferences.theme),
                    onClick = { 
                         val newTheme = if (preferences.theme == "dark") "light" else "dark"
                         viewModel.updateTheme(newTheme)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                

                
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Accent Color",
                    subtitle = preferences.accentColor.replaceFirstChar { it.uppercase() },
                    onClick = { onNavigateToAccentColors() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsItem(
                    icon = Icons.Default.TextFormat,
                    title = "Text Size",
                    subtitle = "${(preferences.fontScale * 100).toInt()}%",
                    onClick = { onNavigateToTextSize() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // general section
            SettingsSection(title = "General") {
                // Notification Mode
                // Notification Settings
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage schedule and alerts",
                    onClick = { showModeDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    subtitle = "Manage your account",
                    onClick = { onNavigateToAccountManagement() }
                )
            }

            // logout button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            // version
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // consolidated notification settings dialog
    if (showModeDialog) {
        NotificationSettingsDialog(
            preferences = preferences,
            context = LocalContext.current,
            availableCategories = availableCategories,
            onDismiss = { showModeDialog = false },
            onModeSelected = viewModel::updateNotificationMode,
            onIntervalSelected = viewModel::updateNotificationInterval,
            onTimeSelected = viewModel::updateNotificationTime,
            onCategoriesUpdated = viewModel::updateNotificationCategories
        )
    }

    // logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = {
                Text("Are you sure you want to log out?")
            },
            confirmButton = {
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch {
                            showLogoutDialog = false
                            viewModel.logoutAndWait()
                            onLogoutComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    preferences: UserPreferences,
    context: android.content.Context,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onCategoriesUpdated: (List<String>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Schedule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Selection
                Text("Frequency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = preferences.notificationMode == "daily",
                        onClick = { onModeSelected("daily") }
                    )
                    Text("Daily at Specific Time", modifier = Modifier.clickable { onModeSelected("daily") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = preferences.notificationMode == "frequency",
                        onClick = { onModeSelected("frequency") }
                    )
                    Text("Every X Hours", modifier = Modifier.clickable { onModeSelected("frequency") })
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Conditional Options
                if (preferences.notificationMode == "daily") {
                    Text("Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = {
                            val timeParts = preferences.notificationTime.split(":")
                            val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                            val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                             TimePickerDialog(context, { _, hour, minute -> onTimeSelected(hour, minute) }, h, m, true).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Time: ${preferences.notificationTime}")
                    }
                } else {
                    Text("Interval", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val intervals = listOf(2, 4, 6, 8, 12, 24)
                    OutlinedButton(
                        onClick = {
                            val currentIndex = intervals.indexOf(preferences.notificationInterval)
                            val nextIndex = (currentIndex + 1) % intervals.size
                            onIntervalSelected(intervals[nextIndex])
                        },
                         modifier = Modifier.fillMaxWidth()
                    ) {
                         Text("Every ${preferences.notificationInterval} hours (Tap to change)")
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Content Filter
                Text("Content Categories", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Select categories you want to see. Leave empty for all.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { category ->
                         val isSelected = preferences.notificationCategories.contains(category)
                         FilterChip(
                             selected = isSelected,
                             onClick = {
                                 val current = preferences.notificationCategories.toMutableList()
                                 if (isSelected) {
                                     current.remove(category)
                                 } else {
                                     current.add(category)
                                 }
                                 onCategoriesUpdated(current)
                             },
                             label = { Text(category) },
                             leadingIcon = if (isSelected) {
                                 { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                             } else null
                         )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Test Button
                 Button(
                    onClick = {
                         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                             if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                val request = androidx.work.OneTimeWorkRequestBuilder<com.quotevault.worker.DailyQuoteWorker>().build()
                                androidx.work.WorkManager.getInstance(context).enqueue(request)
                            } else {
                                // Simple toast or ignore in dialog test for now, usually permission requested elsewhere
                            }
                        } else {
                            val request = androidx.work.OneTimeWorkRequestBuilder<com.quotevault.worker.DailyQuoteWorker>().build()
                            androidx.work.WorkManager.getInstance(context).enqueue(request)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("Test Notification Now")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ProfileCard(
    userProfile: com.quotevault.domain.model.UserProfile?,
    email: String?,
    name: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.avatarUrl != null) {
                    AsyncImage(
                        model = userProfile.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!name.isNullOrBlank()) name else "User",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (!email.isNullOrBlank()) email else "Not Signed In",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (email != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "FREE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), // Subtler background for section
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

fun mapThemeToLabel(theme: String): String {
    return when (theme) {
        "dark" -> "Dark Mode"
        "light" -> "Light Mode"
        else -> "System Default"
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    com.quotevault.ui.theme.QuoteVaultTheme {
        SettingsScreen(onBack = {}, onNavigateToAccentColors = {})
    }
}
