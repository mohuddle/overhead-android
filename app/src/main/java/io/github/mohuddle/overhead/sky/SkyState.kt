package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.domain.Aircraft

data class SkyState(
    val aircraft: List<Aircraft>,
    val provider: String,
    val error: String,
    val updatedEpochMs: Long,
    val lastSuccessProvider: String,
    val lastSuccessEpochMs: Long,
)

fun emptySky(): SkyState = SkyState(
    aircraft = emptyList(),
    provider = "",
    error = "",
    updatedEpochMs = 0L,
    lastSuccessProvider = "",
    lastSuccessEpochMs = 0L,
)
