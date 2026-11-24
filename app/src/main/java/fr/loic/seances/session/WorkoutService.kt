package fr.loic.seances.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import fr.loic.seances.MainActivity
import fr.loic.seances.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Pont entre la séance en cours (SessionViewModel) et le service.
 * null = aucune séance en cours.
 */
object ActiveSession {
    data class Status(
        val title: String,
        val text: String,
        /** Heure murale (System.currentTimeMillis) de fin de l'étape, null si en pause. */
        val endsAtWallMs: Long?,
        val paused: Boolean,
        val accent: Int,
    )

    val status = MutableStateFlow<Status?>(null)

    /** Branché par la séance en cours, appelé par le bouton Pause de la notification. */
    @Volatile
    var onTogglePause: (() -> Unit)? = null
}

/**
 * Service au premier plan : garde l'app vivante et le processeur éveillé pendant la séance,
 * pour que le chrono et les bips continuent écran éteint. Affiche une notification
 * avec le décompte et un bouton Pause / Reprendre.
 */
class WorkoutService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Séance en cours", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Chrono de la séance"
                setShowBadge(false)
            },
        )
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SeanceDeLolo:chrono")
            .apply { acquire(4 * 60 * 60 * 1000L) } // garde-fou : 4 h max
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_PAUSE) {
            ActiveSession.onTogglePause?.invoke()
        }
        // Toujours (re)passer au premier plan : obligatoire après startForegroundService().
        val current = ActiveSession.status.value
        ServiceCompat.startForeground(
            this, NOTIF_ID,
            build(current ?: ActiveSession.Status("Sporten", "", null, false, 0)),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (!started) {
            started = true
            scope.launch {
                ActiveSession.status.collect { s ->
                    if (s == null) {
                        ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        runCatching { getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build(s)) }
                    }
                }
            }
        } else if (current == null) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun build(s: ActiveSession.Status): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val toggle = PendingIntent.getService(
            this, 1,
            Intent(this, WorkoutService::class.java).setAction(ACTION_TOGGLE_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.title)
            .setContentText(s.text)
            .setColor(s.accent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(0, if (s.paused) "Reprendre" else "Pause", toggle)
            .apply {
                if (s.endsAtWallMs != null) {
                    setWhen(s.endsAtWallMs)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                } else {
                    setShowWhen(false)
                }
            }
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "session"
        private const val NOTIF_ID = 42
        private const val ACTION_TOGGLE_PAUSE = "fr.loic.seances.TOGGLE_PAUSE"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        }
    }
}
