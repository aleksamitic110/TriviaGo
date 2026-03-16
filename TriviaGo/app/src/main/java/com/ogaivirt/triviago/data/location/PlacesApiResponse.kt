
package com.ogaivirt.triviago.data.location

import com.google.gson.annotations.SerializedName

data class PlacesApiResponse(
    val results: List<PlaceResult>,
    val status: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class PlaceResult(
    val geometry: Geometry,
    val name: String,
    @SerializedName("vicinity") val address: String? = null,
    @SerializedName("place_id") val placeId: String? = null
)

data class Geometry(
    val location: PlaceLocation
)

data class PlaceLocation(
    val lat: Double,
    val lng: Double
)