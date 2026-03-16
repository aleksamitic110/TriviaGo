package com.ogaivirt.triviago.ui.screens.quiz_detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizDetailUiState(
    val isLoading: Boolean = true,
    val quiz: Quiz? = null,
    val errorMessage: String? = null,
    val isSubscribed: Boolean = false,
    val isCurrentUserTheCreator: Boolean = false
)

sealed class DeletionEvent {
    object QuizDeleted : DeletionEvent()
}

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val quizRepo: QuizRepository,
    private val authRepo: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = savedStateHandle.get<String>("quizId").orEmpty()


    private val _uiState = MutableStateFlow(QuizDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _deletionEvent = MutableSharedFlow<DeletionEvent>()
    val deletionEvent = _deletionEvent.asSharedFlow()


    init {
        if (quizId.isNotBlank()) {
            loadQuizDetails()
        }
    }

    fun loadQuizDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }


            val quizResult = quizRepo.getQuizById(quizId)
            val profileResult = authRepo.getUserProfile()

            if (quizResult.isSuccess && profileResult.isSuccess) {
                val quiz = quizResult.getOrNull()
                val userProfile = profileResult.getOrNull()

                if (quiz != null && userProfile != null) {
                    val currentUserId = userProfile.uid
                    val isCurrentUserSubscribed = quiz.subscriberIds.contains(currentUserId)

                    val isCreator = quiz.creatorId == currentUserId
                    val isAdmin = userProfile.roles.contains("ADMIN")
                    val hasPermission = isCreator || isAdmin


                    Log.d("QuizDetailDebug", "--------------------")
                    Log.d("QuizDetailDebug", "Trenutni korisnik ID: $currentUserId")
                    Log.d("QuizDetailDebug", "Kreator kviza ID:     ${quiz.creatorId}")
                    Log.d("QuizDetailDebug", "Da li je kreator? -> $isCreator")
                    Log.d("QuizDetailDebug", "Da li je admin? -> $isAdmin")
                    Log.d("QuizDetailDebug", "Ima dozvolu? -> $hasPermission")
                    Log.d("QuizDetailVerified", "STATUS VERIFIKACIJE: ${quiz.isCreatorVerified}")
                    Log.d("QuizDetailDebug", "--------------------")


                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            quiz = quiz,
                            isSubscribed = isCurrentUserSubscribed,
                            isCurrentUserTheCreator = hasPermission
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Kviz ili korisnik nije pronađen.") }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Greška pri učitavanju: ${quizResult.exceptionOrNull()?.localizedMessage ?: profileResult.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onSubscribeClick() {
        viewModelScope.launch {
            val currentState = uiState.value

            if (currentState.isSubscribed) {
                quizRepo.unsubscribeFromQuiz(quizId).onSuccess {
                    _uiState.update { it.copy(
                        isSubscribed = false,
                        errorMessage = "Uspešno ste se odjavili."
                    ) }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Greška: ${exception.localizedMessage}"
                        )
                    }
                }
            } else {
                quizRepo.subscribeToQuiz(quizId).onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubscribed = true,
                            errorMessage = "Uspešno ste se pretplatili!"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Greška: ${exception.localizedMessage}"
                        )
                    }
                }
            }
        }
    }
    fun onInfoMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onRateQuiz(rating: Int) {
        val currentUser = authRepo.getCurrentUser()

        if (currentUser == null) {
            _uiState.update { it.copy(errorMessage = "Greška: Korisnik nije ulogovan.") }
            return
        }

        val userId = currentUser.uid

        viewModelScope.launch {
            quizRepo.rateQuiz(quizId, userId, rating).onSuccess {
                loadQuizDetails()
                _uiState.update { it.copy(errorMessage = "Hvala na oceni!") }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "Greška pri ocenjivanju.") }
            }
        }
    }

    fun onDeleteQuizClick() {
        viewModelScope.launch {
            quizRepo.deleteQuiz(quizId).onSuccess {
                _uiState.update { it.copy(errorMessage = "Kviz uspešno obrisan.") }
                _deletionEvent.emit(DeletionEvent.QuizDeleted) // Emit event
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "Greška pri brisanju.") }
            }
        }
    }


    fun onReportQuiz(reason: String) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Razlog prijave ne sme biti prazan.") }
            return
        }

        viewModelScope.launch {
            quizRepo.reportQuiz(quizId, reason).onSuccess {
                _uiState.update { it.copy(errorMessage = "Kviz je uspešno prijavljen. Hvala vam.") }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "Došlo je do greške pri slanju prijave.") }
            }
        }
    }

    fun onDeleteQuestion(question: Question) {
        viewModelScope.launch {
            quizRepo.deleteQuestion(quizId, question).onSuccess {
                loadQuizDetails()
                _uiState.update { it.copy(errorMessage = "Pitanje obrisano.") }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "Greška pri brisanju pitanja.") }
            }
        }
    }
}