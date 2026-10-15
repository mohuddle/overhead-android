package io.github.mohuddle.overhead.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mohuddle.overhead.OverheadApp
import io.github.mohuddle.overhead.domain.SetQuery
import io.github.mohuddle.overhead.domain.USER_AGENT
import io.github.mohuddle.overhead.domain.classifySet
import io.github.mohuddle.overhead.domain.noMatchMessage
import io.github.mohuddle.overhead.domain.placeFromNominatim
import io.github.mohuddle.overhead.domain.shouldRunService
import io.github.mohuddle.overhead.domain.summarizeCounts
import io.github.mohuddle.overhead.sky.OverheadService
import io.github.mohuddle.overhead.sky.SkyHolder
import io.github.mohuddle.overhead.sky.emptySky
import io.github.mohuddle.overhead.sky.httpGet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverheadViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as OverheadApp).store
    private val placeError = MutableStateFlow<String?>(null)
    private val deviceError = MutableStateFlow<String?>(null)
    private val notifyDenied = MutableStateFlow(false)

    val screen: StateFlow<ScreenState> = combine(
        store.config,
        SkyHolder.sky,
        placeError,
        deviceError,
        notifyDenied,
    ) { config, sky, placeMessage, deviceMessage, denied ->
        present(
            PresentInput(
                place = config.place,
                rings = config.rings,
                notify = config.notify,
                watch = config.watch,
                aircraft = sky.aircraft,
                error = sky.error,
                counts = summarizeCounts(sky.aircraft),
                lastSuccessProvider = sky.lastSuccessProvider,
                timeLabel = timeLabel(sky.lastSuccessEpochMs),
                placeError = placeMessage,
                deviceError = deviceMessage,
                notifyDenied = denied,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), idleScreen())

    fun set(text: String) {
        viewModelScope.launch {
            when (val query = classifySet(text)) {
                is SetQuery.Manual -> {
                    placeError.value = null
                    store.setPlace(query.place)
                    syncService()
                }
                is SetQuery.Rejected -> {
                    placeError.value = query.message
                }
                is SetQuery.Remote -> {
                    try {
                        val body = withContext(Dispatchers.IO) { httpGet(query.url, USER_AGENT) }
                        val place = placeFromNominatim(text.trim(), body)
                        placeError.value = null
                        store.setPlace(place)
                        syncService()
                    } catch (error: Exception) {
                        placeError.value = error.message?.takeIf { it.isNotBlank() }
                            ?: noMatchMessage(text.trim())
                    }
                }
            }
        }
    }

    fun toggleWatch(ask: () -> Unit) {
        viewModelScope.launch {
            val config = store.config.first()
            val next = !config.watch
            if (next) ask()
            store.setWatch(next)
            syncService()
        }
    }

    fun toggleNotify(ask: () -> Unit) {
        viewModelScope.launch {
            val next = !store.config.first().notify
            if (next) ask()
            store.setNotify(next)
        }
    }

    fun ring(ring: Int) {
        viewModelScope.launch {
            val current = store.config.first().rings
            val next = if (ring in current) current.filter { it != ring } else current + ring
            store.setRings(next)
        }
    }

    fun clear() {
        viewModelScope.launch {
            store.clearPlace()
            SkyHolder.publish(emptySky())
            placeError.value = null
            deviceError.value = null
            stopWatching()
        }
    }

    fun setNotifyDenied(denied: Boolean) {
        notifyDenied.value = denied
    }

    private suspend fun syncService() {
        val config = store.config.first()
        val context = getApplication<Application>()
        val intent = Intent(context, OverheadService::class.java)
        if (shouldRunService(config.watch, config.place)) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    private fun stopWatching() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, OverheadService::class.java))
    }
}

private fun idleScreen(): ScreenState = present(
    PresentInput(
        place = null,
        rings = listOf(1, 5, 10),
        notify = true,
        watch = false,
        aircraft = emptyList(),
        error = "",
        counts = emptyMap(),
        lastSuccessProvider = "",
        timeLabel = null,
        placeError = null,
        deviceError = null,
        notifyDenied = false,
    ),
)

private fun timeLabel(epochMs: Long): String? {
    if (epochMs == 0L) return null
    val local = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalTime()
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)
}
