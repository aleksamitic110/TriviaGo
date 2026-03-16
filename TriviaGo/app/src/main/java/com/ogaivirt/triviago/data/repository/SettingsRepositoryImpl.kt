package com.ogaivirt.triviago.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ogaivirt.triviago.domain.repository.SettingsRepository
import com.ogaivirt.triviago.domain.repository.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val SEARCH_RADIUS = intPreferencesKey("search_radius")
        val MARKER_DENSITY = intPreferencesKey("marker_density")
    }

    override fun getUserSettings(): Flow<UserSettings> {
        return context.dataStore.data.map { preferences ->
            UserSettings(
                searchRadius = preferences[Keys.SEARCH_RADIUS] ?: 1000,
                markerDensity = preferences[Keys.MARKER_DENSITY] ?: 15
            )
        }
    }

    override suspend fun updateSearchRadius(radius: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.SEARCH_RADIUS] = radius
        }
    }

    override suspend fun updateMarkerDensity(density: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.MARKER_DENSITY] = density
        }
    }
}
