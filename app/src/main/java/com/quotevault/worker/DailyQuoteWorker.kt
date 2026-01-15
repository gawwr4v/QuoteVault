package com.quotevault.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.quotevault.R
import com.quotevault.domain.repository.QuoteRepository
import com.quotevault.domain.repository.SettingsRepository
import com.quotevault.widget.QuoteWidget
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit


@HiltWorker
class DailyQuoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val quoteRepository: QuoteRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Get User Preferences
            val preferences = settingsRepository.userPreferences.first()
            
            // 2. Fetch Quote with Categories
            val result = quoteRepository.getDailyQuote(preferences.notificationCategories)
            
            if (result.isSuccess) {
                val quote = result.getOrThrow()
                settingsRepository.updateDailyQuote(quote.id, quote.content, quote.author)
                showNotification(quote.content, quote.author)
                
                // Trigger Widget Update
                try {
                    QuoteWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    android.util.Log.e("DailyQuoteWorker", "Widget update failed", e)
                }

            } else {
                // Fetch failed, fallback
                showNotification("Time for your daily inspiration!", "QuoteVault")
            }
            
            // 3. Reschedule Next Work (Chain)
            scheduleNextWork(preferences)

            Result.success()
        } catch (e: Exception) {
             android.util.Log.e("DailyQuoteWorker", "Worker failed", e)
             showNotification("Time for your daily inspiration!", "QuoteVault")
             // Even if failed, try to reschedule so we don't stop forever
             // wrapping in try-catch to avoid crash loop if scheduling fails
             try {
                val preferences = settingsRepository.userPreferences.first()
                scheduleNextWork(preferences)
             } catch (inner: Exception) {
                 android.util.Log.e("DailyQuoteWorker", "Rescheduling failed", inner)
             }
             Result.success()
        }
    }

    private fun scheduleNextWork(preferences: com.quotevault.domain.model.UserPreferences) {
        val workManager = androidx.work.WorkManager.getInstance(applicationContext)
        val requestBuilder = androidx.work.OneTimeWorkRequestBuilder<DailyQuoteWorker>()
        
        val delay: Long = if (preferences.notificationMode == "daily") {
            // Parse time HH:mm
            val parts = preferences.notificationTime.split(":")
            val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            
            val now = java.time.LocalDateTime.now()
            var target = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0)
            
            // If target is before now, add 1 day
            // But be careful: if this worker is running AT the target time (approx), we want to schedule for TOMORROW.
            // If we are running slightly *after* target time due to delay, we still want tomorrow.
            // A simple rule: if target < now + 1 minute, schedule for next day. 
            // Or just: strict "future" 
            if (target.isBefore(now.plusMinutes(1))) { // Buffer of 1 min to avoid immediate loop
                target = target.plusDays(1)
            }
            
            java.time.Duration.between(now, target).toMillis()
        } else {
            // Frequency: Every X hours
            val intervalHours = preferences.notificationInterval
            java.util.concurrent.TimeUnit.HOURS.toMillis(intervalHours.toLong())
        }
        
        requestBuilder.setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
        
        workManager.enqueueUniqueWork(
            "DailyQuoteOneTime", // Unique name for the chain
            androidx.work.ExistingWorkPolicy.REPLACE, // Replace old pending ones
            requestBuilder.build()
        )
        android.util.Log.d("DailyQuoteWorker", "Scheduled next work with delay: ${delay}ms")
    }

    private fun showNotification(content: String, author: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_quote_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Quote",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this exists, or android.R.drawable.ic_dialog_info
            .setContentTitle("Quote of the Day")
            .setContentText("\"$content\" - ${author ?: "Unknown"}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"$content\" - ${author ?: "Unknown"}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
