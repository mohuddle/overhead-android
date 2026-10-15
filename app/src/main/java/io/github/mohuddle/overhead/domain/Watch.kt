package io.github.mohuddle.overhead.domain

fun shouldRunService(watch: Boolean, place: Place?): Boolean = watch && place != null

fun resumesOnColdStart(watch: Boolean, place: Place?): Boolean = shouldRunService(watch, place)

fun shouldKeepPoll(started: Place, current: Place?, watch: Boolean): Boolean {
    if (!watch || current == null) return false
    return started.lat == current.lat && started.lon == current.lon
}
