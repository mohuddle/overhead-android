package io.github.mohuddle.overhead.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_MI = 3958.7613
private val LAT_NUMBER = """(-?\d+(?:\.\d+)?)"""
private val MAPS_AT = Regex("""@$LAT_NUMBER\s*,\s*$LAT_NUMBER""")
private val PAIR = Regex("""^$LAT_NUMBER\s*[,/\s]\s*$LAT_NUMBER$""")
private val HEMISPHERE = Regex("""^$LAT_NUMBER\s*([NSns])\s*[,/\s]?\s*$LAT_NUMBER\s*([EWew])$""")
private val ALLOWED_RINGS = setOf(1, 5, 10)
private val US_ZIP = Regex("""^\d{5}(?:-\d{4})?$""")

fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
    return 2 * EARTH_RADIUS_MI * asin(min(1.0, sqrt(a)))
}

fun validCoords(lat: Double, lon: Double): Boolean {
    return lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)
}

fun parseLocationText(text: String): Pair<Double, Double>? {
    val raw = text.trim()
    if (raw.isEmpty()) return null
    MAPS_AT.find(raw)?.let { match ->
        val lat = match.groupValues[1].toDouble()
        val lon = match.groupValues[2].toDouble()
        return if (validCoords(lat, lon)) lat to lon else null
    }
    HEMISPHERE.matchEntire(raw.replace("°", " "))?.let { match ->
        var lat = match.groupValues[1].toDouble()
        var lon = match.groupValues[3].toDouble()
        lat = if (match.groupValues[2].uppercase() == "S") -abs(lat) else abs(lat)
        lon = if (match.groupValues[4].uppercase() == "W") -abs(lon) else abs(lon)
        return if (validCoords(lat, lon)) lat to lon else null
    }
    PAIR.matchEntire(raw)?.let { match ->
        val lat = match.groupValues[1].toDouble()
        val lon = match.groupValues[2].toDouble()
        return if (validCoords(lat, lon)) lat to lon else null
    }
    return null
}

fun ringsInside(miles: Double, enabled: List<Int>): List<Int> {
    return enabled.filter { miles <= it.toDouble() }
}

fun innermostRing(miles: Double, enabled: List<Int>): Int? {
    return ringsInside(miles, enabled).minOrNull()
}

fun formatMiles(miles: Double): String {
    return if (miles < 10) {
        String.format(Locale.US, "%.1f mi", miles)
    } else {
        String.format(Locale.US, "%.0f mi", miles)
    }
}

fun formatAlt(altFt: Int?, ground: Boolean): String {
    if (ground || altFt == null) return "ground"
    return String.format(Locale.US, "%,d ft", altFt)
}

fun normalizeRings(value: List<Int>?): List<Int> {
    val rings = mutableListOf<Int>()
    value?.forEach { ring ->
        if (ring in ALLOWED_RINGS && ring !in rings) rings.add(ring)
    }
    return rings.ifEmpty { listOf(1, 5, 10) }
}

fun isUsZip(text: String): Boolean = US_ZIP.matches(text)
