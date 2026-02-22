package com.aminmart.passwordmanager.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aminmart.passwordmanager.ui.components.VaultSetupDialog

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    AuthScreenContent(
        uiState = uiState,
        onMasterPasswordChange = viewModel::onMasterPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onAuthenticate = viewModel::authenticate,
        onBiometricAuth = viewModel::authenticateWithBiometric,
        onAuthSuccess = onAuthSuccess
    )
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onMasterPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onBiometricAuth: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var passwordDialogOpen by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }
    
    LaunchedEffect(uiState.showBiometricPrompt) {
        if (uiState.showBiometricPrompt && uiState.biometricAvailable) {
            onBiometricAuth()
        }
    }
    
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
                text = if (uiState.needsSetup) "Set Master Password" else "Enter Master Password",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = if (uiState.needsSetup) {
                    "Create a master password to protect all your passwords"
                } else {
                    "Enter your master password to unlock your vault"
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
                    imeAction = if (uiState.needsSetup) ImeAction.Next else ImeAction.Done,
                    keyboardType = KeyboardOptions.Password
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null
            )
            
            if (uiState.needsSetup) {
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardOptions.Password
                    ),
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
                        text = if (uiState.needsSetup) "Save & Continue" else "Unlock",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            if (uiState.biometricAvailable && !uiState.needsSetup) {
                TextButton(
                    onClick = { passwordDialogOpen = true }
                ) {
                    Text("Use Biometric Instead")
                }
            }
        }
    }
    
    if (passwordDialogOpen) {
        AlertDialog(
            onDismissRequest = { passwordDialogOpen = false },
            title = { Text("Authentication") },
            text = { Text("Choose authentication method") },
            confirmButton = {
                TextButton(onClick = {
                    passwordDialogOpen = false
                    onBiometricAuth()
                }) {
                    Text("Biometric")
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
