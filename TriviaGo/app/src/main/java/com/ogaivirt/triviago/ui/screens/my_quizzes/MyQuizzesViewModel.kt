package com.ogaivirt.triviago.ui.screens.my_quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyQuizzesUiState(
    val isLoading: Boolean = true,
    val quizzes: List<Quiz> = emptyList(),
    val activeQuizIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val hasChanges: Boolean = false
)

@HiltViewModel
class MyQuizzesViewModel @Inject constructor(
    private val quizRepo: QuizRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyQuizzesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

     fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profileResult = authRepo.getUserProfile()
            val initiallyActiveIds = profileResult.getOrNull()?.activeQuizIds ?: emptyList()

            quizRepo.getMySubscribedQuizzes().onSuccess { quizList ->
                _uiState.update { it.copy(isLoading = false, quizzes = quizList, activeQuizIds = initiallyActiveIds.toSet()) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = exception.localizedMessage) }
            }
        }
    }

    fun onQuizActivationChanged(quizId: String, isActive: Boolean) {
        val currentActiveIds = _uiState.value.activeQuizIds.toMutableSet()
        if (isActive) {
            currentActiveIds.add(quizId)
        } else {
            currentActiveIds.remove(quizId)
        }
        _uiState.update { it.copy(activeQuizIds = currentActiveIds, hasChanges = true) }

    }

    fun onConfirmSelection() {
        viewModelScope.launch {
            authRepo.updateActiveQuizzes(_uiState.value.activeQuizIds.toList())
            _uiState.update { it.copy(hasChanges = false) }
        }
    }
}