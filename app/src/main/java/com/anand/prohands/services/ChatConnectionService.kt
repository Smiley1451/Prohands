package com.anand.prohands.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anand.prohands.R
import com.anand.prohands.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatConnectionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var heartbeatJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "chat_service_channel"
        private const val NOTIFICATION_ID = 2
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, ChatConnectionService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChatConnectionService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER_ID)
        
        if (userId != null) {
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Initialize connection
            ChatRepository.initialize(userId)
            
            // Start heartbeat
            startHeartbeat()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (true) {
                ChatRepository.sendHeartbeat()
                delay(30000) // 30 seconds
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProHands Chat")
            .setContentText("Checking for new messages...")
            .setSmallIcon(R.mipmap.ic_launcher) // Make sure this resource exists
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        ChatRepository.disconnect()
    }
}
