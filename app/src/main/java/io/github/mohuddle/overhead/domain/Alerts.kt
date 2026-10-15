package io.github.mohuddle.overhead.domain

import java.util.Locale

private const val MIN_ALERT_ALT_FT = 500
private const val NOTIFY_MIN_INTERVAL = 8.0

data class AlertEvent(
    val hex: String,
    val callsign: String,
    val type: String,
    val miles: Double,
    val ring: Int,
    val altFt: Int?,
    val ground: Boolean,
    val gsKt: Double?,
    val extra: Int = 0,
)

fun alertable(item: Aircraft, minAltFt: Int = MIN_ALERT_ALT_FT): Boolean {
    if (item.ground) return false
    val alt = item.altFt
    if (alt != null && alt < minAltFt) return false
    return true
}

fun newAlerts(
    previous: Map<String, List<Int>>,
    aircraft: List<Aircraft>,
    rings: List<Int>,
): Pair<List<AlertEvent>, Map<String, List<Int>>> {
    val current = linkedMapOf<String, List<Int>>()
    val events = mutableListOf<AlertEvent>()
    for (item in aircraft) {
        if (item.hex.isEmpty()) continue
        val inside = ringsInside(item.miles, rings)
        if (inside.isEmpty()) continue
        current[item.hex] = inside
        val already = previous[item.hex].orEmpty().toSet()
        val entered = inside.filter { it !in already }
        if (entered.isEmpty()) continue
        events += AlertEvent(
            hex = item.hex,
            callsign = item.callsign,
            type = item.type,
            miles = item.miles,
            ring = entered.min(),
            altFt = item.altFt,
            ground = item.ground,
            gsKt = item.gsKt,
        )
    }
    events.sortWith(compareBy({ it.ring }, { it.miles }))
    return events to current
}

fun coalesce(events: List<AlertEvent>): List<AlertEvent> {
    if (events.size <= 1) return events
    val closest = events.minWith(compareBy({ it.ring }, { it.miles }))
    return listOf(closest.copy(extra = events.size - 1))
}

fun titleFor(event: AlertEvent): String {
    if (event.extra > 0) {
        val unit = if (event.ring == 1) "mile" else "miles"
        return "${event.extra + 1} aircraft within ${event.ring} $unit"
    }
    if (event.ring <= 1) return "Plane overhead"
    return "Plane within ${event.ring} miles"
}

fun bodyFor(event: AlertEvent): String {
    val callsign = event.callsign.ifBlank { event.hex.ifBlank { "Aircraft" } }
    val label = if (event.type.isNotBlank()) "$callsign (${event.type})" else callsign
    val parts = mutableListOf("$label is ${formatMiles(event.miles)} away")
    val alt = formatAlt(event.altFt, event.ground)
    if (alt.isNotEmpty()) parts += alt
    event.gsKt?.let { speed ->
        parts += String.format(Locale.US, "%.0f kt", speed)
    }
    var text = parts.joinToString(", ") + "."
    if (event.extra > 0) text += " ${event.extra} more inside the same ring."
    return text
}

fun urgencyFor(event: AlertEvent): String {
    return when {
        event.ring <= 1 -> "critical"
        event.ring <= 5 -> "normal"
        else -> "low"
    }
}

fun selectToast(events: List<AlertEvent>, notify: Boolean, secondsSinceLast: Double): AlertEvent? {
    if (!notify || events.isEmpty() || secondsSinceLast < NOTIFY_MIN_INTERVAL) return null
    return coalesce(events).single()
}
