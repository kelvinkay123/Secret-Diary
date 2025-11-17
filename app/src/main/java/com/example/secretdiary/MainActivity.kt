package com.example.secretdiary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.secretdiary.ui.navigation.AppNavigation
import com.example.secretdiary.ui.theme.SecretDiaryTheme
import com.example.secretdiary.ui.viewmodel.ViewModelFactory
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {

    // executor can be initialized directly.
    private val executor: Executor by lazy { ContextCompat.getMainExecutor(this) }
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // A variable to hold the action to run on successful authentication.
    private var onAuthSuccess: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val application = application as DiaryApplication
        val repository = application.repository
        val viewModelFactory = ViewModelFactory(repository)

        setupBiometrics()

        setContent {
            SecretDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        viewModelFactory = viewModelFactory,
                        onShowBiometricPrompt = { onSuccess ->
                            showBiometricPrompt(onSuccess)
                        }
                    )
                }
            }
        }
    }

    private fun setupBiometrics() {
        // FIX: The constructor requires an Activity, an Executor, AND a Callback.
        // We provide a single, reusable callback here.
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // When authentication succeeds, run the lambda we stored earlier.
                onAuthSuccess?.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    applicationContext,
                    "Authentication error: $errString",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    applicationContext,
                    "Authentication failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Now this constructor call is valid.
        biometricPrompt = BiometricPrompt(this, executor, callback)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Secret Diary")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            // Store the success action to be run by the callback.
            this.onAuthSuccess = onSuccess

            // Now, we just call authenticate with the prompt info.
            // The prompt will use the callback we defined during its construction.
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Handle cases where biometrics are not set up.
            Toast.makeText(
                this,
                "Biometric authentication is not available or not enrolled.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}