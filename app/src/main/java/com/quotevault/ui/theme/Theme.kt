package com.quotevault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = QuoteVaultPrimary,
    onPrimary = BackgroundDark, // Text on primary button should be dark
    secondary = QuoteVaultPrimaryDark,
    tertiary = QuoteVaultPrimary, // Using primary as accent

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = QuoteVaultPrimary,
    onPrimary = Color.Black, // Dark text on light accent
    
    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    
    surfaceVariant = Color(0xFFEEEEEE), // Slightly darker surface for light mode
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun QuoteVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    fontScale: Float = 1.0f,
    accentColor: String = "purple",
    content: @Composable () -> Unit
) {
    val primaryColor = when (accentColor) {
        "blue" -> QuoteVaultBlue
        "green" -> QuoteVaultGreen
        "yellow" -> QuoteVaultYellow
        "orange" -> QuoteVaultOrange
        "red" -> QuoteVaultRed
        "pink" -> QuoteVaultPink
        "teal" -> QuoteVaultTeal
        else -> QuoteVaultPrimary // purple
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.copy(primary = primaryColor, tertiary = primaryColor)
        else -> LightColorScheme.copy(primary = primaryColor)
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = androidx.compose.runtime.remember(currentDensity, fontScale) {
        androidx.compose.ui.unit.Density(currentDensity.density, fontScale = fontScale)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}