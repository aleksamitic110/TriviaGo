package com.ogaivirt.triviago.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogaivirt.triviago.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val ime: String = "",
    val email: String = "",
    val lozinka: String = "",
    val potvrdaLozinke: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onSignUpClick() {

        if (uiState.value.lozinka != uiState.value.potvrdaLozinke) {
            _uiState.update { it.copy(errorMessage = "Lozinke se ne poklapaju!") }
            return
        }
        if (uiState.value.lozinka.length < 6) {
            _uiState.update { it.copy(errorMessage = "Lozinka mora imati najmanje 6 karaktera.") }
            return
        }

        viewModelScope.launch {
            repo.createUserWithEmailAndPassword(
                ime = uiState.value.ime,
                email = uiState.value.email,
                lozinka = uiState.value.lozinka
            ).onSuccess { _uiState.update { it.copy(registrationSuccess = true) } }
                .onFailure { exception -> _uiState.update { it.copy(errorMessage = "Greska: ${exception.localizedMessage}") }}
        }
    }
    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onImeChange(novoIme: String) {
        _uiState.update { it.copy(ime = novoIme) }
    }

    fun onEmailChange(noviEmail: String) {
        _uiState.update { it.copy(email = noviEmail) }
    }

    fun onLozinkaChange(novaLozinka: String) {
        _uiState.update { it.copy(lozinka = novaLozinka) }
    }

    fun onPotvrdaLozinkeChange(novaPotvrda: String) {
        _uiState.update { it.copy(potvrdaLozinke = novaPotvrda) }
    }

    fun onPasswordVisibilityChange() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onConfirmPasswordVisibilityChange() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }
}