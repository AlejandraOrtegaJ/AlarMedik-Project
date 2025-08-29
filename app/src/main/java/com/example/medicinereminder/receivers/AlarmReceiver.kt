package com.example.medicinereminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.ui.HomeActivity

// Receptor de alarmas para notificaciones de medicamentos
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("medName") ?: "Medicamento"
        val medId = intent.getLongExtra("medId", 0)

        // Crear canal de notificación (requerido en Android 8+)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meds_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Medicamentos", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }

        // Intent para abrir la app al hacer clic en la notificación
        val launch = Intent(context, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)

        // Construir la notificación
        val n = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Hora de tu medicación")
            .setContentText(medName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        // Mostrar notificación
        nm.notify(medId.toInt(), n)
    }
}