package com.clawstack.shellguard.totp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.clawstack.shellguard.totp.ui.navigation.TotpNavHost
import com.clawstack.shellguard.totp.ui.theme.ShellGuardTheme
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.GatewayViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel

/**
 * ShellGuard-TOTP Primary Activity
 * Enforces FLAG_SECURE on creation in production builds to prevent screen recording and screenshots.
 */
class MainActivity : FragmentActivity() {

    private val app by lazy { application as ShellGuardTotpApp }

    private val authViewModel: AuthViewModel by viewModels()
    private val totpViewModel: TotpViewModel by viewModels()

    private val gatewayViewModel: GatewayViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GatewayViewModel(
                    authRepository = app.authRepository,
                    totpRepository = app.totpRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android 12+ Splash Screen before super.onCreate()
        installSplashScreen()

        // 2. Enforce Screenshot & Task-Switcher Preview Shield in production
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by authViewModel.themeMode.collectAsStateWithLifecycle()
            val themeAccent by authViewModel.themeAccent.collectAsStateWithLifecycle()
            val isVaultHatched by authViewModel.isVaultHatched.collectAsStateWithLifecycle()
            val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsStateWithLifecycle()
            val isLocked by authViewModel.isLocked.collectAsStateWithLifecycle()

            ShellGuardTheme(
                themeMode = themeMode,
                themeAccent = themeAccent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TotpNavHost(
                        navController = navController,
                        totpViewModel = totpViewModel,
                        gatewayViewModel = gatewayViewModel,
                        authViewModel = authViewModel,
                        isVaultHatched = isVaultHatched,
                        isBiometricEnabled = isBiometricEnabled,
                        isLocked = isLocked
                    )
                }
            }
        }
    }
}
