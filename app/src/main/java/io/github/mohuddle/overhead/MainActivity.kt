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
import io.github.mohuddle.overhead.sky.notificationsDenied
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

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) model.readDevice() else model.denyDevice()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[OverheadViewModel::class.java]
        model.setNotifyDenied(
            notificationsDenied(
                Build.VERSION.SDK_INT,
                granted(Manifest.permission.POST_NOTIFICATIONS),
            ),
        )
        setContent {
            OverheadTheme {
                var draft by rememberSaveable { mutableStateOf("") }
                val state by model.screen.collectAsStateWithLifecycle()
                OverheadScreen(
                    state = state,
                    draft = draft,
                    onDraft = { draft = it },
                    onSet = { model.set(draft) },
                    onDevice = ::askDevice,
                    onWatch = { model.toggleWatch(::askNotifications) },
                    onNotify = { model.toggleNotify(::askNotifications) },
                    onClear = { model.clear() },
                    onRing = model::ring,
                    onRow = ::openRow,
                )
            }
        }
    }

    private fun askDevice() {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine || coarse) {
            model.readDevice()
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun granted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun askNotifications() {
        val denied = notificationsDenied(
            Build.VERSION.SDK_INT,
            granted(Manifest.permission.POST_NOTIFICATIONS),
        )
        model.setNotifyDenied(denied)
        if (denied) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openRow(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
