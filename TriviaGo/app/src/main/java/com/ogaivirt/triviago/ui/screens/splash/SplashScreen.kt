package com.ogaivirt.triviago.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {

    val isUserAuthenticated = remember { viewModel.isUserAuthenticated() }


    val animationFinished = remember { mutableStateOf(false) }

    AnimatedSplashScreen(
        onAnimationEnd = {
            animationFinished.value = true
        }
    )


    LaunchedEffect(animationFinished.value) {
        if (animationFinished.value) {
            if (isUserAuthenticated) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }
}