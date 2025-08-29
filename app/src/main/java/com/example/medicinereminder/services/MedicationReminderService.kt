package com.example.medicinereminder.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.R
import com.example.medicinereminder.data.DatabaseHelper
import com.example.medicinereminder.ui.HomeActivity
import java.util.Timer
import java.util.TimerTask
import android.content.pm.ServiceInfo
import android.util.Log

/**
 * Servicio en segundo plano que monitorea y gestiona recordatorios de medicamentos
 * Mantiene una notificación persistente mientras esté activo
 */
class MedicationReminderService : Service() {

    private lateinit var db: DatabaseHelper
    private lateinit var notificationManager: NotificationManager
    private lateinit var timer: Timer
    private var userId: Long = 0

    companion object {
        const val CHANNEL_ID = "MedicationReminderServiceChannel"
        const val NOTIFICATION_ID = 12345
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_USER_ID = "user_id"

        fun startService(context: Context, userId: Long) {
            val intent = Intent(context, MedicationReminderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MedicationReminderService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = DatabaseHelper(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getLongExtra(EXTRA_USER_ID, 0)
                startForegroundService()
                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Recordatorios",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio en segundo plano para recordatorios de medicamentos"
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                // Iniciar el servicio en primer plano sin usar tipo HEALTH
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            } catch (e: Exception) {
                Log.e("MedicationService", "Error al iniciar foreground service: ${e.message}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            // Para versiones anteriores a Android 14
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recordatorios de Medicamentos")
            .setContentText("Monitoreando tus medicamentos...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkUpcomingMedications()
                updateNotification()
            }
        }, 0, 60000) // Verificar cada minuto
    }

    private fun stopMonitoring() {
        timer?.cancel()
    }

    private fun checkUpcomingMedications() {
        val medications = db.getMedicationsForUser(userId)
        val currentTime = System.currentTimeMillis()

        medications.forEach { medication ->
            // Aquí se puede implementar lógica para verificar horarios de medicamentos
        }
    }

    private fun updateNotification() {
        val adherenceRate = db.getAdherenceRate(userId)
        val medications = db.getMedicationsForUser(userId)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recordatorios Activos")
            .setContentText("${medications.size} medicamentos - Adherencia: ${"%.1f".format(adherenceRate)}%")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}