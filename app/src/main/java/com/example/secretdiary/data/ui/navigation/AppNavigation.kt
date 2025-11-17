package com.example.secretdiary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.secretdiary.ui.screen.AuthScreen
import com.example.secretdiary.ui.screen.DiaryDetailScreen
import com.example.secretdiary.ui.screen.DiaryListScreen
import com.example.secretdiary.ui.viewmodel.ViewModelFactory

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object DiaryList : Screen("diaryList")
    object DiaryDetail : Screen("diaryDetail")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    onShowBiometricPrompt: (onSuccess: () -> Unit) -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.DiaryList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onAuthRequested = {
                    // Call the biometric prompt function from MainActivity
                    onShowBiometricPrompt {
                        // This is the onSuccess callback
                        navController.navigate(Screen.DiaryList.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.DiaryList.route) {
            DiaryListScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onAddEntry = {
                    navController.navigate("${Screen.DiaryDetail.route}/-1")
                },
                onEntryClick = { entryId ->
                    navController.navigate("${Screen.DiaryDetail.route}/$entryId")
                }
            )
        }

        composable(
            route = "${Screen.DiaryDetail.route}/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1
            DiaryDetailScreen(
                viewModel = viewModel(factory = viewModelFactory),
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}