package com.aminmart.passwordmanager

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.aminmart.passwordmanager.data.repository.VaultRepository
import com.aminmart.passwordmanager.ui.navigation.AppNavigation
import com.aminmart.passwordmanager.ui.navigation.Screen
import com.aminmart.passwordmanager.ui.theme.AminmartPasswordManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// FragmentActivity (not ComponentActivity): required by androidx.biometric 1.1.0 BiometricPrompt
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var vaultRepository: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Block screenshots and hide vault contents in the recent-apps switcher
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            AminmartPasswordManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Auto-lock: require re-auth after the app sits in background past the timeout.
                    // Timeout (instead of instant lock) so short trips out of the app — SAF file
                    // pickers for backup/restore, share sheets — don't lock the vault mid-flow.
                    DisposableEffect(Unit) {
                        var stoppedAt = 0L
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> stoppedAt = SystemClock.elapsedRealtime()
                                Lifecycle.Event.ON_START -> {
                                    val backgroundMs = stoppedAt.takeIf { it != 0L }
                                        ?.let { SystemClock.elapsedRealtime() - it }
                                    if (backgroundMs != null) {
                                        lifecycleScope.launch {
                                            val expired = backgroundMs > vaultRepository.getAutoLockTimeoutMs()
                                            if (expired && navController.currentDestination?.route != Screen.Auth.route) {
                                                navController.navigate(Screen.Auth.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = Screen.Auth.route
                    )
                }
            }
        }
    }
}
