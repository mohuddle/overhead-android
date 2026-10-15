package io.github.mohuddle.overhead.sky

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.mohuddle.overhead.MainActivity
import io.github.mohuddle.overhead.OverheadApp
import io.github.mohuddle.overhead.Palette
import io.github.mohuddle.overhead.R
import io.github.mohuddle.overhead.domain.AlertEvent
import io.github.mohuddle.overhead.domain.POLL_INTERVAL_MS
import io.github.mohuddle.overhead.domain.bodyFor
import io.github.mohuddle.overhead.domain.fetchWith
import io.github.mohuddle.overhead.domain.shouldKeepPoll
import io.github.mohuddle.overhead.domain.shouldRunService
import io.github.mohuddle.overhead.domain.titleFor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val WATCH_NOTIFICATION_ID = 1
private const val ALERT_NOTIFICATION_ID = 2

private enum class Tick { Continue, Retry, Stop }

class OverheadService : LifecycleService() {
    private val loopStarted = AtomicBoolean(false)
    private val timingOut = AtomicBoolean(false)
    private var lastToastAt = 0L

    override fun onTimeout(startId: Int, fgsType: Int) {
        endBecauseTimedOut()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onTimeout(startId: Int) {
        endBecauseTimedOut()
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(
            WATCH_NOTIFICATION_ID,
            watchNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == STOP_WATCH) {
            lifecycleScope.launch {
                val store = (application as OverheadApp).store
                store.setWatch(afterWatchDismissed(store.config.first()).watch)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (loopStarted.compareAndSet(false, true)) {
            lifecycleScope.launch(Dispatchers.IO) { loop() }
        }
        return START_STICKY
    }

    private fun endBecauseTimedOut() {
        if (!timingOut.compareAndSet(false, true)) return
        SkyHolder.publish(skyAfterTimeout(SkyHolder.sky.value))
        try {
            runBlocking(Dispatchers.IO) {
                val store = (application as OverheadApp).store
                store.setWatch(afterWatchDismissed(store.config.first()).watch)
            }
        } catch (_: Exception) {
            // The notice still stops. A later launch reads whatever was saved.
        }
        stopSelf()
    }

    private suspend fun loop() {
        while (currentCoroutineContext().isActive) {
            val step = try {
                tick()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Tick.Continue
            }
            when (step) {
                Tick.Stop -> break
                Tick.Retry -> Unit
                Tick.Continue -> delay(POLL_INTERVAL_MS)
            }
        }
        stopSelf()
    }

    private suspend fun tick(): Tick {
        if (timingOut.get()) return Tick.Stop
        val store = (application as OverheadApp).store
        val config = store.config.first()
        val place = config.place
        if (!shouldRunService(config.watch, place) || place == null) return Tick.Stop
        val now = System.currentTimeMillis()
        val since = if (lastToastAt == 0L) 8.0 else (now - lastToastAt) / 1000.0
        val result = poll(
            place = place,
            rings = config.rings,
            notify = config.notify,
            alerted = config.alerted,
            sky = SkyHolder.sky.value,
            secondsSinceLast = since,
            nowEpochMs = now,
            fetch = { here, rings -> fetchWith(::httpGet, here.lat, here.lon, rings) },
        )
        if (timingOut.get()) return Tick.Stop
        val latest = store.config.first()
        if (!shouldKeepPoll(place, latest.place, latest.watch)) {
            return if (shouldRunService(latest.watch, latest.place)) Tick.Retry else Tick.Stop
        }
        SkyHolder.publish(result.sky)
        val allowed = notificationsAllowed()
        val toast = result.toast
        if (toast != null && allowed) {
            postToast(toast)
            lastToastAt = now
        }
        if (shouldPersistAlerted(toast, allowed) && result.alerted != latest.alerted) {
            store.setAlerted(result.alerted)
        }
        return Tick.Continue
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        for (spec in channels()) {
            val channel = NotificationChannel(spec.id, channelName(spec.id), importance(spec.importance))
            if (spec.silent) {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun watchNotification(): Notification {
        return NotificationCompat.Builder(this, "overhead-watch")
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setColor(Palette.Sky.toInt())
            .setContentTitle(WATCH_TITLE)
            .setContentIntent(activityIntent(Intent(this, MainActivity::class.java), 0))
            .setDeleteIntent(dismissIntent())
            .setSilent(true)
            .build()
    }

    private fun postToast(event: AlertEvent) {
        val open = when (val target = alertOpen(event)) {
            is AlertOpen.Globe -> Intent(Intent.ACTION_VIEW, Uri.parse(target.url))
            AlertOpen.App -> Intent(this, MainActivity::class.java)
        }
        val notification = NotificationCompat.Builder(this, "overhead-${event.ring}")
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setColor(Palette.Sky.toInt())
            .setContentTitle(titleFor(event))
            .setContentText(bodyFor(event))
            .setContentIntent(activityIntent(open, event.hex.hashCode()))
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun activityIntent(intent: Intent, request: Int): PendingIntent {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            request,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun dismissIntent(): PendingIntent {
        val intent = Intent(this, OverheadService::class.java).setAction(STOP_WATCH)
        return PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

private fun importance(name: String): Int = when (name) {
    "high" -> NotificationManager.IMPORTANCE_HIGH
    "default" -> NotificationManager.IMPORTANCE_DEFAULT
    "low" -> NotificationManager.IMPORTANCE_LOW
    else -> NotificationManager.IMPORTANCE_DEFAULT
}

private fun channelName(id: String): String = when (id) {
    "overhead-watch" -> "Watching"
    "overhead-1" -> "Within 1 mile"
    "overhead-5" -> "Within 5 miles"
    "overhead-10" -> "Within 10 miles"
    else -> id
}
