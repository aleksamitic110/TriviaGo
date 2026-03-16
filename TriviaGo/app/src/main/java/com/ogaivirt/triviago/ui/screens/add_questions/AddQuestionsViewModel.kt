package com.ogaivirt.triviago.ui.screens.add_questions

import android.util.Log
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
import java.util.UUID

data class AddQuestionUiState(
    val questionText: String = "",
    val answer1: String = "",
    val answer2: String = "",
    val answer3: String = "",
    val answer4: String = "",
    val correctAnswerIndex: Int = 0,
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    val isEditMode: Boolean = false,
    val screenTitle: String = "Dodaj pitanje",
    val questionCount: Int = 0
)

@HiltViewModel
class AddQuestionsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repo: QuizRepository
) : ViewModel() {


    val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()
    private val questionId: String? = savedStateHandle.get<String>("questionId")
    private val isEditMode = questionId != null

    private var originalQuestion: Question? = null

    private val _uiState = MutableStateFlow(AddQuestionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (isEditMode) {
            _uiState.update { it.copy(isEditMode = true, screenTitle = "Izmeni pitanje") }
            loadQuestionData()
        } else {

            loadInitialQuestionCount()
        }
    }

    private fun loadInitialQuestionCount() {
        viewModelScope.launch {
            repo.getQuizById(quizId).onSuccess { quiz ->
                quiz?.let {
                    _uiState.update { it.copy(questionCount = quiz.questions.size) }
                }
            }
        }
    }


    private fun loadQuestionData() {
        if (questionId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.getQuizById(quizId).onSuccess { quiz ->
                val question = quiz?.questions?.find { it.id == questionId }
                if (question != null) {
                    originalQuestion = question
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questionText = question.text,
                            answer1 = question.answers.getOrElse(0) { Answer() }.text,
                            answer2 = question.answers.getOrElse(1) { Answer() }.text,
                            answer3 = question.answers.getOrElse(2) { Answer() }.text,
                            answer4 = question.answers.getOrElse(3) { Answer() }.text,
                            correctAnswerIndex = question.answers.indexOfFirst { it.isCorrect }.coerceAtLeast(0),
                            questionCount = quiz.questions.size
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, infoMessage = "Pitanje nije pronađeno.") }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, infoMessage = "Greška pri učitavanju pitanja.") }
            }
        }
    }


    fun onQuestionTextChange(text: String) { _uiState.update { it.copy(questionText = text) } }
    fun onAnswer1Change(text: String) { _uiState.update { it.copy(answer1 = text) } }
    fun onAnswer2Change(text: String) { _uiState.update { it.copy(answer2 = text) } }
    fun onAnswer3Change(text: String) { _uiState.update { it.copy(answer3 = text) } }
    fun onAnswer4Change(text: String) { _uiState.update { it.copy(answer4 = text) } }
    fun onCorrectAnswerChange(index: Int) { _uiState.update { it.copy(correctAnswerIndex = index) } }

    fun onSaveQuestionClick() {
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

        if (isEditMode) {
            runUpdateQuestion(currentState)
        } else {
            runAddQuestion(currentState)
        }
    }

    private fun runUpdateQuestion(currentState: AddQuestionUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val answers = listOf(
                Answer(text = currentState.answer1, isCorrect = currentState.correctAnswerIndex == 0),
                Answer(text = currentState.answer2, isCorrect = currentState.correctAnswerIndex == 1),
                Answer(text = currentState.answer3, isCorrect = currentState.correctAnswerIndex == 2),
                Answer(text = currentState.answer4, isCorrect = currentState.correctAnswerIndex == 3)
            )

            val updatedQuestion = Question(
                quizId = quizId,
                id = originalQuestion?.id ?: UUID.randomUUID().toString(),
                text = currentState.questionText,
                answers = answers
            )

            repo.updateQuestion(quizId, updatedQuestion).onSuccess {
                _uiState.update { it.copy(
                    infoMessage = "Pitanje uspešno ažurirano!",
                    isLoading = false
                )}
            }.onFailure { exception ->
                _uiState.update { it.copy(
                    infoMessage = "Greška: ${exception.localizedMessage}",
                    isLoading = false
                )}
            }
        }
    }

    private fun runAddQuestion(currentState: AddQuestionUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val answers = listOf(
                Answer(text = currentState.answer1, isCorrect = currentState.correctAnswerIndex == 0),
                Answer(text = currentState.answer2, isCorrect = currentState.correctAnswerIndex == 1),
                Answer(text = currentState.answer3, isCorrect = currentState.correctAnswerIndex == 2),
                Answer(text = currentState.answer4, isCorrect = currentState.correctAnswerIndex == 3)
            )

            val newQuestion = Question(
                quizId = quizId,
                id = UUID.randomUUID().toString(),
                text = currentState.questionText,
                answers = answers
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
                    correctAnswerIndex = 0,
                    questionCount = it.questionCount + 1
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