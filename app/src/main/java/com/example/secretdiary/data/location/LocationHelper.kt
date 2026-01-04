package com.example.secretdiary.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class LocationHelper(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    /**
     * Get current location:
     * 1) Uses lastLocation (fast)
     * 2) Uses getCurrentLocation (fresh)
     * 3) If still null, requests one-time update
     */
    fun getCurrentLocation(
        timeoutMs: Long = 8000L,
        onResult: (Location?) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        if (!isLocationEnabled()) {
            // GPS / Location services are OFF
            onResult(null)
            return
        }

        // Try last location first
        tryLastLocation(timeoutMs, onResult)
    }

    private fun tryLastLocation(timeoutMs: Long, onResult: (Location?) -> Unit) {
        try {
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        onResult(loc)
                    } else {
                        tryGetCurrentLocation(timeoutMs, onResult)
                    }
                }
                .addOnFailureListener {
                    tryGetCurrentLocation(timeoutMs, onResult)
                }
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryGetCurrentLocation(timeoutMs: Long, onResult: (Location?) -> Unit) {
        val tokenSource = CancellationTokenSource()

        // Use HIGH only if FINE is granted, otherwise BALANCED for COARSE
        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        // Timeout guard (so we never hang)
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        val finished = AtomicBoolean(false)

        val timeoutJob = scope.launch {
            delay(timeoutMs)
            if (finished.compareAndSet(false, true)) {
                tokenSource.cancel()
                // If getCurrentLocation timed out, try one-time update request
                requestSingleUpdate(timeoutMs, onResult)
            }
        }

        try {
            fusedClient.getCurrentLocation(priority, tokenSource.token)
                .addOnSuccessListener { fresh ->
                    if (finished.compareAndSet(false, true)) {
                        timeoutJob.cancel()
                        if (fresh != null) onResult(fresh)
                        else requestSingleUpdate(timeoutMs, onResult)
                    }
                }
                .addOnFailureListener {
                    if (finished.compareAndSet(false, true)) {
                        timeoutJob.cancel()
                        requestSingleUpdate(timeoutMs, onResult)
                    }
                }
        } catch (_: SecurityException) {
            timeoutJob.cancel()
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(timeoutMs: Long, onResult: (Location?) -> Unit) {
        val finished = AtomicBoolean(false)
        val scope = CoroutineScope(Dispatchers.Main.immediate)

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

        // Timeout guard for update request
        val timeoutJob = scope.launch {
            delay(timeoutMs)
            if (finished.compareAndSet(false, true)) {
                fusedClient.removeLocationUpdates(callback)
                onResult(null)
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                request,
                callback,
                appContext.mainLooper
            ).addOnFailureListener {
                timeoutJob.cancel()
                if (finished.compareAndSet(false, true)) onResult(null)
            }
        } catch (_: SecurityException) {
            timeoutJob.cancel()
            onResult(null)
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
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }
}
