package com.rangelreale.devsettings.keepon

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rangelreale.devsettings.MainActivity
import com.rangelreale.devsettings.R
import com.rangelreale.devsettings.tiles.KeepScreenOnTileService

/**
 * Holds a screen wake lock for a chosen duration, with an ongoing notification
 * that counts down and offers a Stop action.
 */
class KeepScreenOnService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private val stopRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getStringExtra(EXTRA_DURATION)
            ?.let { name -> KeepScreenOnDuration.entries.firstOrNull { it.name == name } }
        if (intent?.action == ACTION_STOP || duration == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val endsAt = duration.millis?.let { System.currentTimeMillis() + it }
        val status = KeepScreenOnStatus(duration, endsAt)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(status),
            // The specialUse type exists from Android 14. Earlier versions take no type here.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        acquireWakeLock(duration.millis)
        handler.removeCallbacks(stopRunnable)
        duration.millis?.let { handler.postDelayed(stopRunnable, it) }

        KeepScreenOnState.set(status)
        refreshTile()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        releaseWakeLock()
        KeepScreenOnState.set(null)
        refreshTile()
        super.onDestroy()
    }

    // SCREEN_BRIGHT_WAKE_LOCK is the only way to hold the screen on from a service.
    // "Indefinitely" deliberately holds the lock with no timeout until the user stops it.
    @Suppress("DEPRECATION")
    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock(timeoutMillis: Long?) {
        releaseWakeLock()
        val pm = getSystemService(PowerManager::class.java)
        val lock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "DevSettings:KeepScreenOn",
        )
        lock.setReferenceCounted(false)
        // The timeout is a safety net in case the stop callback never runs.
        if (timeoutMillis != null) lock.acquire(timeoutMillis) else lock.acquire()
        wakeLock = lock
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(status: KeepScreenOnStatus): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, KeepScreenOnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen)
            .setContentTitle(getString(R.string.notification_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stop),
                    getString(R.string.stop),
                    stop,
                ).build(),
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Show the notification at once instead of after the default 10-second delay.
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (status.endsAtMillis != null) {
            // The system renders a live countdown; no per-second updates are needed.
            builder.setWhen(status.endsAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setContentText(getString(R.string.notification_indefinite))
                .setShowWhen(false)
        }
        return builder.build()
    }

    private fun refreshTile() {
        TileService.requestListeningState(
            this,
            ComponentName(this, KeepScreenOnTileService::class.java),
        )
    }

    companion object {
        private const val CHANNEL_ID = "keep_screen_on"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.rangelreale.devsettings.action.KEEP_SCREEN_ON_START"
        private const val ACTION_STOP = "com.rangelreale.devsettings.action.KEEP_SCREEN_ON_STOP"
        private const val EXTRA_DURATION = "duration"

        /**
         * Starts or restarts the service with [duration]. The countdown starts now.
         * Throws when Android refuses to start a foreground service from the caller's state.
         */
        fun start(context: Context, duration: KeepScreenOnDuration) {
            val intent = Intent(context, KeepScreenOnService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DURATION, duration.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepScreenOnService::class.java))
        }

        /** Starts [duration], or stops the service when [duration] is null. */
        fun apply(context: Context, duration: KeepScreenOnDuration?) {
            if (duration == null) stop(context) else start(context, duration)
        }
    }
}
