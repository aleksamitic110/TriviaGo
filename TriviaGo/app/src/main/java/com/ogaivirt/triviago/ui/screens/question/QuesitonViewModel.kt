package com.ogaivirt.triviago.ui.screens.question

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class AnswerState{
    UNANSWERED,
    REVEALED
}


data class QuestionUiState(
    val question: Question? = null,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val selectedAnswerIndex: Int? = null,
    val knowledgeSliderValue: Float = 5f,
    val isLoading: Boolean = true
)

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val quizRepo: QuizRepository,
    private val statsRepo: StatisticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()
    private val questionId: String = savedStateHandle.get<String>("questionId").orEmpty()

    private val _uiState = MutableStateFlow(QuestionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadQuestion()
    }

    private fun loadQuestion() {
        if (quizId.isBlank() || questionId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            quizRepo.getQuizById(quizId).onSuccess { quiz ->
                val question = quiz?.questions?.find { it.id == questionId }
                if (question != null) {
                    _uiState.update { it.copy(isLoading = false, question = question) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAnswerSelected(index: Int) {
        if (_uiState.value.answerState == AnswerState.UNANSWERED) {
            _uiState.update { it.copy(selectedAnswerIndex = index) }
        }
    }

    fun onConfirmAnswer() {
        if (_uiState.value.selectedAnswerIndex != null) {
            _uiState.update { it.copy(answerState = AnswerState.REVEALED) }
        }
    }


    fun onSliderValueChanged(newValue: Float) {
        _uiState.update { it.copy(knowledgeSliderValue = newValue) }
    }

    fun onFinishAndSave(onResult: (QuestionStatistic) -> Unit) {

        val selectedIndex = uiState.value.selectedAnswerIndex ?: return
        val question = uiState.value.question ?: return

        val wasCorrect = question.answers[selectedIndex].isCorrect
        val quality = (uiState.value.knowledgeSliderValue / 2).roundToInt()

        viewModelScope.launch {

            val oldStats = statsRepo.getStatisticForQuestion(quizId, questionId).getOrNull()
                ?: QuestionStatistic(questionId = questionId, quizId = quizId)

            val newRepetitions: Int
            val newInterval: Int
            var newEasinessFactor: Float = oldStats.easinessFactor

            if (quality < 3 || !wasCorrect) {
                newRepetitions = 0
                newInterval = 1
            } else {
                newRepetitions = oldStats.repetitions + 1
                newEasinessFactor = (oldStats.easinessFactor + (0.1 - (5 - (quality / 2f)) * (0.08 + (5 - (quality / 2f)) * 0.02))).toFloat()
                if (newEasinessFactor < 1.3f) newEasinessFactor = 1.3f

                newInterval = when (newRepetitions) {
                    1 -> 1
                    2 -> 6
                    else -> (oldStats.interval * newEasinessFactor).roundToInt()
                }
            }

            val nextDueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(newInterval.toLong())

            val newStats = oldStats.copy(
                repetitions = newRepetitions,
                easinessFactor = newEasinessFactor,
                interval = newInterval,
                nextDueDate = nextDueDate
            )

            statsRepo.saveQuestionStatistic(quizId, newStats).onSuccess {
                println("Statistika uspešno ažurirana: $newStats")
                onResult(newStats)
            }.onFailure {
                println("Greška pri ažuriranju statistike: ${it.message}")
            }

        }

    }



}