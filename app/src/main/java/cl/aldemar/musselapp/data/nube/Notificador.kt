package cl.aldemar.musselapp.data.nube

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.aldemar.musselapp.MainActivity
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra

/** Avisa al operario, con una notificación del teléfono, que el supervisor revisó su muestra. */
class Notificador(context: Context) {

    private val app = context.applicationContext

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Revisión de muestras", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Avisa cuando el supervisor valida o corrige una muestra" }
            app.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
        }
    }

    fun avisarRevision(muestra: Muestra, estado: EstadoMuestra, comentario: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val titulo = if (estado == EstadoMuestra.VALIDADO) "Muestra validada" else "Muestra corregida"
            val detalle = buildString {
                append("${muestra.nombre} · Línea ${muestra.codigoLinea}: el supervisor ya la revisó.")
                if (!comentario.isNullOrBlank()) append(" \"$comentario\"")
            }
            val abrir = PendingIntent.getActivity(
                app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(app, CANAL)
                .setSmallIcon(cl.aldemar.musselapp.R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(detalle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .build()
            NotificationManagerCompat.from(app).notify(muestra.id.toInt(), n)
        } catch (_: Exception) {
            // La notificación es un extra: nunca debe romper la sincronización
        }
    }

    private companion object {
        const val CANAL = "revision_muestras"
    }
}
