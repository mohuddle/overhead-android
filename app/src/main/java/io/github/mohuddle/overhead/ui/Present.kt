package io.github.mohuddle.overhead.ui

import io.github.mohuddle.overhead.domain.Aircraft
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.aircraftLine
import io.github.mohuddle.overhead.domain.globeUrl
import io.github.mohuddle.overhead.domain.nearestLine

data class RingChip(val ring: Int, val label: String, val count: Int, val selected: Boolean)

data class PlaneRow(val line: String, val insideOneMile: Boolean, val url: String)

data class ScreenState(
    val badge: String,
    val watching: Boolean,
    val statusLine: String,
    val statusAccent: Boolean,
    val placeholder: String,
    val placeLine: String?,
    val rings: List<RingChip>,
    val watchLabel: String,
    val notifyLabel: String,
    val notifyOn: Boolean,
    val clearVisible: Boolean,
    val emptyCopy: String?,
    val rows: List<PlaneRow>,
    val caption: String?,
    val placeError: String?,
    val deviceError: String?,
    val notifyError: String?,
)

data class PresentInput(
    val place: Place?,
    val rings: List<Int>,
    val notify: Boolean,
    val watch: Boolean,
    val aircraft: List<Aircraft>,
    val error: String,
    val counts: Map<String, Int>,
    val lastSuccessProvider: String,
    val timeLabel: String?,
    val placeError: String?,
    val deviceError: String?,
    val notifyDenied: Boolean,
)

private const val PLACEHOLDER = "72714, Santa Monica, or 34.05, -118.24"

fun present(input: PresentInput): ScreenState {
    val hasPlace = input.place != null
    val nearest = input.aircraft.firstOrNull()
    val statusLine = if (input.error.isNotBlank()) {
        input.error
    } else {
        nearestLine(hasPlace, nearest)
    }
    val rows = if (hasPlace) {
        input.aircraft.map { plane ->
            PlaneRow(aircraftLine(plane), plane.miles <= 1.0, globeUrl(plane.hex))
        }
    } else {
        emptyList()
    }
    val place = input.place
    val placeLine = when {
        place == null -> null
        input.lastSuccessProvider.isBlank() -> place.label
        else -> "${place.label} · ${input.lastSuccessProvider}"
    }
    val caption = if (input.lastSuccessProvider.isBlank() || input.timeLabel == null) {
        null
    } else {
        "${input.lastSuccessProvider} · ${input.timeLabel}"
    }
    return ScreenState(
        badge = if (input.watch) "Watching" else "Idle",
        watching = input.watch,
        statusLine = statusLine,
        statusAccent = input.watch && input.error.isBlank() && nearest != null,
        placeholder = PLACEHOLDER,
        placeLine = placeLine,
        rings = listOf(1, 5, 10).map { ring ->
            RingChip(ring, "$ring mi", input.counts[ring.toString()] ?: 0, ring in input.rings)
        },
        watchLabel = if (input.watch) "Stop" else "Watch",
        notifyLabel = if (input.notify) "Turn alerts off" else "Turn alerts on",
        notifyOn = input.notify,
        clearVisible = hasPlace,
        emptyCopy = when {
            rows.isNotEmpty() -> null
            !hasPlace -> "Set a location to list nearby flights."
            else -> "No aircraft in range."
        },
        rows = rows,
        caption = caption,
        placeError = input.placeError,
        deviceError = input.deviceError,
        notifyError = if (input.notifyDenied) "Notifications are off." else null,
    )
}
