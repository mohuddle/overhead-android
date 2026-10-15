package io.github.mohuddle.overhead.domain

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

private const val MI_PER_NM = 1.150779448
private const val LIST_RADIUS_MI = 20.0
private const val QUERY_NM = 25

data class FeedEndpoint(val name: String, val url: String)

data class FeedChoice(
    val replace: Boolean,
    val aircraft: List<Aircraft>,
    val provider: String,
    val error: String,
)

fun feedUrls(lat: Double, lon: Double): List<FeedEndpoint> {
    val needed = ceil(LIST_RADIUS_MI / MI_PER_NM).toInt()
    val nm = max(5, min(250, max(needed, QUERY_NM)))
    val latText = String.format(Locale.US, "%.4f", lat)
    val lonText = String.format(Locale.US, "%.4f", lon)
    return listOf(
        FeedEndpoint("adsb.fi", "https://opendata.adsb.fi/api/v3/lat/$latText/lon/$lonText/dist/$nm"),
        FeedEndpoint("adsb.lol", "https://api.adsb.lol/v2/lat/$latText/lon/$lonText/dist/$nm"),
        FeedEndpoint("opensky", openskyUrl(lat, lon, nm.toDouble())),
    )
}

fun fetchWith(
    get: (url: String, userAgent: String) -> String,
    lat: Double,
    lon: Double,
    rings: List<Int>,
): FeedChoice {
    val origin = lat to lon
    val endpoints = feedUrls(lat, lon)
    val errors = mutableListOf<String>()
    var remembered: FeedChoice? = null
    for (endpoint in endpoints.take(2)) {
        val aircraft = try {
            readAircraft(endpoint.name, get(endpoint.url, USER_AGENT), origin, rings)
        } catch (error: Exception) {
            errors += "${endpoint.name}: ${error.message}"
            continue
        }
        if (aircraft.isNotEmpty()) return FeedChoice(true, aircraft, endpoint.name, "")
        remembered = FeedChoice(true, emptyList(), endpoint.name, "")
    }
    val opensky = endpoints.last()
    val openskyAircraft = try {
        readAircraft(opensky.name, get(opensky.url, USER_AGENT), origin, rings)
    } catch (error: Exception) {
        errors += "${opensky.name}: ${error.message}"
        null
    }
    if (openskyAircraft != null && (openskyAircraft.isNotEmpty() || remembered == null)) {
        return FeedChoice(true, openskyAircraft, opensky.name, "")
    }
    remembered?.let { return it }
    return FeedChoice(false, emptyList(), "", "ADS-B feeds failed (${errors.joinToString("; ")})")
}

private fun readAircraft(
    name: String,
    body: String,
    origin: Pair<Double, Double>,
    rings: List<Int>,
): List<Aircraft> {
    val root = JsonParser.parseString(body).asJsonObject
    val normalized = if (name == "opensky") {
        val states = root.get("states")
        val rows = if (states is JsonArray) states else JsonArray()
        rows.mapNotNull { element ->
            if (!element.isJsonArray) return@mapNotNull null
            normalizeOpensky(element.asJsonArray.toLooseList(), origin, rings)
        }
    } else {
        val rows = root.get("ac")
        if (rows !is JsonArray) throw IllegalStateException("unexpected ADS-B payload")
        rows.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            normalizeExchange(element.asJsonObject.toLooseMap(), origin, rings)
        }
    }
    return filterListed(normalized)
}

private fun openskyUrl(lat: Double, lon: Double, nm: Double): String {
    val dLat = nm / 60.0
    val dLon = nm / max(0.2, 60.0 * abs(cos(Math.toRadians(lat))))
    val laMin = max(-90.0, lat - dLat)
    val laMax = min(90.0, lat + dLat)
    val loMin = max(-180.0, lon - dLon)
    val loMax = min(180.0, lon + dLon)
    return "https://opensky-network.org/api/states/all" +
        "?lamin=${format4(laMin)}&lomin=${format4(loMin)}&lamax=${format4(laMax)}&lomax=${format4(loMax)}"
}

private fun format4(value: Double): String = String.format(Locale.US, "%.4f", value)

private fun JsonObject.toLooseMap(): Map<String, Any?> {
    val map = linkedMapOf<String, Any?>()
    for ((key, value) in entrySet()) map[key] = value.toLoose()
    return map
}

private fun JsonArray.toLooseList(): List<Any?> = map { it.toLoose() }

private fun com.google.gson.JsonElement.toLoose(): Any? {
    if (isJsonNull) return null
    if (!isJsonPrimitive) return toString()
    val primitive = asJsonPrimitive
    return when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isNumber -> primitive.asDouble
        primitive.isString -> primitive.asString
        else -> null
    }
}
