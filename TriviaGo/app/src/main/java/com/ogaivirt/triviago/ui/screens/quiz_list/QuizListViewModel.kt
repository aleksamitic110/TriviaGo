package com.ogaivirt.triviago.ui.screens.quiz_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ogaivirt.triviago.domain.model.QuizDifficulty
import com.ogaivirt.triviago.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SortType {
    BY_NAME,
    BY_RATING,
    BY_SUBSCRIBERS
}

data class QuizListUiState(
    val isLoading: Boolean = true,
    val allQuizzes: List<Quiz> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val sortType: SortType = SortType.BY_NAME,
    val difficultyFilters: Set<QuizDifficulty> = emptySet(),
    val showOnlyMyQuizzes: Boolean = false,
    val showInvalidKeyDialog: Boolean = false
)

sealed class NavigationEvent {
    object NavigateToMyQuizzes : NavigationEvent()
}

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val quizRepo: QuizRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizListUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    val sortedAndFilteredQuizzes: StateFlow<List<Quiz>> =
        combine(
            _uiState.map { it.allQuizzes },
            _uiState.map { it.searchQuery },
            _uiState.map { it.sortType },
            _uiState.map { it.difficultyFilters },
            _uiState.map { it.showOnlyMyQuizzes })
        { quizzes, query, sortType, difficultyFilters, showOnlyMy  ->


            val filtered = if (query.isBlank()) {
                quizzes
            } else {
                quizzes.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            }

            val filteredByDifficulty = if (difficultyFilters.isEmpty()) {
                filtered
            }else{
                filtered.filter { it.difficulty in difficultyFilters }
            }

            val currentUserId = authRepo.getCurrentUser()?.uid
            val filteredByOwnership = if (showOnlyMy && currentUserId != null) {
                filteredByDifficulty.filter { it.creatorId == currentUserId }
            } else {
                filteredByDifficulty
            }

            when (sortType){
                SortType.BY_NAME -> filteredByOwnership.sortedBy { it.name }
                SortType.BY_RATING -> filteredByOwnership.sortedByDescending { it.averageRating }
                SortType.BY_SUBSCRIBERS -> filteredByOwnership.sortedByDescending { it.subscriberIds.size }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun onShowOnlyMyQuizzesChange(isEnabled: Boolean) {
        _uiState.update { it.copy(showOnlyMyQuizzes = isEnabled) }
    }

    fun onSortTypeChange(newSortType: SortType) {
        _uiState.update { it.copy(sortType = newSortType) }
    }

    init {
        refreshQuizzes()
    }


    fun refreshQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            quizRepo.getAllPublicQuizzes().onSuccess { quizList ->
                _uiState.update { it.copy(isLoading = false, allQuizzes = quizList, errorMessage = null) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = exception.localizedMessage) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDifficultyFilterChange(difficulty: QuizDifficulty, isSelected: Boolean) {
        val currentFilters = _uiState.value.difficultyFilters.toMutableSet()
        if (isSelected) {
            currentFilters.add(difficulty)
        } else {
            currentFilters.remove(difficulty)
        }
        _uiState.update { it.copy(difficultyFilters = currentFilters) }
    }

    fun onJoinPrivateQuizClick(quizId: String) {
        if (quizId.isBlank()) return

        viewModelScope.launch {
            quizRepo.subscribeToQuiz(quizId).onSuccess {
                _navigationEvent.emit(NavigationEvent.NavigateToMyQuizzes)
            }.onFailure {
                _uiState.update { it.copy( showInvalidKeyDialog = true ) }
            }
        }
    }

    fun onInvalidKeyDialogDismiss() {
        _uiState.update { it.copy(showInvalidKeyDialog = false) }
    }
}