package com.clawstack.shellguard.totp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clawstack.shellguard.totp.ui.screens.AddSecretScreen
import com.clawstack.shellguard.totp.ui.screens.GatewayScreen
import com.clawstack.shellguard.totp.ui.screens.HatchVaultScreen
import com.clawstack.shellguard.totp.ui.screens.LockScreen
import com.clawstack.shellguard.totp.ui.screens.LoginScreen
import com.clawstack.shellguard.totp.ui.screens.QrScannerScreen
import com.clawstack.shellguard.totp.ui.screens.SettingsScreen
import com.clawstack.shellguard.totp.ui.screens.TotpListScreen
import com.clawstack.shellguard.totp.ui.viewmodels.AuthViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.GatewayViewModel
import com.clawstack.shellguard.totp.ui.viewmodels.TotpViewModel

sealed class Screen(val route: String) {
    data object HatchVault : Screen("hatch_vault")
    data object Lock : Screen("lock")
    data object Login : Screen("login")
    data object Gateway : Screen("gateway")
    data object CodeList : Screen("code_list")
    data object AddSecret : Screen("add_secret")
    data object QrScanner : Screen("qr_scanner")
    data object Settings : Screen("settings")
}

@Composable
fun TotpNavHost(
    navController: NavHostController,
    totpViewModel: TotpViewModel,
    gatewayViewModel: GatewayViewModel,
    authViewModel: AuthViewModel,
    isVaultHatched: Boolean,
    isBiometricEnabled: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val initialDestination = when {
        !isVaultHatched -> Screen.HatchVault.route
        isBiometricEnabled || isLocked -> Screen.Lock.route
        else -> Screen.CodeList.route
    }

    val vaultMode by authViewModel.vaultMode.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = initialDestination,
        modifier = modifier
    ) {
        composable(
            route = Screen.HatchVault.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            HatchVaultScreen(
                onVaultHatched = { masterSecret, isPin, enableBiometrics ->
                    authViewModel.hatchVault(masterSecret, isPin, enableBiometrics)
                    navController.navigate(Screen.CodeList.route) {
                        popUpTo(Screen.HatchVault.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Lock.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            LockScreen(
                vaultMode = vaultMode,
                isBiometricEnabled = isBiometricEnabled,
                onUnlockWithSecret = { secret ->
                    val success = authViewModel.unlockWithSecret(secret)
                    if (success) {
                        navController.navigate(Screen.CodeList.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                    success
                },
                onUnlockSuccess = {
                    authViewModel.unlockWithBiometrics()
                    navController.navigate(Screen.CodeList.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                },
                onNavigateToGateway = {
                    navController.navigate(Screen.Gateway.route)
                }
            )
        }

        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.CodeList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToGateway = {
                    navController.navigate(Screen.Gateway.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Gateway.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) {
            GatewayScreen(
                viewModel = gatewayViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.CodeList.route) {
                        popUpTo(Screen.Gateway.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.CodeList.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            TotpListScreen(
                viewModel = totpViewModel,
                onAddSecretClick = { navController.navigate(Screen.AddSecret.route) },
                onScanQrClick = { navController.navigate(Screen.QrScanner.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.AddSecret.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) {
            val isServerConnected by totpViewModel.isServerConnected.collectAsStateWithLifecycle()
            AddSecretScreen(
                onSaveSecret = { title, username, category, secret, algorithm, digits, period ->
                    totpViewModel.addManualSecret(
                        title = title,
                        username = username,
                        category = category,
                        secret = secret,
                        algorithm = algorithm,
                        digits = digits,
                        period = period
                    )
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
                isServerConnected = isServerConnected
            )
        }

        composable(
            route = Screen.QrScanner.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) {
            QrScannerScreen(
                onCodeScanned = { scannedUri ->
                    totpViewModel.importScannedUri(scannedUri)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGateway = { navController.navigate(Screen.Gateway.route) },
                totpViewModel = totpViewModel
            )
        }
    }
}
