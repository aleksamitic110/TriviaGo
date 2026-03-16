package com.ogaivirt.triviago.ui.screens.question

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogaivirt.triviago.domain.model.Answer
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(
    viewModel: QuestionViewModel = hiltViewModel(),
    onNavigateBack: (updatedStatistic: QuestionStatistic) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pitanje") }
            )
        }
    ) { padding ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.question == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pitanje nije pronađeno.")
                }
            }

            else -> {
                val question = uiState.question!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pitanje
                    item {
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Odgovori
                    itemsIndexed(
                        items = question.answers,
                        key = { index, answer -> answer.text }
                    ) { index, answer ->

                        val isSelected = uiState.selectedAnswerIndex == index
                        val answerState = uiState.answerState

                        val containerColor: Color
                        val contentColor: Color

                        when (answerState) {
                            AnswerState.UNANSWERED -> {
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            AnswerState.REVEALED -> {
                                containerColor = when {
                                    answer.isCorrect -> Color(0xFF4CAF50)
                                    isSelected && !answer.isCorrect -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                }
                                contentColor = Color.White
                            }
                        }

                        AnswerButton(
                            answerText = answer.text,
                            onClick = { viewModel.onAnswerSelected(index) },
                            containerColor = containerColor,
                            contentColor = contentColor,
                            enabled = answerState == AnswerState.UNANSWERED,
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))

                        if (uiState.answerState == AnswerState.UNANSWERED) {
                            Button(
                                onClick = viewModel::onConfirmAnswer,
                                enabled = uiState.selectedAnswerIndex != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Potvrdi odgovor")
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.answerState == AnswerState.REVEALED,
                            enter = fadeIn(animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(500))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Koliko dobro poznaješ ovo pitanje?")
                                Slider(
                                    value = uiState.knowledgeSliderValue,
                                    onValueChange = viewModel::onSliderValueChanged,
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                Text(uiState.knowledgeSliderValue.roundToInt().toString())

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.onFinishAndSave{newStats -> onNavigateBack(newStats)}
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                {
                                    Text("Dalje")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AnswerButton(
    answerText: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        enabled = enabled
    ) {
        Text(text = answerText)
    }
}