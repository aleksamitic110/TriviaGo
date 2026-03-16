package com.ogaivirt.triviago.ui.screens.quiz_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogaivirt.triviago.domain.model.Quiz
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ogaivirt.triviago.domain.model.QuizDifficulty
import com.ogaivirt.triviago.ui.components.QuizListItem
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry // Import NavBackStackEntry
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    viewModel: QuizListViewModel = hiltViewModel(),
    navBackStackEntry: NavBackStackEntry,
    onNavigateToQuizDetail: (quizId: String) -> Unit,
    onNavigateToMyQuizzes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortedAndFilteredQuizzes  by viewModel.sortedAndFilteredQuizzes.collectAsState()
    val (isSortMenuExpanded, setSortMenuExpanded) = remember { mutableStateOf(false) }


    var showJoinQuizDialog by remember { mutableStateOf(false) }
    var privateQuizId by remember { mutableStateOf("") }

    // Observe result from QuizDetailScreen
    LaunchedEffect(Unit) {
        val savedStateHandle = navBackStackEntry.savedStateHandle
        savedStateHandle.getStateFlow("quiz_deleted", false)
            .collectLatest { deleted ->
                if (deleted) {
                    viewModel.refreshQuizzes()
                    savedStateHandle["quiz_deleted"] = false
                }
            }
    }

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToMyQuizzes -> {
                    onNavigateToMyQuizzes()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Istraži kvizove") },
                actions = {

                    IconButton(onClick = { showJoinQuizDialog = true }) {
                        Icon(Icons.Default.VpnKey, "Pridruži se kodom")
                    }

                    IconButton(onClick = { setSortMenuExpanded(true) }) {
                        Icon(Icons.Default.Sort, "Sortiraj")
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { setSortMenuExpanded(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sortiraj po imenu") },
                            onClick = {
                                viewModel.onSortTypeChange(SortType.BY_NAME)
                                setSortMenuExpanded(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sortiraj po oceni") },
                            onClick = {
                                viewModel.onSortTypeChange(SortType.BY_RATING)
                                setSortMenuExpanded(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sortiraj po popularnosti") },
                            onClick = {
                                viewModel.onSortTypeChange(SortType.BY_SUBSCRIBERS)
                                setSortMenuExpanded(false)
                            }
                        )
                    }
                }
            ) }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Pretraži po imenu ili opisu") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pretraga") },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizDifficulty.values().forEach { difficulty ->
                    val isSelected = uiState.difficultyFilters.contains(difficulty)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onDifficultyFilterChange(difficulty, !isSelected) },
                        label = { Text(difficulty.name) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                FilterChip(
                    selected = uiState.showOnlyMyQuizzes,
                    onClick = { viewModel.onShowOnlyMyQuizzesChange(!uiState.showOnlyMyQuizzes) },
                    label = { Text("Moji") },
                    leadingIcon = { Icon(Icons.Default.Face, "Moji kvizovi") }
                )

            }

            if (showJoinQuizDialog) {
                AlertDialog(
                    onDismissRequest = { showJoinQuizDialog = false },
                    title = { Text("Pridruži se privatnom kvizu") },
                    text = {
                        OutlinedTextField(
                            value = privateQuizId,
                            onValueChange = { privateQuizId = it },
                            label = { Text("Unesi kod kviza") }
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.onJoinPrivateQuizClick(privateQuizId)
                                showJoinQuizDialog = false
                                privateQuizId = ""
                            }
                        ) {
                            Text("Pridruži se")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJoinQuizDialog = false }) {
                            Text("Otkaži")
                        }
                    }
                )
            }

            if (uiState.showInvalidKeyDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onInvalidKeyDialogDismiss() },
                    title = { Text("Greška") },
                    text = { Text("Uneti ključ nije ispravan ili kviz nije pronađen.") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.onInvalidKeyDialogDismiss() }
                        ) {
                            Text("U redu")
                        }
                    }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                println(uiState.errorMessage)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedAndFilteredQuizzes, key = { it.id }) { quiz ->
                        QuizListItem(
                            quiz = quiz,
                            modifier = Modifier.clickable { onNavigateToQuizDetail(quiz.id) })
                    }
                }
            }
        }
    }
}



@Composable
fun RatingBarIndicator(
    modifier: Modifier = Modifier,
    rating: Float,
    maxRating: Int = 5
) {
    val filledStars = rating.toInt()
    val hasHalfStar = rating - filledStars >= 0.5f

    Row(modifier = modifier) {
        repeat(filledStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
        }


        if (hasHalfStar && filledStars < maxRating) {
            Icon(
                imageVector = Icons.Default.StarHalf,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
        }

        val emptyStars = maxRating - filledStars - if (hasHalfStar) 1 else 0
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Default.StarOutline,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}