package com.ogaivirt.triviago.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.ogaivirt.triviago.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    fun isUserAuthenticated(): Boolean {
        return repo.isUserAuthenticated()
    }
}