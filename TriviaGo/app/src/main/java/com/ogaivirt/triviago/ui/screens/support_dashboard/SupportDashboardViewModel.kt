package com.ogaivirt.triviago.ui.screens.support_dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.NotificationType
import com.ogaivirt.triviago.domain.model.Report
import com.ogaivirt.triviago.domain.model.TicketAssignee
import com.ogaivirt.triviago.domain.model.VerificationRequest
import com.ogaivirt.triviago.domain.model.VerificationStatus
import com.ogaivirt.triviago.domain.repository.NotificationRepository
import com.ogaivirt.triviago.domain.repository.ReportRepository
import com.ogaivirt.triviago.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportDashboardUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val verificationRequests: List<VerificationRequest> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class SupportDashboardViewModel @Inject constructor(
    private val reportRepo: ReportRepository,
    private val verificationRepo: VerificationRepository,
    private val notificationRepo: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportDashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }


    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }


            val reportsDeferred = async { reportRepo.getOpenSupportReports() }
            val verificationsDeferred = async { verificationRepo.getPendingVerificationRequests() }

            val reportsResult = reportsDeferred.await()
            val verificationsResult = verificationsDeferred.await()


            val reportsList = reportsResult.getOrElse {
                _uiState.update { s -> s.copy(errorMessage = "Greška pri učitavanju prijava: ${it.localizedMessage}") }
                emptyList()
            }
            val verificationsList = verificationsResult.getOrElse {
                _uiState.update { s -> s.copy(errorMessage = "Greška pri učitavanju verifikacija: ${it.localizedMessage}") }
                emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    reports = reportsList,
                    verificationRequests = verificationsList,

                    errorMessage = if (reportsResult.isFailure || verificationsResult.isFailure) uiState.value.errorMessage else null
                )
            }
        }
    }


    fun onResolveClick(reportId: String) {
        viewModelScope.launch {
            reportRepo.deleteReport(reportId).onSuccess {
                _uiState.update { it.copy(infoMessage = "Prijava rešena (obrisana).") }
                loadDashboardData()
            }.onFailure { exception ->
                _uiState.update { it.copy(errorMessage = "Greška pri rešavanju prijave: ${exception.localizedMessage}") }
            }
        }
    }

    fun onEscalateToAdmin(report: Report) {
        viewModelScope.launch {
            reportRepo.createTicketFromReport(report, TicketAssignee.ADMIN).onSuccess {
                _uiState.update { it.copy(infoMessage = "Prijava eskalirana Adminu.") }
                loadDashboardData()
            }.onFailure { exception ->
                _uiState.update { it.copy(errorMessage = "Greška pri eskalaciji Adminu: ${exception.localizedMessage}") }
            }
        }
    }

    fun onEscalateToDev(report: Report, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("aleksamitic110@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Novi tiket za developera: Kviz ${report.quizId}")
                putExtra(Intent.EXTRA_TEXT, "Detalji prijave:\n\nID Prijave: ${report.reportId}\nKorisnik: ${report.reportedByUid}\nRazlog: ${report.reason}")
            }
            context.startActivity(intent)
            _uiState.update { it.copy(infoMessage = "Email klijent otvoren za slanje Developeru.") }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Greška pri otvaranju email klijenta.") }
        }
    }


    fun onApproveVerificationRequest(requestId: String) {
        updateRequestStatus(requestId, VerificationStatus.APPROVED)
    }

    fun onRejectVerificationRequest(requestId: String, userId: String, reason: String) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Razlog odbijanja ne sme biti prazan.") }
            return
        }
        updateRequestStatus(requestId, VerificationStatus.REJECTED, reason) { success ->
            if (success) {
                sendRejectionNotification(userId, reason)
            }
        }
    }

    private fun updateRequestStatus(
        requestId: String,
        status: VerificationStatus,
        reason: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            verificationRepo.updateVerificationStatus(requestId, status, reason).onSuccess {
                _uiState.update { it.copy(infoMessage = "Status zahteva ažuriran na ${status.name}.") }
                loadDashboardData()
                onComplete?.invoke(true)
            }.onFailure { exception ->
                _uiState.update { it.copy(errorMessage = "Greška pri ažuriranju statusa: ${exception.localizedMessage}") }
                onComplete?.invoke(false)
            }
        }
    }

    private fun sendRejectionNotification(userId: String, reason: String) {
        viewModelScope.launch {
            notificationRepo.createNotification(
                userId = userId,
                message = "Vaš zahtev za verifikaciju je odbijen. Razlog: $reason",
                type = NotificationType.GENERIC
            ).onFailure {
                _uiState.update { it.copy(errorMessage = "Status zahteva ažuriran, ali greška pri slanju notifikacije.") }
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