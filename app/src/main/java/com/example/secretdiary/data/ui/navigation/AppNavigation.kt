package com.example.secretdiary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// FIX: Corrected import paths to reflect the actual project structure
import com.example.secretdiary.data.ui.camerascreen.CameraScreen
import com.example.secretdiary.ui.screen.AuthScreen
import com.example.secretdiary.data.ui.screen.DiaryDetailScreen
import com.example.secretdiary.ui.screen.DiaryListScreen
import com.example.secretdiary.ui.viewmodel.ViewModelFactory

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object DiaryList : Screen("diaryList")
    object DiaryDetail : Screen("diaryDetail")
    object Camera : Screen("camera")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    onShowBiometricPrompt: (onSuccess: () -> Unit) -> Unit
) {
    // The NavHost defines the navigation graph. All composable destinations must be inside this block.
    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.DiaryList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onAuthRequested = {
                    // FIX: Re-enabled the biometric prompt functionality
                    onShowBiometricPrompt {
                        // This is the onSuccess callback for biometrics
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
                onNavigateBack = { navController.popBackStack() },
                onOpenCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                // FIX: Pass the SavedStateHandle from the backStackEntry to the screen
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        // FIX: Moved the CameraScreen composable INSIDE the NavHost
        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { uri ->
                    // Pass the captured image URI back to the previous screen (DiaryDetailScreen)
                    // using the SavedStateHandle. This is the modern, recommended way.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("image_uri", uri)
                    navController.popBackStack()
                },
                onCancelled = {
                    // If the user cancels, just navigate back.
                    navController.popBackStack()
                }
            )
        }
    }
}
