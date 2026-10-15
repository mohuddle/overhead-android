package io.github.mohuddle.overhead

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.mohuddle.overhead.ui.OverheadScreen
import io.github.mohuddle.overhead.ui.PresentInput
import io.github.mohuddle.overhead.ui.present
import io.github.mohuddle.overhead.ui.theme.OverheadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OverheadTheme {
                var draft by rememberSaveable { mutableStateOf("") }
                val state = present(
                    PresentInput(
                        place = null,
                        rings = listOf(1, 5, 10),
                        notify = true,
                        watch = false,
                        aircraft = emptyList(),
                        error = "",
                        counts = emptyMap(),
                        lastSuccessProvider = "",
                        timeLabel = null,
                        placeError = null,
                        deviceError = null,
                        notifyDenied = false,
                    ),
                )
                OverheadScreen(
                    state = state,
                    draft = draft,
                    onDraft = { draft = it },
                    onSet = {},
                    onDevice = {},
                    onWatch = {},
                    onNotify = {},
                    onClear = {},
                    onRing = {},
                    onRow = {},
                )
            }
        }
    }
}
