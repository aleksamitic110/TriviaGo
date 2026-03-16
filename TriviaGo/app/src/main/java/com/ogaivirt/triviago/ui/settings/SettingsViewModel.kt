package com.ogaivirt.triviago.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.repository.SettingsRepository
import com.ogaivirt.triviago.domain.repository.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {


    val userSettings = settingsRepo.getUserSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings(1000, 15)
    )

    fun onRadiusChange(radius: Int) {
        viewModelScope.launch {
            settingsRepo.updateSearchRadius(radius)
        }
    }

    fun onDensityChange(density: Int) {
        viewModelScope.launch {
            settingsRepo.updateMarkerDensity(density)
        }
    }

}