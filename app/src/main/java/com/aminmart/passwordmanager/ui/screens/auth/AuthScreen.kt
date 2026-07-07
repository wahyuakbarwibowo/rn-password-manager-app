package com.aminmart.passwordmanager.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    val recoveryFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(viewModel::verifyRecoveryFile) }

    AuthScreenContent(
        uiState = uiState,
        onMasterPasswordChange = viewModel::onMasterPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onAuthenticate = viewModel::authenticate,
        onBiometricAuth = { activity?.let(viewModel::authenticateWithBiometric) },
        onForgotPassword = viewModel::onForgotPassword,
        onAuthSuccess = onAuthSuccess
    )

    if (uiState.showForgotDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissForgotDialog,
            title = { Text("Forgot Password?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to verify your identity and set a new master password. Your saved passwords will be kept.")
                    if (uiState.hasRecoveryKey) {
                        Button(
                            onClick = { recoveryFileLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Use Recovery Key File") }
                    }
                    if (uiState.biometricEnabled) {
                        Button(
                            onClick = { activity?.let(viewModel::authenticateWithBiometricForReset) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Use Biometric") }
                    }
                    if (!uiState.hasRecoveryKey && !uiState.biometricEnabled) {
                        Text(
                            "No recovery key or biometric is set up. The only option is to reset the app, which deletes all saved passwords.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::requestWipe,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Reset App (Delete All Data)") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissForgotDialog) { Text("Cancel") }
            }
        )
    }

    if (uiState.showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWipeConfirm,
            title = { Text("Delete All Data?") },
            text = { Text("This permanently deletes ALL saved passwords and resets the app. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::wipeAndStartOver,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWipeConfirm) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onMasterPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onBiometricAuth: () -> Unit,
    onForgotPassword: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    // Setup and reset both use the password+confirm form
    val needsConfirm = uiState.needsSetup || uiState.isResetMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App icon / logo
            Text(
                text = "🔐",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = when {
                    uiState.isResetMode -> "Set New Master Password"
                    uiState.needsSetup -> "Set Master Password"
                    else -> "Enter Master Password"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = when {
                    uiState.isResetMode -> "Identity verified. Choose a new master password"
                    uiState.needsSetup -> "Create a master password to protect all your passwords"
                    else -> "Enter your master password to unlock your vault"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.masterPassword,
                onValueChange = onMasterPasswordChange,
                label = { Text("Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    imeAction = if (needsConfirm) ImeAction.Next else ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(onDone = { onAuthenticate() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null
            )

            if (needsConfirm) {
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAuthenticate() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage != null
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onAuthenticate,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (needsConfirm) "Save & Continue" else "Unlock",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (uiState.biometricAvailable && !uiState.needsSetup && !uiState.isResetMode) {
                TextButton(onClick = onBiometricAuth) {
                    Text("Use Biometric Instead")
                }
            }

            if (!uiState.needsSetup && !uiState.isResetMode) {
                TextButton(onClick = onForgotPassword) {
                    Text("Forgot Password?")
                }
            }
        }
    }
}
