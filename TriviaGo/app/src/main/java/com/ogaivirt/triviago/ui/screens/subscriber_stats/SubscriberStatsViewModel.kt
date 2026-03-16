package com.ogaivirt.triviago.ui.screens.subscriber_stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriberStatsUiState(
    val isLoading: Boolean = true,
    val statistics: List<QuestionStatistic> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class SubscriberStatsViewModel @Inject constructor(
    private val statsRepo: StatisticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()

    private val _uiState = MutableStateFlow(SubscriberStatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (quizId.isNotBlank()) {
            loadStatistics()
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            statsRepo.getStatisticsForQuiz(quizId).onSuccess { statsList ->
                _uiState.update { it.copy(isLoading = false, statistics = statsList) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = exception.localizedMessage) }
            }
        }
    }
}