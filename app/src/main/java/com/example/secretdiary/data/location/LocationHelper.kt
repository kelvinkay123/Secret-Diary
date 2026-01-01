package com.example.secretdiary.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(context: Context) {

    // Prevent memory leaks by using applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val appContext = context.applicationContext

    fun getCurrentLocation(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        try {
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onResult(location)
                    } else {
                        fetchFreshLocation(onResult)
                    }
                }
                .addOnFailureListener {
                    fetchFreshLocation(onResult)
                }
        } catch (_: SecurityException) {
            // FIXED: Renamed 'e' to '_' to silence the "unused parameter" warning
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFreshLocation(onResult: (Location?) -> Unit) {
        val tokenSource = CancellationTokenSource()

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { freshLocation ->
            onResult(freshLocation)
        }.addOnFailureListener {
            onResult(null)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFine || hasCoarse
    }
}