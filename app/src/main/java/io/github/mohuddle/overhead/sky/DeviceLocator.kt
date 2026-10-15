package io.github.mohuddle.overhead.sky

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import io.github.mohuddle.overhead.data.OverheadConfig
import io.github.mohuddle.overhead.domain.Place
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

const val DEVICE_DENIED = "Location permission is off. A ZIP or city works too."
const val DEVICE_TIMEOUT = "Couldn't read a device location."
private const val FIX_TIMEOUT_MS = 10_000L

fun devicePlace(lat: Double, lon: Double, accuracyM: Double?): Place {
    val label = String.format(Locale.US, "%.4f, %.4f", lat, lon)
    return Place(lat, lon, "device", label, accuracyM)
}

fun onPermissionDenied(config: OverheadConfig): OverheadConfig = config

fun locationProvider(): String {
    return if (Build.VERSION.SDK_INT >= 31) {
        LocationManager.FUSED_PROVIDER
    } else {
        LocationManager.GPS_PROVIDER
    }
}

@SuppressLint("MissingPermission")
fun readDeviceFix(manager: LocationManager, timeoutMs: Long = FIX_TIMEOUT_MS): Location? {
    val provider = locationProvider()
    return try {
        if (Build.VERSION.SDK_INT >= 30) {
            readCurrent(manager, provider, timeoutMs)
        } else {
            readOneShot(manager, provider, timeoutMs)
        }
    } catch (error: SecurityException) {
        throw error
    } catch (_: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
private fun readCurrent(manager: LocationManager, provider: String, timeoutMs: Long): Location? {
    val box = AtomicReference<Location?>()
    val latch = CountDownLatch(1)
    val signal = CancellationSignal()
    val executor = Executors.newSingleThreadExecutor()
    try {
        manager.getCurrentLocation(provider, signal, executor) { location ->
            box.set(location)
            latch.countDown()
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            signal.cancel()
            return null
        }
        return box.get()
    } finally {
        executor.shutdown()
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private fun readOneShot(manager: LocationManager, provider: String, timeoutMs: Long): Location? {
    val box = AtomicReference<Location?>()
    val latch = CountDownLatch(1)
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            box.set(location)
            latch.countDown()
        }
    }
    try {
        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) return null
        return box.get()
    } finally {
        manager.removeUpdates(listener)
    }
}
