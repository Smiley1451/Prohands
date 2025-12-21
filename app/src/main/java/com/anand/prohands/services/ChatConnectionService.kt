package com.anand.prohands.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ChatService", "Failed to start service", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, ChatConnectionService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e("ChatService", "Failed to stop service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e("ChatService", "Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // CRITICAL: We MUST start foreground immediately to satisfy Android 8+ requirements
            // regardless of whether we have a userId or not.
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val userId = intent?.getStringExtra(EXTRA_USER_ID)
            
            if (!userId.isNullOrEmpty()) {
                Log.d("ChatService", "Starting chat connection for user: $userId")
                
                // Initialize connection
                ChatRepository.initialize(userId)
                
                // Start heartbeat
                startHeartbeat()
            } else {
                Log.w("ChatService", "No user ID provided, stopping service")
                stopSelf()
            }

        } catch (e: Exception) {
            Log.e("ChatService", "Error in onStartCommand", e)
            // Even if we crash, we try to stop gracefully
            stopSelf()
        }

        return START_STICKY
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (true) {
                try {
                    ChatRepository.sendHeartbeat()
                } catch (e: Exception) {
                    Log.e("ChatService", "Error sending heartbeat", e)
                }
                delay(30000) // 30 seconds
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProHands Chat")
            .setContentText("Checking for new messages...")
            .setSmallIcon(R.mipmap.ic_launcher) 
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the chat connection alive"
                setShowBadge(false)
            }
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
        try {
            ChatRepository.disconnect()
        } catch (e: Exception) {
            Log.e("ChatService", "Error disconnecting repository", e)
        }
    }
}
