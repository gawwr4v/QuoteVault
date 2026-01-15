package com.quotevault

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuoteVaultApplication : Application(), androidx.work.Configuration.Provider {

    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initial scheduling - just enqueue for a reasonable default.
        // The worker will reschedule itself after running based on user preferences.
        // Only schedule if there's no existing pending work.
        val workManager = androidx.work.WorkManager.getInstance(this)
        
        // Check if there's already pending work
        val workInfo = workManager.getWorkInfosForUniqueWork("DailyQuoteOneTime")
        workInfo.addListener({
            try {
                val infos = workInfo.get()
                val hasPendingWork = infos.any { it.state == androidx.work.WorkInfo.State.ENQUEUED || it.state == androidx.work.WorkInfo.State.RUNNING }
                if (!hasPendingWork) {
                    // No pending work, schedule initial one for 8 AM tomorrow (or use saved prefs)
                    android.util.Log.d("QuoteVaultApp", "No pending notification work, scheduling initial...")
                    scheduleInitialWork()
                } else {
                    android.util.Log.d("QuoteVaultApp", "Notification work already scheduled, skipping initial setup")
                }
            } catch (e: Exception) {
                android.util.Log.e("QuoteVaultApp", "Error checking work status", e)
                // Schedule anyway if check fails
                scheduleInitialWork()
            }
        }, java.util.concurrent.Executors.newSingleThreadExecutor())
    }
    
    private fun scheduleInitialWork() {
        // Schedule for 8 AM by default - worker will read user prefs and reschedule
        val now = java.time.LocalDateTime.now()
        var target = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (target.isBefore(now.plusMinutes(1))) {
            target = target.plusDays(1)
        }
        val delay = java.time.Duration.between(now, target).toMillis()
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.quotevault.worker.DailyQuoteWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        
        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "DailyQuoteOneTime",
            androidx.work.ExistingWorkPolicy.KEEP, // Don't replace if already exists
            workRequest
        )
        android.util.Log.d("QuoteVaultApp", "Scheduled initial notification work with delay: ${delay}ms")
    }
}
