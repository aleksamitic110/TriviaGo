package com.ogaivirt.triviago.ui.screens.register

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit // Funkcija za povratak na login
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current



    if (uiState.registrationSuccess) {
        AlertDialog(
            onDismissRequest = {}, // Prazno da ne može da se zatvori klikom sa strane
            icon = { Icon(Icons.Default.MarkEmailRead, contentDescription = "Email ikonica") },
            title = { Text(text = "Registracija uspešna!") },
            text = { Text("Poslali smo vam verifikacioni link. Molimo vas, aktivirajte nalog pre prijave.") },
            confirmButton = {
                TextButton(
                    onClick = onNavigateToLogin // Klikom na dugme se vraćamo na login
                ) {
                    Text("U redu")
                }
            }
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.onErrorMessageShown() // Javljamo ViewModel-u da je poruka prikazana
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kreiraj novi nalog 🚀",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pridruži se TriviaGO zajednici!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Polje za unos imena
            OutlinedTextField(
                value = uiState.ime,
                onValueChange = viewModel::onImeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ime") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Polje za unos emaila
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email adresa") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Polje za unos lozinke
            OutlinedTextField(
                value = uiState.lozinka,
                onValueChange = viewModel::onLozinkaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lozinka") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = viewModel::onPasswordVisibilityChange) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Polje za potvrdu lozinke
            OutlinedTextField(
                value = uiState.potvrdaLozinke,
                onValueChange = viewModel::onPotvrdaLozinkeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Potvrdi lozinku") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    val image = if (uiState.isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = viewModel::onConfirmPasswordVisibilityChange) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Dugme za registraciju
            Button(
                onClick = viewModel::onSignUpClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(text = "Registruj se", fontSize = 16.sp)
            }

            // Link za povratak na prijavu
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        "Već imaš nalog? Prijavi se",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}