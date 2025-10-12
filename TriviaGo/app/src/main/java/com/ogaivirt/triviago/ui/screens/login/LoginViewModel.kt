package com.ogaivirt.triviago.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

data class LoginUiState(
    val email: String = "",
    val lozinka: String = "",
    val isPasswordVisible: Boolean = false,
    val infoMessage: String? = null,
    val emailError: String? = null,
    val lozinkaError: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _loginSuccessEvent = MutableSharedFlow<Unit>()
    val loginSuccessEvent = _loginSuccessEvent.asSharedFlow()

    fun onGoogleSignInClick(idToken: String?) {
        if (idToken == null) {
            _uiState.update { it.copy(infoMessage = "Greska: idToken null!") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.signInWithGoogle(idToken)
                .onSuccess {
                    _loginSuccessEvent.emit(Unit)
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(infoMessage = "Greška: ${exception.localizedMessage}") }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSignInClick() {

        val email = uiState.value.email
        val lozinka = uiState.value.lozinka

        _uiState.update { it.copy(emailError = null, lozinkaError = null) }

        val isEmailValid = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!isEmailValid) {
            _uiState.update { it.copy(emailError = "Unesite validnu email adresu.") }
        }

        val isLozinkaValid = lozinka.isNotBlank()
        if (!isLozinkaValid) {
            _uiState.update { it.copy(lozinkaError = "Unesite lozinku.") }
        }

        if (!isEmailValid || !isLozinkaValid) {
            return
        }


        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.signInWithEmailAndPassword(
                email = uiState.value.email,
                lozinka = uiState.value.lozinka
            ).onSuccess {
                _loginSuccessEvent.emit(Unit)
            }.onFailure { exception ->
                val errorMessage = "Pogrešan email ili lozinka. Molimo vas, pokušajte ponovo."
                _uiState.update { it.copy(infoMessage = errorMessage) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEmailChange(noviEmail: String) {
        _uiState.update { it.copy(email = noviEmail) }
    }

    fun onLozinkaChange(novaLozinka: String) {
        _uiState.update { it.copy(lozinka = novaLozinka) }
    }


    fun onPasswordVisibilityChange() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onInfoMessageShown() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}