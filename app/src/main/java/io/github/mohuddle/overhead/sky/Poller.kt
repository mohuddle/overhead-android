package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.domain.AlertEvent
import io.github.mohuddle.overhead.domain.FeedChoice
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.alertable
import io.github.mohuddle.overhead.domain.newAlerts
import io.github.mohuddle.overhead.domain.selectToast

data class PollResult(
    val sky: SkyState,
    val alerted: Map<String, List<Int>>,
    val toast: AlertEvent?,
    val fetched: Boolean,
)

fun poll(
    place: Place?,
    rings: List<Int>,
    notify: Boolean,
    alerted: Map<String, List<Int>>,
    sky: SkyState,
    secondsSinceLast: Double,
    nowEpochMs: Long,
    fetch: (Place, List<Int>) -> FeedChoice,
): PollResult {
    if (place == null) {
        return PollResult(sky, alerted, toast = null, fetched = false)
    }
    val choice = fetch(place, rings)
    if (!choice.replace) {
        return PollResult(
            sky = sky.copy(provider = "", error = choice.error),
            alerted = alerted,
            toast = null,
            fetched = true,
        )
    }
    val next = sky.copy(
        aircraft = choice.aircraft,
        provider = choice.provider,
        error = "",
        updatedEpochMs = nowEpochMs,
        lastSuccessProvider = choice.provider,
        lastSuccessEpochMs = nowEpochMs,
    )
    val (events, memory) = newAlerts(alerted, choice.aircraft.filter(::alertable), rings)
    return PollResult(
        sky = next,
        alerted = memory,
        toast = selectToast(events, notify, secondsSinceLast),
        fetched = true,
    )
}
