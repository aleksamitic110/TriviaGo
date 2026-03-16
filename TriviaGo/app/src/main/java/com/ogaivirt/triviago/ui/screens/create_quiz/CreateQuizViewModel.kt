package com.ogaivirt.triviago.ui.screens.create_quiz

import androidx.lifecycle.ViewModel
import com.ogaivirt.triviago.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.model.QuizDifficulty
import kotlinx.coroutines.launch
data class CreateQuizUiState(
    val quizName: String = "",
    val quizDescription: String = "",
    val difficulty: QuizDifficulty = QuizDifficulty.SREDNJE,
    val isLoading: Boolean = false,
    val quizCreatedId: String? = null,
    val isPrivate: Boolean = false,
    val quizNameError: String? = null,
    val quizDescriptionError: String? = null
)

@HiltViewModel
class CreateQuizViewModel @Inject constructor(
    private val repo: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState = _uiState.asStateFlow()


    fun onPrivacyChange(isPrivate: Boolean) {
        _uiState.update { it.copy(isPrivate = isPrivate) }
    }

    fun onQuizNameChange(name: String) {
        _uiState.update { it.copy(quizName = name) }
    }

    fun onQuizDescriptionChange(description: String) {
        _uiState.update { it.copy(quizDescription = description) }
    }

    fun onNextClick() {
        val state = uiState.value

        _uiState.update { it.copy(quizNameError = null, quizDescriptionError = null) }

        val isNameValid = state.quizName.isNotBlank()
        if (!isNameValid) {
            _uiState.update { it.copy(quizNameError = "Naziv kviza ne sme biti prazan.") }
        }

        val isDescriptionValid = state.quizDescription.isNotBlank()
        if (!isDescriptionValid) {
            _uiState.update { it.copy(quizDescriptionError = "Opis kviza ne sme biti prazan.") }
        }

        if (!isNameValid || !isDescriptionValid) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val newQuiz = Quiz(
                name = uiState.value.quizName,
                description = uiState.value.quizDescription,
                difficulty = uiState.value.difficulty,
                isPrivate = uiState.value.isPrivate
            )

            repo.createQuiz(newQuiz).onSuccess { createdQuizId ->
                _uiState.update { it.copy(isLoading = false, quizCreatedId = createdQuizId) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false,) }
            }
        }
    }


    fun onDifficultyChange(difficulty: QuizDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onNavigatedToNextStep() {
        _uiState.update { it.copy(quizCreatedId = null) }
    }
}