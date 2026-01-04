package com.example.secretdiary.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.secretdiary.ui.theme.screen.AuthScreen
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
                onAuthRequested = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onAuthSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ---------- LOGIN (BIOMETRIC INSIDE LOGIN) ----------
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onBiometricUnlock = {
                    onShowBiometricPrompt {
                        navController.navigate(Screen.DiaryList.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ---------- SIGNUP ----------
        composable(Screen.Signup.route) {
            SignupScreen(navController = navController)
        }

        // ---------- DIARY LIST ----------
        composable(Screen.DiaryList.route) { backStackEntry ->
            DiaryListScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onAddEntry = { navController.navigate(Screen.diaryDetailRoute(-1)) },
                onEntryClick = { entryId -> navController.navigate(Screen.diaryDetailRoute(entryId)) },

                // ✅ open camera from list
                onOpenCamera = { navController.navigate(Screen.Camera.route) },

                // ✅ after capture, open detail(-1) and pass media into THAT detail savedStateHandle
                onCreateEntryWithMedia = { uriString: String, isVideo: Boolean ->
                    navController.navigate(Screen.diaryDetailRoute(-1))
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("media_uri", uriString)
                        set("is_video", isVideo)
                    }
                },

                // ✅ list reads camera result from here
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        // ---------- DIARY DETAIL ----------
        composable(route = "${Screen.DiaryDetail.route}/{entryId}") { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")?.toIntOrNull() ?: -1

            DiaryDetailScreen(
                viewModel = viewModel(factory = viewModelFactory),
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        // ---------- CAMERA ----------
        composable(Screen.Camera.route) {
            CameraScreen(
                onMediaCaptured = { uri, isVideo ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("media_uri", uri.toString())
                        set("is_video", isVideo)
                    }
                    navController.popBackStack()
                },
                onCancelled = { navController.popBackStack() }
            )
        }
    }
}
