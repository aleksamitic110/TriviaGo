package com.ogaivirt.triviago.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun calculateDistanceInMeters(start: LatLng, end: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        start.latitude,
        start.longitude,
        end.latitude,
        end.longitude,
        results
    )
    return results[0]
}