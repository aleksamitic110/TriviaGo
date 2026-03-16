package com.ogaivirt.triviago.ui.screens.admin_dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.NotificationType
import com.ogaivirt.triviago.domain.model.Ticket
import com.ogaivirt.triviago.domain.model.VerificationRequest
import com.ogaivirt.triviago.domain.model.VerificationStatus
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.NotificationRepository
import com.ogaivirt.triviago.domain.repository.QuizRepository
import com.ogaivirt.triviago.domain.repository.TicketRepository
import com.ogaivirt.triviago.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val tickets: List<Ticket> = emptyList(),
    val approvedVerificationRequests: List<VerificationRequest> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val ticketRepo: TicketRepository,
    private val authRepo: AuthRepository,
    private val quizRepo: QuizRepository,
    private val notificationRepo: NotificationRepository,
    private val verificationRepo: VerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val ticketsDeferred = async { ticketRepo.getOpenAdminTickets() }
            val verificationsDeferred = async { verificationRepo.getApprovedVerificationRequests() }

            val ticketsResult = ticketsDeferred.await()
            val verificationsResult = verificationsDeferred.await()

            val tickets = ticketsResult.getOrElse { emptyList() }
            val verifications = verificationsResult.getOrElse { emptyList() }


            val errorMsg = ticketsResult.exceptionOrNull()?.localizedMessage ?: verificationsResult.exceptionOrNull()?.localizedMessage

            _uiState.update {
                it.copy(
                    isLoading = false,
                    tickets = tickets,
                    approvedVerificationRequests = verifications,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private fun refreshDashboard() = loadDashboardData()

    fun onResolveTicketClick(ticketId: String) {
        viewModelScope.launch {
            ticketRepo.closeTicket(ticketId).onSuccess {
                _uiState.update { it.copy(infoMessage = "Tiket zatvoren.") }
                refreshDashboard()
            }.onFailure { exception ->
                _uiState.update { it.copy(errorMessage = "Greška pri zatvaranju tiketa: ${exception.localizedMessage}") }
            }
        }
    }

    fun onDisableCreatorClick(quizId: String, ticketId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            quizRepo.getQuizById(quizId).onSuccess { quiz ->
                val creatorId = quiz?.creatorId
                if (quiz == null || creatorId == null || creatorId.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Nije moguće pronaći kreatora za kviz ID: $quizId") }
                    return@onSuccess
                }

                authRepo.removeRole(creatorId, "CREATOR").onSuccess {
                    quizRepo.deleteAllQuizzesByCreator(creatorId).onSuccess {
                        notificationRepo.createNotification(
                            userId = creatorId,
                            message = "Vaša mogućnost kreiranja kvizova je onemogućena od strane administratora.",
                            type = NotificationType.CREATOR_BAN
                        ).onSuccess {
                            ticketRepo.closeTicket(ticketId).onSuccess {
                                refreshDashboard()
                                _uiState.update { it.copy(infoMessage = "Korisniku ($creatorId) oduzeta rola, kvizovi obrisani, notifikacija poslata i tiket zatvoren.") }
                            }.onFailure { ticketException ->
                                _uiState.update { it.copy(isLoading = false, errorMessage = "Rola/kvizovi uklonjeni, notifikacija poslata, ali greška pri zatvaranju tiketa: ${ticketException.localizedMessage}") }
                            }
                        }.onFailure { notificationException ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = "Rola/kvizovi uklonjeni, ali greška pri slanju notifikacije: ${notificationException.localizedMessage}") }
                        }
                    }.onFailure { quizException ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Rola uklonjena, ali greška pri brisanju kvizova: ${quizException.localizedMessage}") }
                    }
                }.onFailure { roleException ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Greška pri oduzimanju role kreatoru ($creatorId): ${roleException.localizedMessage}") }
                }

            }.onFailure { getQuizException ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Greška pri dobavljanju detalja kviza: ${getQuizException.localizedMessage}") }
            }
        }
    }



    fun onFinalVerifyClick(userId: String, requestId: String, username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }


            authRepo.addRole(userId, "VERIFIED").onSuccess {

                quizRepo.setVerifiedStatusForCreatorQuizzes(userId, true).onSuccess {

                    verificationRepo.updateVerificationStatus(requestId, VerificationStatus.VERIFIED).onSuccess {

                        notificationRepo.createNotification(
                            userId = userId,
                            message = "Čestitamo! Vaš nalog je verifikovan. Sada imate plavi checkmark na svim vašim kvizovima.",
                            type = NotificationType.GENERIC
                        )

                        refreshDashboard()
                        _uiState.update { it.copy(infoMessage = "Korisnik $username uspešno VERIFIKOVAN.", isLoading = false) }
                    }.onFailure { exception ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Verifikacija uspela, ali greška pri zatvaranju zahteva: ${exception.localizedMessage}") }
                    }
                }.onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Verifikacija uspela, ali greška pri ažuriranju kvizova: ${exception.localizedMessage}") }
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Greška pri dodavanju VERIFIED role: ${exception.localizedMessage}") }
            }
        }
    }

    fun onFinalRejectClick(requestId: String, userId: String, username: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }


            verificationRepo.updateVerificationStatus(requestId, VerificationStatus.REJECTED, reason).onSuccess {

                notificationRepo.createNotification(
                    userId = userId,
                    message = "Vaš zahtev za verifikaciju je odbijen od strane Admina. Razlog: $reason",
                    type = NotificationType.GENERIC
                )

                refreshDashboard()
                _uiState.update { it.copy(infoMessage = "Zahtev korisnika $username je ODBIJEN.", isLoading = false) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Greška pri odbijanju zahteva: ${exception.localizedMessage}") }
            }
        }
    }

    fun onInfoMessageShown() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}