package io.github.mohuddle.overhead

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mohuddle.overhead.ui.OverheadScreen
import io.github.mohuddle.overhead.ui.OverheadViewModel
import io.github.mohuddle.overhead.ui.theme.OverheadTheme

class MainActivity : ComponentActivity() {
    private lateinit var model: OverheadViewModel

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        model.setNotifyDenied(!granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[OverheadViewModel::class.java]
        setContent {
            OverheadTheme {
                var draft by rememberSaveable { mutableStateOf("") }
                val state by model.screen.collectAsStateWithLifecycle()
                OverheadScreen(
                    state = state,
                    draft = draft,
                    onDraft = { draft = it },
                    onSet = { model.set(draft) },
                    onDevice = {},
                    onWatch = { model.toggleWatch(::askNotifications) },
                    onNotify = { model.toggleNotify(::askNotifications) },
                    onClear = { model.clear() },
                    onRing = model::ring,
                    onRow = ::openRow,
                )
            }
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < 33) {
            model.setNotifyDenied(false)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            model.setNotifyDenied(false)
        } else {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openRow(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
