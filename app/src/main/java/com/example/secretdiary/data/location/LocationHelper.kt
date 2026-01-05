package com.example.secretdiary.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.atomic.AtomicBoolean

class LocationHelper(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Get current location:
     * 1) lastLocation (fast)
     * 2) requestSingleUpdate (reliable on real phones)
     * 3) fallback: LocationManager lastKnown
     */
    fun getCurrentLocation(
        timeoutMs: Long = 12000L,
        onResult: (Location?) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        if (!isLocationEnabled()) {
            onResult(null)
            return
        }

        // 1) Try last known fused location first
        tryLastLocation { last ->
            if (last != null) {
                onResult(last)
            } else {
                // 2) Force one update
                requestSingleUpdate(timeoutMs) { fresh ->
                    if (fresh != null) onResult(fresh)
                    else onResult(getLastKnownFromLocationManager())
                }
            }
        }
    }

    private fun tryLastLocation(onResult: (Location?) -> Unit) {
        try {
            fusedClient.lastLocation
                .addOnSuccessListener { onResult(it) }
                .addOnFailureListener { onResult(null) }
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(timeoutMs: Long, onResult: (Location?) -> Unit) {
        val finished = AtomicBoolean(false)

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, 1000L)
            .setMaxUpdates(1)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (finished.compareAndSet(false, true)) {
                    fusedClient.removeLocationUpdates(this)
                    onResult(result.lastLocation)
                }
            }
        }

        // Timeout (runs on main thread, cancels updates if no result)
        val handler = android.os.Handler(appContext.mainLooper)
        val timeoutRunnable = Runnable {
            if (finished.compareAndSet(false, true)) {
                fusedClient.removeLocationUpdates(callback)
                onResult(null)
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        try {
            fusedClient.requestLocationUpdates(
                request,
                callback,
                appContext.mainLooper
            ).addOnFailureListener {
                handler.removeCallbacks(timeoutRunnable)
                if (finished.compareAndSet(false, true)) {
                    onResult(null)
                }
            }
        } catch (_: SecurityException) {
            handler.removeCallbacks(timeoutRunnable)
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownFromLocationManager(): Location? {
        return try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            providers
                .mapNotNull { provider ->
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else null
                }
                .maxByOrNull { it.time } // newest
        } catch (_: Exception) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() || hasCoarseLocationPermission()
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }
}
