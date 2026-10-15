package io.github.mohuddle.overhead.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.normalizeRings

data class OverheadConfig(
    val place: Place?,
    val rings: List<Int>,
    val notify: Boolean,
    val watch: Boolean,
    val alerted: Map<String, List<Int>>,
)

fun afterClear(config: OverheadConfig): OverheadConfig {
    return config.copy(place = null, watch = false, alerted = emptyMap())
}

fun decodeAlerted(raw: String?): Map<String, List<Int>> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val type = object : TypeToken<Map<String, List<Int>>>() {}.type
        Gson().fromJson<Map<String, List<Int>>>(raw, type) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

fun decodeRings(raw: String?): List<Int> {
    val parsed = raw.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
    return normalizeRings(parsed)
}
