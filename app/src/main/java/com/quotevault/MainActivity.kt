package com.quotevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.quotevault.ui.navigation.QuoteVaultNavGraph
import com.quotevault.ui.theme.QuoteVaultTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: com.quotevault.ui.screens.settings.SettingsViewModel = hiltViewModel()
            val preferences by settingsViewModel.userPreferences.collectAsState(initial = com.quotevault.domain.model.UserPreferences())
            
            val darkTheme = when (preferences.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            QuoteVaultTheme(
                darkTheme = darkTheme,
                fontScale = preferences.fontScale,
                accentColor = preferences.accentColor
            ) {
                // No outer Scaffold here; QuoteVaultNavGraph handles the top-level Scaffold
                QuoteVaultNavGraph()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    QuoteVaultTheme {
        Greeting("Android")
    }
}