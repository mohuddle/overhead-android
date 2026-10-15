package io.github.mohuddle.overhead.domain

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import java.util.Locale

const val USER_AGENT =
    "overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)"

data class Place(
    val lat: Double,
    val lon: Double,
    val source: String,
    val label: String,
    val accuracyM: Double?,
)

sealed class SetQuery {
    data class Manual(val place: Place) : SetQuery()
    data class Remote(val url: String) : SetQuery()
    data class Rejected(val message: String) : SetQuery()
}

fun noMatchMessage(query: String): String {
    return "no match for '$query'. Try a ZIP, city, or lat, lon."
}

fun classifySet(text: String): SetQuery {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
        return SetQuery.Rejected("enter a ZIP, city, or coordinates")
    }
    parseLocationText(trimmed)?.let { (lat, lon) ->
        val label = String.format(Locale.US, "%.4f, %.4f", lat, lon)
        return SetQuery.Manual(Place(lat, lon, "manual", label, null))
    }
    val url = if (isUsZip(trimmed)) {
        val postal = trimmed.take(5)
        "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&postalcode=$postal&country=us"
    } else {
        val q = URLEncoder.encode(trimmed, Charsets.UTF_8)
        "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$q"
    }
    return SetQuery.Remote(url)
}

fun placeFromNominatim(query: String, body: String): Place {
    val rows = Gson().fromJson(body, JsonArray::class.java) ?: JsonArray()
    if (rows.size() == 0) throw IllegalStateException(noMatchMessage(query))
    val row = rows[0].asJsonObject
    val lat = row.flexibleDouble("lat")
    val lon = row.flexibleDouble("lon")
    if (lat == null || lon == null || !validCoords(lat, lon)) {
        throw IllegalStateException("geocoder returned unusable coordinates")
    }
    val display = row.get("display_name")?.takeUnless { it.isJsonNull }?.asString
    val label = display?.trim()?.takeIf { it.isNotEmpty() } ?: query.trim()
    return Place(lat, lon, "manual", label, null)
}

private fun JsonObject.flexibleDouble(name: String): Double? {
    val element = get(name) ?: return null
    if (element.isJsonNull || !element.isJsonPrimitive) return null
    val primitive = element.asJsonPrimitive
    return when {
        primitive.isNumber -> primitive.asDouble
        primitive.isString -> primitive.asString.toDoubleOrNull()
        else -> null
    }
}
