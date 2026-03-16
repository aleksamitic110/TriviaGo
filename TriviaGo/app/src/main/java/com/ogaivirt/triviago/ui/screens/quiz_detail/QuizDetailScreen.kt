package com.ogaivirt.triviago.ui.screens.quiz_detail

import android.widget.RatingBar
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Import Shape
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import kotlin.math.ceil
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ogaivirt.triviago.domain.model.QuizDifficulty
import kotlinx.coroutines.flow.collectLatest

// Helper function for difficulty color (copied from QuizComponents)
@Composable
fun difficultyColor(difficulty: QuizDifficulty): Color {
    return when (difficulty) {
        QuizDifficulty.LAKO -> Color(0xFF4CAF50)
        QuizDifficulty.SREDNJE -> Color(0xFFFFC107)
        QuizDifficulty.TESKO -> MaterialTheme.colorScheme.error
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    viewModel: QuizDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateBackAfterDelete: () -> Unit,
    onNavigateToQuestionReview: (quizId: String) -> Unit,
    onNavigateToMyStats: (quizId: String) -> Unit,
    onNavigateToCreatorStats: (quizId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onInfoMessageShown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.deletionEvent.collectLatest { event ->
            when (event) {
                is DeletionEvent.QuizDeleted -> {
                    onNavigateBackAfterDelete()
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(uiState.quiz?.name ?: "Učitavanje...") },
            actions = {
                IconButton(onClick = { showReportDialog = true }) {
                    Icon(Icons.Default.Flag, "Prijavi kviz")
                }
            }
        ) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.quiz != null) {
            val quiz = uiState.quiz!!
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {


                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(quiz.name, style = MaterialTheme.typography.headlineLarge)
                    if (quiz.isCreatorVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "VERIFIED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Autor: ${quiz.creatorName}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Row for Difficulty Label and Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Težina: ", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = difficultyColor(quiz.difficulty)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = quiz.difficulty.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(quiz.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Broj pitanja: ${quiz.questions.size}")
                Text("Broj pretplatnika: ${quiz.subscriberIds.size}")

                Spacer(modifier = Modifier.height(16.dp))
                RatingBar(
                    currentRating = quiz.averageRating,
                    onRatingSelected = { newRating ->
                        viewModel.onRateQuiz(newRating)
                    }
                )

                if (uiState.isCurrentUserTheCreator) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onNavigateToCreatorStats(quiz.id) }) {
                        Text("Statistika Kviza")
                    }
                }

                if (uiState.isCurrentUserTheCreator && quiz.isPrivate) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Privatni kod za deljenje:", style = MaterialTheme.typography.titleMedium)

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = quiz.id, // Use quiz directly
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(quiz.id))
                                Toast.makeText(context, "Kod kopiran!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kopiraj kod")
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Pridruži mi se u TriviaGO! Kod za kviz '${quiz.name}' je: ${quiz.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Podeli")
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onNavigateToQuestionReview(quiz.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pregledaj pitanja i odgovore")
                }

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.isSubscribed) {
                    Button(
                        onClick = { onNavigateToMyStats(quiz.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Moja statistika")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = viewModel::onSubscribeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (uiState.isSubscribed) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ){
                    Text(if (uiState.isSubscribed) "Odjavi se sa kviza" else "Pretplati se")
                }

                if (uiState.isCurrentUserTheCreator) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.onDeleteQuizClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Obriši ovaj kviz")
                    }
                }

            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Kviz nije pronađen ili je došlo do greške.")
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Prijavi kviz") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Razlog prijave") },
                    placeholder = { Text("Npr. neprimeren sadržaj, netačna pitanja...") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onReportQuiz(reportReason)
                        showReportDialog = false
                        reportReason = ""
                    }
                ) {
                    Text("Pošalji prijavu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Otkaži")
                }
            }
        )
    }
}

@Composable
fun RatingBar(
    maxRating: Int = 5,
    currentRating: Float,
    onRatingSelected: (Int) -> Unit
) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onRatingSelected(i) },
                tint = if (i <= currentRating) Color(0xFFFFC107) else Color.Gray
            )
        }
    }
}