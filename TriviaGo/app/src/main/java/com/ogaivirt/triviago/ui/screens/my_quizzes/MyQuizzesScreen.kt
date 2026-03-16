package com.ogaivirt.triviago.ui.screens.my_quizzes

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.ogaivirt.triviago.ui.components.MyQuizListItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuizzesScreen(
    viewModel: MyQuizzesViewModel = hiltViewModel(),
    onConfirmAndNavigateBack: () -> Unit,
    onNavigateToQuizDetail: (quizId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasConfirmed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val hasChanges = uiState.quizzes.any { !uiState.activeQuizIds.contains(it.id) } ||
            uiState.quizzes.size != uiState.activeQuizIds.size

    BackHandler(enabled = hasChanges && !hasConfirmed) {
        Toast.makeText(
            context,
            "Napravili ste promene - Osvezite mapu",
            Toast.LENGTH_SHORT
        ).show()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Scaffold(
        topBar = { TopAppBar(title = { Text("Moji Kvizovi") }) },
        floatingActionButton = {
            PulsatingMapButton(
                hasChanges = uiState.hasChanges,
                onClick = {
                    viewModel.onConfirmSelection()
                    hasConfirmed = true
                    onConfirmAndNavigateBack()
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.quizzes) { quiz ->
                MyQuizListItem(
                    quiz = quiz,
                    isActive = uiState.activeQuizIds.contains(quiz.id),
                    onActivationChanged = { isActive ->
                        viewModel.onQuizActivationChanged(quiz.id, isActive)
                    },
                    modifier = Modifier.clickable { onNavigateToQuizDetail(quiz.id) }
                )
            }
        }
    }
}



@Composable
fun PulsatingMapButton(
    hasChanges: Boolean,
    onClick: () -> Unit,
    text: String = "Osveži mapu"
) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by if (hasChanges) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { Icon(Icons.Default.Map, contentDescription = "Mapa") },
        onClick = onClick,
        modifier = Modifier.scale(scale)
    )
}