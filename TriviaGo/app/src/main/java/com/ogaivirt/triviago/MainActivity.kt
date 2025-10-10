package com.ogaivirt.triviago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ogaivirt.triviago.ui.screens.login.LoginScreen
import com.ogaivirt.triviago.ui.screens.register.RegisterScreen
import com.ogaivirt.triviago.ui.theme.TriviaGoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TriviaGoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController() // Kreiramo kontroler za navigaciju

                    NavHost(
                        navController = navController,
                        startDestination = "login" // Početni ekran je "login"
                    ) {
                        composable("login") {
                            LoginScreen() // Kada je ruta "login", prikaži LoginScreen
                        }
                        composable("register") {
                            RegisterScreen() // Kada je ruta "register", prikaži RegisterScreen
                        }
                    }
                }
            }
        }
    }
}