package com.example.secretdiary.data.location


import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices

class LocationHelper(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onResult: (Location?) -> Unit) {
        fusedClient.lastLocation
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    }
}
