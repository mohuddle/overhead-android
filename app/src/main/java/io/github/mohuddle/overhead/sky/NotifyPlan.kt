package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.data.OverheadConfig
import io.github.mohuddle.overhead.domain.AlertEvent
import io.github.mohuddle.overhead.domain.globeUrl

data class ChannelSpec(
    val id: String,
    val importance: String,
    val silent: Boolean,
    val ongoing: Boolean,
)

fun channels(): List<ChannelSpec> = listOf(
    ChannelSpec("overhead-watch", "low", silent = true, ongoing = true),
    ChannelSpec("overhead-1", "high", silent = false, ongoing = false),
    ChannelSpec("overhead-5", "default", silent = false, ongoing = false),
    ChannelSpec("overhead-10", "low", silent = false, ongoing = false),
)

sealed class AlertOpen {
    data class Globe(val url: String) : AlertOpen()
    data object App : AlertOpen()
}

fun alertOpen(event: AlertEvent): AlertOpen {
    if (event.extra > 0) return AlertOpen.App
    return AlertOpen.Globe(globeUrl(event.hex))
}

const val WATCH_TITLE = "Overhead is watching"
const val STOP_WATCH = "io.github.mohuddle.overhead.action.STOP_WATCH"

fun afterWatchDismissed(config: OverheadConfig): OverheadConfig = config.copy(watch = false)
