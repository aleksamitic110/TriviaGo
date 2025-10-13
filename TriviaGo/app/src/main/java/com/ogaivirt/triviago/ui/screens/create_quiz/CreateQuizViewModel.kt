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
import kotlinx.coroutines.launch
data class CreateQuizUiState(
    val quizName: String = "",
    val quizDescription: String = "",
    // TODO: Dodati stanje za kategoriju, privatnost, itd.
    val isLoading: Boolean = false,
    val quizCreatedId: String? = null
)

@HiltViewModel
class CreateQuizViewModel @Inject constructor(
    private val repo: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState = _uiState.asStateFlow()

    fun onQuizNameChange(name: String) {
        _uiState.update { it.copy(quizName = name) }
    }

    fun onQuizDescriptionChange(description: String) {
        _uiState.update { it.copy(quizDescription = description) }
    }

    fun onNextClick() {
        // TODO: Validacija

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val newQuiz = Quiz(
                name = uiState.value.quizName,
                description = uiState.value.quizDescription
            )

            repo.createQuiz(newQuiz).onSuccess { createdQuizId ->
                _uiState.update { it.copy(isLoading = false, quizCreatedId = createdQuizId) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, /* TODO: error poruka */) }
            }
        }
    }



    fun onNavigatedToNextStep() {
        _uiState.update { it.copy(quizCreatedId = null) }
    }
}