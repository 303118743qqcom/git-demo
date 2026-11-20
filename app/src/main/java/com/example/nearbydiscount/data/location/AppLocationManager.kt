package com.example.nearbydiscount.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AppLocationManager(private val context: Context) {

    sealed interface LocationResult {
        data class Success(val latitude: Double, val longitude: Double) : LocationResult
        data object PermissionDenied : LocationResult
        data object Unavailable : LocationResult
        data object Timeout : LocationResult
    }

    suspend fun currentLocation(): LocationResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext LocationResult.PermissionDenied

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext LocationResult.Unavailable
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return@withContext LocationResult.Unavailable

        val recent = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }

        if (recent != null && System.currentTimeMillis() - recent.time < 60_000L) {
            return@withContext recent.toResult()
        }

        suspendCancellableCoroutine<LocationResult> { cont ->
            val handler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (cont.isActive) {
                        manager.removeUpdates(this)
                        handler.removeCallbacksAndMessages(null)
                        cont.resume(location.toResult())
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }

            providers.forEach { provider ->
                runCatching {
                    manager.requestLocationUpdates(provider, 1_000L, 10f, listener, Looper.getMainLooper())
                }
            }

            val timeoutRunnable = Runnable {
                if (cont.isActive) {
                    manager.removeUpdates(listener)
                    cont.resume(recent?.toResult() ?: LocationResult.Timeout)
                }
            }
            handler.postDelayed(timeoutRunnable, 12_000L)

            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                manager.removeUpdates(listener)
            }
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun Location.toResult() = LocationResult.Success(latitude, longitude)
}
