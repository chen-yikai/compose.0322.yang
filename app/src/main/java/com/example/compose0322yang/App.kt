package com.example.compose0322yang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createChannel()
        updateWidget()
        startForegroundService(Intent(this, PlaybackService::class.java))
    }

    private fun updateWidget() {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        appScope.launch {
            Widget().updateAll(applicationContext)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            "media_channel",
            "Media Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }
}