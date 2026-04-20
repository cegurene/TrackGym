package com.example.gimnasio.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gimnasio.MainActivity
import com.example.gimnasio.R

object EntrenamientoNotificationManager {

    private const val CHANNEL_ID = "entrenamiento_activo"
    private const val CHANNEL_NAME = "Entrenamiento activo"
    private const val NOTIFICATION_ID = 1001

    @SuppressLint("MissingPermission")
    fun mostrarEntrenamientoActivo(context: Context, nombreEntrenamiento: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        crearCanalSiHaceFalta(context)

        val abrirAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            abrirAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = nombreEntrenamiento?.takeIf { it.isNotBlank() } ?: "Entrenamiento en curso"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText("Tienes un entrenamiento en curso")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancelarEntrenamientoActivo(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun crearCanalSiHaceFalta(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existente = manager.getNotificationChannel(CHANNEL_ID)
        if (existente != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifica cuando hay un entrenamiento activo"
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }
}


