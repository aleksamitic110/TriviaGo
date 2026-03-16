package com.ogaivirt.triviago.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.userSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podešavanja Mape") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "Radijus pretrage: ${settings.searchRadius}m",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.searchRadius.toFloat(),
                onValueChange = { newValue ->
                    viewModel.onRadiusChange(newValue.roundToInt())
                },
                valueRange = 500f..5000f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Maksimalan broj markera: ${settings.markerDensity}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.markerDensity.toFloat(),
                onValueChange = { newValue ->
                    viewModel.onDensityChange(newValue.roundToInt())
                },
                valueRange = 5f..50f,
                steps = 8
            )
        }
    }
}