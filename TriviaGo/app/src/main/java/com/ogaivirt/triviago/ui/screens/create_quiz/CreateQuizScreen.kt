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
import com.ogaivirt.triviago.domain.model.QuizDifficulty

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
                singleLine = true,
                isError = uiState.quizNameError != null,
                supportingText = {
                    if (uiState.quizNameError != null) {
                        Text(text = uiState.quizNameError.toString())
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.quizDescription,
                onValueChange = viewModel::onQuizDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Kratak opis kviza") },
                isError = uiState.quizDescriptionError != null,
                supportingText = {
                    if (uiState.quizDescriptionError != null) {
                        Text(text = uiState.quizDescriptionError.toString())
                    }
                }
            )


            Spacer(modifier = Modifier.height(24.dp))
            Text("Izaberi težinu kviza:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuizDifficulty.values().forEach { difficulty ->
                    FilterChip(
                        selected = uiState.difficulty == difficulty,
                        onClick = { viewModel.onDifficultyChange(difficulty) },
                        label = { Text(difficulty.name) }
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privatan kviz", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ako je uključen, samo vi ćete videti ovaj kviz.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Switch(
                    checked = uiState.isPrivate,
                    onCheckedChange = viewModel::onPrivacyChange
                )
            }

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