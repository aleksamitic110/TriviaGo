package com.ogaivirt.triviago.ui.screens.splash


import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ogaivirt.triviago.R

@Composable
fun AnimatedSplashScreen(
    onAnimationEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        val lottieFile = R.raw.splash_animation

        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieFile))


        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 2,
            speed = 0.85f
        )


        if (progress == 1f) {
            onAnimationEnd()
        }


        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(1f)
        )
    }
}