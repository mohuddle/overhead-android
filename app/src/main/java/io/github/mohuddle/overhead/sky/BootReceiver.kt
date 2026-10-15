package io.github.mohuddle.overhead.sky

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.mohuddle.overhead.OverheadApp
import io.github.mohuddle.overhead.domain.shouldRunService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = (appContext as OverheadApp).store.config.first()
                if (shouldRunService(config.watch, config.place)) {
                    ContextCompat.startForegroundService(
                        appContext,
                        Intent(appContext, OverheadService::class.java),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
