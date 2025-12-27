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
import com.example.secretdiary.data.location.LocationHelper
import com.example.secretdiary.ui.theme.navigation.AppNavigation
import com.example.secretdiary.ui.theme.SecretDiaryTheme
import com.example.secretdiary.ui.theme.viewmodel.ViewModelFactory
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {

    private val executor: Executor by lazy { ContextCompat.getMainExecutor(this) }
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var onAuthSuccess: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val application = application as DiaryApplication
        val repository = application.repository
        val locationHelper = LocationHelper(this)
        val viewModelFactory = ViewModelFactory(repository, locationHelper)

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
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
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
            this.onAuthSuccess = onSuccess
            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(
                this,
                "Biometric authentication is not available or not enrolled.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}