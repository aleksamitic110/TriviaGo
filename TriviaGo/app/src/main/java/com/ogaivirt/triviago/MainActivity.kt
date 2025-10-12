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
import com.ogaivirt.triviago.ui.screens.home.HomeScreen
import com.ogaivirt.triviago.ui.screens.profile.ProfileScreen
import com.ogaivirt.triviago.ui.screens.splash.SplashScreen



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
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {

                        composable("splash") {
                            SplashScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(onNavigateToLogin = {
                                navController.navigate("login"){
                                    popUpTo("login") { inclusive = true }
                                }
                            })
                        }

                        composable("home") {
                            HomeScreen(onNavigateToProfile = { navController.navigate("profile") })
                        }

                        composable("profile") {
                            ProfileScreen(
                                onSignOut = {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}