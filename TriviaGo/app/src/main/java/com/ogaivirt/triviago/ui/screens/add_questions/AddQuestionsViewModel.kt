package com.ogaivirt.triviago.ui.screens.add_questions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Answer
import com.ogaivirt.triviago.domain.model.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.ogaivirt.triviago.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddQuestionUiState(
    val questionText: String = "",
    val answer1: String = "",
    val answer2: String = "",
    val answer3: String = "",
    val answer4: String = "",
    val correctAnswerIndex: Int = 0, // 0=prvi, 1=drugi, 2=treci, 3=cetvrti
    val isLoading: Boolean = false,
    val infoMessage: String? = null
)

@HiltViewModel
class AddQuestionsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repo: QuizRepository
) : ViewModel() {


    val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()
    private val _uiState = MutableStateFlow(AddQuestionUiState())
    val uiState = _uiState.asStateFlow()

    fun onQuestionTextChange(text: String) { _uiState.update { it.copy(questionText = text) } }
    fun onAnswer1Change(text: String) { _uiState.update { it.copy(answer1 = text) } }
    fun onAnswer2Change(text: String) { _uiState.update { it.copy(answer2 = text) } }
    fun onAnswer3Change(text: String) { _uiState.update { it.copy(answer3 = text) } }
    fun onAnswer4Change(text: String) { _uiState.update { it.copy(answer4 = text) } }
    fun onCorrectAnswerChange(index: Int) { _uiState.update { it.copy(correctAnswerIndex = index) } }

    fun onAddQuestionClick() {
        val currentState = uiState.value
        if (currentState.questionText.isBlank() ||
            currentState.answer1.isBlank() ||
            currentState.answer2.isBlank() ||
            currentState.answer3.isBlank() ||
            currentState.answer4.isBlank()
        ) {
            _uiState.update { it.copy(infoMessage = "Sva polja moraju biti popunjena.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val answers = listOf(
                Answer(
                    text = currentState.answer1,
                    isCorrect = currentState.correctAnswerIndex == 0
                ),
                Answer(text = currentState.answer2, isCorrect = currentState.correctAnswerIndex == 1),
                Answer(text = currentState.answer3, isCorrect = currentState.correctAnswerIndex == 2),
                Answer(text = currentState.answer4, isCorrect = currentState.correctAnswerIndex == 3)
            )

            val newQuestion = Question(
                text = currentState.questionText,
                answers = answers
                // TODO: Dodati logiku za unos lokacije
            )


            repo.addQuestionToQuiz(quizId, newQuestion).onSuccess {
                _uiState.update { it.copy(
                    infoMessage = "Pitanje uspešno dodato!",
                    isLoading = false,
                    questionText = "",
                    answer1 = "",
                    answer2 = "",
                    answer3 = "",
                    answer4 = "",
                    correctAnswerIndex = 0
                ) }
            }.onFailure { exception ->
                _uiState.update { it.copy(
                    infoMessage = "Greška: ${exception.localizedMessage}",
                    isLoading = false
                ) }
            }
        }
    }

    fun onInfoMessageShown() {
        _uiState.update { it.copy(infoMessage = null) }
    }

}