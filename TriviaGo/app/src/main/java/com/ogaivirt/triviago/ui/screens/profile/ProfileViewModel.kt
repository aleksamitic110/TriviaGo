package com.ogaivirt.triviago.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.model.UserProfile
import com.ogaivirt.triviago.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ogaivirt.triviago.domain.repository.QuizRepository
data class ProfileUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val createdQuizzesCount: Int = 0,
    val subscribedQuizzesCount: Int = 0

)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val quizRepo: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profileResult = authRepo.getUserProfile()
            val createdCountResult = quizRepo.getCreatedQuizzesCount()


            profileResult.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userProfile = profile,
                        subscribedQuizzesCount = profile?.subscribedQuizIds?.size ?: 0
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = exception.localizedMessage)
                }
            }

            createdCountResult.onSuccess { count ->
                _uiState.update { it.copy(createdQuizzesCount = count) }
            }
        }
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.update {
            it.copy(userProfile = it.userProfile?.copy(username = newUsername))
        }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update {
            it.copy(userProfile = it.userProfile?.copy(description = newDescription))
        }
    }

    fun onSaveChanges() {
        val profile = uiState.value.userProfile ?: return
        viewModelScope.launch {
            authRepo.updateUserProfile(profile.username, profile.description ?: "").onSuccess {
                _uiState.update { it.copy(infoMessage = "Podaci uspešno sačuvani!") }
            }.onFailure { exception ->
                _uiState.update { it.copy(infoMessage = "Greška: ${exception.localizedMessage}") }
            }
        }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            authRepo.signOut()
        }
    }


    fun onProfilePictureChange(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepo.uploadProfilePicture(uri).onSuccess { downloadUrl ->

                authRepo.updateProfilePictureUrl(downloadUrl).onSuccess {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = it.userProfile?.copy(profilePictureUrl = downloadUrl)
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update { it.copy(infoMessage = "Greška: ${exception.localizedMessage}") }
                }.onFailure { exception ->
                    _uiState.update { it.copy(infoMessage = "Greška: ${exception.localizedMessage}") }
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onInfoMessageShown() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}