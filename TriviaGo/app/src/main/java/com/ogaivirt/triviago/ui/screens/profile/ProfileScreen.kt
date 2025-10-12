package com.ogaivirt.triviago.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Podešavanja profila") }) }
    ) { innerPadding ->
        if (uiState.isLoading) {

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.userProfile != null) {
            val profile = uiState.userProfile!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                AsyncImage(
                    model = profile.profilePictureUrl,
                    contentDescription = "Slika profila",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    // TODO: Dodati placeholder sliku
                )

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = profile.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Korisničko ime") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))


                OutlinedTextField(
                    value = profile.description ?: "",
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))


                Button(onClick = viewModel::onSaveChanges, modifier = Modifier.fillMaxWidth()) {
                    Text("Sačuvaj izmene")
                }

                Spacer(modifier = Modifier.weight(1f))


                Button(
                    onClick = {
                        viewModel.onSignOutClick()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Odjavi se")
                }
            }
        } else {

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Došlo je do greške.")
            }
        }
    }
}