package io.github.mohuddle.overhead.sky

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SkyHolder {
    private val state = MutableStateFlow(emptySky())
    val sky: StateFlow<SkyState> = state.asStateFlow()

    fun publish(sky: SkyState) {
        state.value = sky
    }
}
