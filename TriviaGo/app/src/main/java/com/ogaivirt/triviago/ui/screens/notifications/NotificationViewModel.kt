package com.ogaivirt.triviago.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.Notification
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = true,
    val notifications: List<Notification> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepo: NotificationRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {

        if (!authRepo.isUserAuthenticated()) {
            _uiState.update { it.copy(isLoading = false, notifications = emptyList(), errorMessage = "Morate biti ulogovani da vidite obaveštenja.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            notificationRepo.getNotificationsForCurrentUser().onSuccess { notificationList ->
                _uiState.update { it.copy(isLoading = false, notifications = notificationList, errorMessage = null) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "Greška pri učitavanju obaveštenja: ${exception.localizedMessage}") }
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepo.markNotificationAsRead(notificationId).onSuccess {

                _uiState.update { currentState ->
                    val updatedList = currentState.notifications.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                    currentState.copy(notifications = updatedList)
                }

                // loadNotifications()
            }.onFailure { exception ->
                _uiState.update { it.copy(errorMessage = "Greška pri označavanju kao pročitano: ${exception.localizedMessage}") }
            }
        }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}