package com.quotevault.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.quotevault.MainActivity
import com.quotevault.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class QuoteWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val settingsRepository = entryPoint.settingsRepository()

        val prefs = settingsRepository.userPreferences.first()
        val content = prefs.dailyQuoteContent ?: "Open App to fetch Quote"
        val author = prefs.dailyQuoteAuthor ?: "Unknown"
        val quoteId = prefs.dailyQuoteId

        // Create intent to open app (with optional deep link to quote)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (quoteId != null) {
                putExtra("quote_id", quoteId)
            }
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(16.dp)
                        .clickable(actionStartActivity(intent)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Decorative quote marks
                        Text(
                            text = "❝",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        Text(
                            text = content,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 4
                        )
                        
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        
                        Text(
                            text = "— $author",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}
