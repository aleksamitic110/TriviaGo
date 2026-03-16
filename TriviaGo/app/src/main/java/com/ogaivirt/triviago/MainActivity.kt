package com.ogaivirt.triviago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ogaivirt.triviago.ui.screens.add_questions.AddQuestionsScreen
import com.ogaivirt.triviago.ui.screens.admin_dashboard.AdminDashboardScreen
import com.ogaivirt.triviago.ui.screens.create_quiz.CreateQuizScreen
import com.ogaivirt.triviago.ui.screens.creator_stats.CreatorStatsScreen
import com.ogaivirt.triviago.ui.screens.login.LoginScreen
import com.ogaivirt.triviago.ui.screens.notifications.NotificationScreen
import com.ogaivirt.triviago.ui.screens.register.RegisterScreen
import com.ogaivirt.triviago.ui.theme.TriviaGoTheme
import dagger.hilt.android.AndroidEntryPoint
import com.ogaivirt.triviago.ui.screens.home.HomeScreen
import com.ogaivirt.triviago.ui.screens.my_quizzes.MyQuizzesScreen
import com.ogaivirt.triviago.ui.screens.profile.ProfileScreen
import com.ogaivirt.triviago.ui.screens.question.QuestionScreen
import com.ogaivirt.triviago.ui.screens.quiz_detail.QuizDetailScreen
import com.ogaivirt.triviago.ui.screens.quiz_list.QuizListScreen
import com.ogaivirt.triviago.ui.screens.splash.SplashScreen
import com.ogaivirt.triviago.ui.screens.support_dashboard.SupportDashboardScreen
import com.ogaivirt.triviago.ui.settings.SettingsScreen
import com.ogaivirt.triviago.ui.screens.question_review.QuestionReviewScreen
import com.ogaivirt.triviago.ui.screens.subscriber_stats.SubscriberStatsScreen


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

                        composable(
                            route = "subscriber_stats/{quizId}",
                            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                        ) {
                            SubscriberStatsScreen()
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
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                }
                            })
                        }

                        composable("home") { backStackEntry ->
                            HomeScreen(
                                navBackStackEntry = backStackEntry,
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToCreateQuiz = { navController.navigate("create_quiz") },
                                onNavigateToQuizList = { navController.navigate("quiz_list") },
                                onNavigateToMyQuizzes = { navController.navigate("my_quizzes") },
                                onMarkerClick = { quizId, questionId -> navController.navigate("question/$quizId/$questionId") },
                                onSignOut = {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                    }
                                },
                                onNavigateToSupportDashboard = { navController.navigate("support_dashboard") },
                                onNavigateToAdminDashboard = { navController.navigate("admin_dashboard") },
                                onNavigateToNotifications = { navController.navigate("notifications") }
                            )
                        }

                        composable("admin_dashboard") {
                            AdminDashboardScreen(
                                onNavigateToQuizDetail = { quizId ->
                                    navController.navigate("quiz_detail/$quizId")
                                }
                            )
                        }

                        composable("create_quiz") {
                            CreateQuizScreen(
                                onNavigateToAddQuestions = { quizId ->
                                    navController.navigate("add_questions/$quizId")
                                }
                            )
                        }

                        composable(
                            route = "add_questions/{quizId}?questionId={questionId}",
                            arguments = listOf(
                                navArgument("quizId") { type = NavType.StringType },
                                navArgument("questionId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) {
                            AddQuestionsScreen(
                                onFinishQuizCreation = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }


                        composable("profile") {
                            ProfileScreen(
                                onSignOut = {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                    }
                                },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("quiz_list") { backStackEntry ->
                            QuizListScreen(
                                navBackStackEntry = backStackEntry,
                                onNavigateToQuizDetail = { quizId ->
                                    navController.navigate("quiz_detail/$quizId")
                                },
                                onNavigateToMyQuizzes = {
                                    navController.navigate("my_quizzes")
                                }
                            )
                        }

                        composable(
                            route = "quiz_detail/{quizId}",
                            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                        ) {
                            QuizDetailScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateBackAfterDelete = {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("quiz_deleted", true)
                                    navController.popBackStack()
                                },
                                onNavigateToQuestionReview = { quizId ->
                                    navController.navigate("question_review/$quizId")
                                },
                                onNavigateToMyStats = { quizId ->
                                    navController.navigate("subscriber_stats/$quizId")
                                },
                                onNavigateToCreatorStats = { quizId ->
                                    navController.navigate("creator_stats/$quizId")
                                }
                            )
                        }

                        composable("my_quizzes") {
                            MyQuizzesScreen(
                                onConfirmAndNavigateBack = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onNavigateToQuizDetail = { quizId ->
                                    navController.navigate("quiz_detail/$quizId")
                                }
                            )
                        }

                        composable(
                            route = "question/{quizId}/{questionId}",
                            arguments = listOf(
                                navArgument("quizId") { type = NavType.StringType },
                                navArgument("questionId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            QuestionScreen(
                                onNavigateBack = { updatedStatistic ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("updated_stat", updatedStatistic)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen()
                        }

                        composable("support_dashboard") {
                            SupportDashboardScreen(
                                onNavigateToQuizDetail = { quizId ->
                                    navController.navigate("quiz_detail/$quizId")
                                },
                                onNavigateBack = { navController.popBackStack() } // Added back nav
                            )
                        }


                        composable(
                            route = "question_review/{quizId}",
                            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                        ) {
                            QuestionReviewScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToEditQuestion = { quizId, questionId ->
                                    navController.navigate("add_questions/$quizId?questionId=$questionId")
                                },
                                onNavigateToAddQuestion = { quizId ->
                                    navController.navigate("add_questions/$quizId")
                                }
                            )
                        }

                        composable(
                            "creator_stats/{quizId}",
                            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                        ) {
                            CreatorStatsScreen()
                        }

                        composable("notifications") {
                            NotificationScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}