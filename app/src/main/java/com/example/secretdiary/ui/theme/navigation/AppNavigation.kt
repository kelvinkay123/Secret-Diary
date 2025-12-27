package com.example.secretdiary.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.secretdiary.ui.theme.screen.AuthScreen
import com.example.secretdiary.ui.theme.screen.BiometricUnlockScreen
import com.example.secretdiary.ui.theme.screen.CameraScreen
import com.example.secretdiary.ui.theme.screen.DiaryDetailScreen
import com.example.secretdiary.ui.theme.screen.DiaryListScreen
import com.example.secretdiary.ui.theme.screen.LoginScreen
import com.example.secretdiary.ui.theme.screen.SignupScreen
import com.example.secretdiary.ui.theme.viewmodel.ViewModelFactory

// -------------------- SCREENS --------------------
sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Biometric : Screen("biometric")
    data object DiaryList : Screen("diary_list")
    data object DiaryDetail : Screen("diary_detail")
    data object Camera : Screen("camera")

    companion object {
        fun diaryDetailRoute(entryId: Int): String {
            return "${DiaryDetail.route}/$entryId"
        }
    }
}

// -------------------- NAVIGATION --------------------
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    onShowBiometricPrompt: (onSuccess: () -> Unit) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {

        // ---------- AUTH ----------
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onAuthRequested = {
                    navController.navigate(Screen.Biometric.route)
                }
            )
        }

        // ---------- LOGIN ----------
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // ---------- SIGNUP ----------
        composable(Screen.Signup.route) {
            SignupScreen(navController = navController)
        }

        // ---------- BIOMETRIC ----------
        composable(Screen.Biometric.route) {
            BiometricUnlockScreen(
                onUnlockRequested = {
                    onShowBiometricPrompt {
                        navController.navigate(Screen.DiaryList.route) {
                            popUpTo(Screen.Biometric.route) { inclusive = true }
                        }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        // ---------- DIARY LIST ----------
        composable(Screen.DiaryList.route) {
            DiaryListScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onAddEntry = {
                    navController.navigate(Screen.diaryDetailRoute(-1))
                },
                onEntryClick = { entryId ->
                    navController.navigate(Screen.diaryDetailRoute(entryId))
                }
            )
        }

        // ---------- DIARY DETAIL ----------
        composable(
            route = "${Screen.DiaryDetail.route}/{entryId}",
            arguments = listOf(
                navArgument("entryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1

            DiaryDetailScreen(
                viewModel = viewModel(factory = viewModelFactory),
                entryId = entryId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        // ---------- CAMERA ----------
        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { uri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("image_uri", uri)
                    navController.popBackStack()
                },
                onCancelled = {
                    navController.popBackStack()
                }
            )
        }
    }
}
