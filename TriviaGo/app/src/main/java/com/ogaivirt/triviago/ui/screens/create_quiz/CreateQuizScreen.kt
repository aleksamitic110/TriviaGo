package com.ogaivirt.triviago.ui.screens.create_quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    viewModel: CreateQuizViewModel = hiltViewModel(),
    onNavigateToAddQuestions: (quizId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()



    LaunchedEffect(uiState.quizCreatedId) {
        uiState.quizCreatedId?.let { quizId ->
            onNavigateToAddQuestions(quizId)
            viewModel.onNavigatedToNextStep()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Napravi novi kviz") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "Osnovne informacije",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = uiState.quizName,
                onValueChange = viewModel::onQuizNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Naziv kviza") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.quizDescription,
                onValueChange = viewModel::onQuizDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Kratak opis kviza") }
            )

            // TODO: Dodati UI za izbor kategorije i da li je kviz privatan

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::onNextClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Dalje na dodavanje pitanja")
                }
            }
        }
    }
}