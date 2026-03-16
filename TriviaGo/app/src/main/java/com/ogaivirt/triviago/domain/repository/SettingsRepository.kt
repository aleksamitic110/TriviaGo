package com.ogaivirt.triviago.domain.repository

import kotlinx.coroutines.flow.Flow

data class UserSettings(
    val searchRadius: Int,
    val markerDensity: Int
)

interface SettingsRepository {
    fun getUserSettings(): Flow<UserSettings>

    suspend fun updateSearchRadius(radius: Int)
    suspend fun updateMarkerDensity(density: Int)
}