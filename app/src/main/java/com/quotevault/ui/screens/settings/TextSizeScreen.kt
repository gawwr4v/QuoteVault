package com.quotevault.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quotevault.ui.theme.QuoteVaultPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    var currentScale by remember(userPreferences.fontScale) { mutableStateOf(userPreferences.fontScale) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Text Size", fontWeight = FontWeight.SemiBold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"The only way to do great work is to love what you do.\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (16 * currentScale).sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "— Steve Jobs",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (14 * currentScale).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            // current size indicator
            Text(
                text = "${(currentScale * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = QuoteVaultPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            // size slider
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("A", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("A", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Slider(
                    value = currentScale,
                    onValueChange = { currentScale = it },
                    onValueChangeFinished = {
                        // update font scale (keywords: textsize, changetextsize)
                        viewModel.updateFontScale(currentScale)
                    },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = QuoteVaultPrimary,
                        activeTrackColor = QuoteVaultPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Large", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // reset button (reset to default text size)
            OutlinedButton(
                onClick = {
                    currentScale = 1.0f
                    viewModel.updateFontScale(1.0f)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset to Default")
            }
        }
    }
}
