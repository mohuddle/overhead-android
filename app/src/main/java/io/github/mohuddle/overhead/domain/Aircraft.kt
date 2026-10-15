package io.github.mohuddle.overhead.domain

import kotlin.math.roundToInt

private const val LIST_RADIUS_MI = 20.0
private const val MAX_AIRCRAFT = 40
private const val SEEN_MAX_SECONDS = 60.0

data class Aircraft(
    val hex: String,
    val callsign: String,
    val registration: String,
    val type: String,
    val desc: String,
    val lat: Double,
    val lon: Double,
    val altFt: Int?,
    val ground: Boolean,
    val gsKt: Double?,
    val track: Double?,
    val miles: Double,
    val bearing: Double,
    val ring: Int?,
    val seen: Double?,
)

fun normalizeExchange(
    item: Map<String, Any?>,
    origin: Pair<Double, Double>,
    rings: List<Int>,
): Aircraft? {
    val lat = asDouble(item["lat"]) ?: return null
    val lon = asDouble(item["lon"]) ?: return null
    var (altFt, ground) = altitude(item["alt_baro"])
    if (!ground && altFt == null) {
        val geom = altitude(item["alt_geom"])
        altFt = geom.first
        ground = geom.second
    }
    val hex = item["hex"]?.toString()?.trim()?.lowercase().orEmpty()
    if (hex.isEmpty()) return null
    val miles = haversineMiles(origin.first, origin.second, lat, lon)
    return Aircraft(
        hex = hex,
        callsign = callsign(item["flight"], item["r"], hex.uppercase()),
        registration = item["r"]?.toString()?.trim().orEmpty(),
        type = item["t"]?.toString()?.trim().orEmpty(),
        desc = item["desc"]?.toString()?.trim().orEmpty(),
        lat = lat,
        lon = lon,
        altFt = altFt,
        ground = ground,
        gsKt = asDouble(item["gs"]),
        track = asDouble(item["track"]),
        miles = roundTo(miles, 2),
        bearing = roundTo(bearingDegrees(origin.first, origin.second, lat, lon), 1),
        ring = innermostRing(miles, rings),
        seen = asDouble(item["seen_pos"] ?: item["seen"]),
    )
}

fun normalizeOpensky(
    row: List<Any?>,
    origin: Pair<Double, Double>,
    rings: List<Int>,
): Aircraft? {
    if (row.size < 11) return null
    val lon = asDouble(row[5]) ?: return null
    val lat = asDouble(row[6]) ?: return null
    val ground = row[8] == true
    val altM = asDouble(row[7])
    val altFt = altM?.let { (it * 3.28084).roundToInt() }
    val gsMs = asDouble(row[9])
    val hex = row[0]?.toString()?.trim()?.lowercase().orEmpty()
    if (hex.isEmpty()) return null
    val miles = haversineMiles(origin.first, origin.second, lat, lon)
    return Aircraft(
        hex = hex,
        callsign = callsign(row[1], hex.uppercase()),
        registration = "",
        type = "",
        desc = "",
        lat = lat,
        lon = lon,
        altFt = altFt,
        ground = ground,
        gsKt = gsMs?.times(1.94384),
        track = asDouble(row[10]),
        miles = roundTo(miles, 2),
        bearing = roundTo(bearingDegrees(origin.first, origin.second, lat, lon), 1),
        ring = innermostRing(miles, rings),
        seen = null,
    )
}

fun filterListed(aircraft: List<Aircraft>): List<Aircraft> {
    return aircraft
        .filter { item ->
            val seen = item.seen
            (seen == null || seen <= SEEN_MAX_SECONDS) && item.miles <= LIST_RADIUS_MI
        }
        .sortedBy { it.miles }
        .take(MAX_AIRCRAFT)
}

fun summarizeCounts(aircraft: List<Aircraft>): Map<String, Int> {
    val counts = mutableMapOf("1" to 0, "5" to 0, "10" to 0)
    for (item in aircraft) {
        for (ring in listOf(1, 5, 10)) {
            if (item.miles <= ring) counts[ring.toString()] = counts.getValue(ring.toString()) + 1
        }
    }
    return counts
}

fun globeUrl(hex: String): String {
    return "https://globe.adsbexchange.com/?icao=${hex.lowercase()}"
}

fun aircraftLine(item: Aircraft): String {
    val bits = mutableListOf(formatMiles(item.miles), item.callsign)
    if (item.type.isNotBlank()) bits += item.type
    val alt = formatAlt(item.altFt, item.ground)
    if (alt.isNotBlank()) bits += alt
    return bits.joinToString("  ")
}

fun nearestLine(hasLocation: Boolean, nearest: Aircraft?): String {
    if (nearest == null) {
        return if (hasLocation) "No aircraft in range" else "Set a location to watch the sky"
    }
    return "${nearest.callsign} · ${formatMiles(nearest.miles)}"
}

private fun callsign(vararg parts: Any?): String {
    for (part in parts) {
        val text = part?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) return text
    }
    return ""
}

private fun altitude(value: Any?): Pair<Int?, Boolean> {
    if (value == "ground" || value == true) return null to true
    val feet = asDouble(value) ?: return null to false
    return feet.roundToInt() to false
}

private fun asDouble(value: Any?): Double? {
    return when (value) {
        null -> null
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }
}

private fun roundTo(value: Double, places: Int): Double {
    val scale = when (places) {
        1 -> 10.0
        else -> 100.0
    }
    return kotlin.math.round(value * scale) / scale
}

private fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLambda = Math.toRadians(lon2 - lon1)
    val y = kotlin.math.sin(dLambda) * kotlin.math.cos(phi2)
    val x = kotlin.math.cos(phi1) * kotlin.math.sin(phi2) -
        kotlin.math.sin(phi1) * kotlin.math.cos(phi2) * kotlin.math.cos(dLambda)
    return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
}
