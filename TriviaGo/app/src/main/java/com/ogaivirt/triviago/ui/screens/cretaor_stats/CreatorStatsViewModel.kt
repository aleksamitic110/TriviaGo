package com.ogaivirt.triviago.ui.screens.creator_stats

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.repository.QuizRepository
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AggregatedQuestionStat(
    val questionId: String,
    val questionText: String,
    val averageEasinessFactor: Float,
    val totalAttempts: Int
)

data class CreatorStatsUiState(
    val isLoading: Boolean = true,
    val aggregatedStats: List<AggregatedQuestionStat> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CreatorStatsViewModel @Inject constructor(
    private val statsRepo: StatisticsRepository,
    private val quizRepo: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()

    private val _uiState = MutableStateFlow(CreatorStatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (quizId.isNotBlank()) {
            loadAggregatedStats()
        }
    }

    private fun loadAggregatedStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val allStatsResult = statsRepo.getAllStatisticsForQuiz(quizId)
                val quizResult = quizRepo.getQuizById(quizId)

                if (allStatsResult.isSuccess && quizResult.isSuccess) {
                    val allStats = allStatsResult.getOrThrow() ?: emptyList()
                    val quiz = quizResult.getOrThrow()

                    if (quiz == null) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Kviz nije pronađen.") }
                        return@launch
                    }

                    val statsByQuestionId = allStats.groupBy { it.questionId }

                    val aggregatedList = quiz.questions.map { question ->
                        val statsForThisQuestion = statsByQuestionId[question.id] ?: emptyList()

                        val averageEF = if (statsForThisQuestion.isNotEmpty()) {
                            statsForThisQuestion.map { it.easinessFactor }.average().toFloat()
                        } else {
                            0f
                        }

                        AggregatedQuestionStat(
                            questionId = question.id,
                            questionText = question.text,
                            averageEasinessFactor = averageEF,
                            totalAttempts = statsForThisQuestion.size
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, aggregatedStats = aggregatedList) }

                } else {

                    val exception = allStatsResult.exceptionOrNull() ?: quizResult.exceptionOrNull()
                    Log.e("FirestoreIndex", "Greška pri čitanju: ${exception?.localizedMessage}", exception)


                    if (exception is com.google.firebase.firestore.FirebaseFirestoreException) {
                        Log.e("FirestoreIndex", "🔥 Firestore Index link: ${exception.message}")
                    }

                    _uiState.update { it.copy(isLoading = false, errorMessage = exception?.localizedMessage) }
                }
            } catch (e: Exception) {
                Log.e("FirestoreIndex", "Neočekivana greška: ${e.localizedMessage}", e)
                if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                    Log.e("FirestoreIndex", "🔥 Firestore Index link: ${e.message}")
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}