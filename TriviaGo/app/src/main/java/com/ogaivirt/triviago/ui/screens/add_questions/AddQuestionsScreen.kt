package com.ogaivirt.triviago.ui.screens.add_questions

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionsScreen(
    viewModel: AddQuestionsViewModel = hiltViewModel(),
    onFinishQuizCreation: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onInfoMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (uiState.isEditMode) {
                                onNavigateBack()
                            } else {
                                onFinishQuizCreation()
                            }
                        },
                        enabled = uiState.isEditMode || uiState.questionCount > 0
                    ) {
                        Text("Završi")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = if (uiState.isEditMode) "Ukupan broj pitanja: ${uiState.questionCount}" else "Dodato pitanja: ${uiState.questionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))


                OutlinedTextField(
                    value = uiState.questionText,
                    onValueChange = viewModel::onQuestionTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tekst pitanja") }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text("Unesi odgovore i označi tačan:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val answers = listOf(uiState.answer1, uiState.answer2, uiState.answer3, uiState.answer4)
                val onAnswerChanges = listOf(
                    viewModel::onAnswer1Change,
                    viewModel::onAnswer2Change,
                    viewModel::onAnswer3Change,
                    viewModel::onAnswer4Change
                )

                answers.forEachIndexed { index, answerText ->
                    AnswerInputRow(
                        answerText = answerText,
                        onValueChange = onAnswerChanges[index],
                        isSelected = uiState.correctAnswerIndex == index,
                        onSelect = { viewModel.onCorrectAnswerChange(index) },
                        label = "Odgovor ${index + 1}"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = viewModel::onSaveQuestionClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading)
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else
                        Text(if (uiState.isEditMode) "Sačuvaj izmene" else "Dodaj ovo pitanje")
                }

                if (!uiState.isEditMode) {
                    Text(
                        "Možeš dodati još pitanja nakon ovoga, ili kliknuti 'Završi' u gornjem uglu. (Trenutno: ${uiState.questionCount} pitanja)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun AnswerInputRow(
    answerText: String,
    onValueChange: (String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        OutlinedTextField(
            value = answerText,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true
        )
    }
}