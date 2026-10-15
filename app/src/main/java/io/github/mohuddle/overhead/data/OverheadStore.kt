package io.github.mohuddle.overhead.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.normalizeRings
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OverheadStore internal constructor(
    private val preferences: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
) {
    val config: Flow<OverheadConfig> = preferences.data.map { prefs -> prefs.toConfig() }

    suspend fun setPlace(place: Place?) {
        preferences.edit { prefs ->
            if (place == null) {
                prefs.remove(LAT)
                prefs.remove(LON)
                prefs.remove(SOURCE)
                prefs.remove(LABEL)
                prefs.remove(ACCURACY)
            } else {
                prefs[LAT] = place.lat
                prefs[LON] = place.lon
                prefs[SOURCE] = place.source
                prefs[LABEL] = place.label
                if (place.accuracyM == null) prefs.remove(ACCURACY) else prefs[ACCURACY] = place.accuracyM
            }
        }
    }

    suspend fun setRings(rings: List<Int>) {
        preferences.edit { prefs ->
            prefs[RINGS] = normalizeRings(rings).joinToString(",")
        }
    }

    suspend fun setNotify(on: Boolean) {
        preferences.edit { prefs -> prefs[NOTIFY] = on }
    }

    suspend fun setWatch(on: Boolean) {
        preferences.edit { prefs -> prefs[WATCH] = on }
    }

    suspend fun setAlerted(alerted: Map<String, List<Int>>) {
        preferences.edit { prefs ->
            prefs[ALERTED] = com.google.gson.Gson().toJson(alerted)
        }
    }

    suspend fun clearPlace() {
        preferences.edit { prefs ->
            val cleared = afterClear(prefs.toConfig())
            prefs.clear()
            prefs.write(cleared)
        }
    }

    companion object {
        fun create(context: Context): OverheadStore {
            return create(File(context.filesDir, "overhead.preferences_pb"))
        }

        fun create(file: File): OverheadStore {
            return OverheadStore(PreferenceDataStoreFactory.create(produceFile = { file }))
        }
    }
}

private val LAT = doublePreferencesKey("lat")
private val LON = doublePreferencesKey("lon")
private val SOURCE = stringPreferencesKey("source")
private val LABEL = stringPreferencesKey("label")
private val ACCURACY = doublePreferencesKey("accuracyM")
private val RINGS = stringPreferencesKey("rings")
private val NOTIFY = booleanPreferencesKey("notify")
private val WATCH = booleanPreferencesKey("watch")
private val ALERTED = stringPreferencesKey("alerted")

private fun androidx.datastore.preferences.core.Preferences.toConfig(): OverheadConfig {
    val lat = this[LAT]
    val lon = this[LON]
    val place = if (lat == null || lon == null) {
        null
    } else {
        Place(lat, lon, this[SOURCE].orEmpty(), this[LABEL].orEmpty(), this[ACCURACY])
    }
    return OverheadConfig(
        place = place,
        rings = decodeRings(this[RINGS]),
        notify = this[NOTIFY] ?: true,
        watch = this[WATCH] ?: false,
        alerted = decodeAlerted(this[ALERTED]),
    )
}

private fun androidx.datastore.preferences.core.MutablePreferences.write(config: OverheadConfig) {
    val place = config.place
    if (place == null) {
        remove(LAT)
        remove(LON)
        remove(SOURCE)
        remove(LABEL)
        remove(ACCURACY)
    } else {
        this[LAT] = place.lat
        this[LON] = place.lon
        this[SOURCE] = place.source
        this[LABEL] = place.label
        if (place.accuracyM == null) remove(ACCURACY) else this[ACCURACY] = place.accuracyM
    }
    this[RINGS] = config.rings.joinToString(",")
    this[NOTIFY] = config.notify
    this[WATCH] = config.watch
    this[ALERTED] = com.google.gson.Gson().toJson(config.alerted)
}
