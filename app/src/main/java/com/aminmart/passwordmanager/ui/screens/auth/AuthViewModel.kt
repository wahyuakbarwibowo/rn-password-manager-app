package com.aminmart.passwordmanager.ui.screens.auth

import android.app.Application
import android.net.Uri
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.data.repository.VaultRepository
import com.aminmart.passwordmanager.data.security.BiometricAuthService
import com.aminmart.passwordmanager.data.security.BiometricAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class AuthUiState(
    val needsSetup: Boolean = true,
    val masterPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val biometricAvailable: Boolean = false,
    // Forgot-password flow
    val isResetMode: Boolean = false,
    val showForgotDialog: Boolean = false,
    val hasRecoveryKey: Boolean = false,
    val biometricEnabled: Boolean = false,
    val showWipeConfirmDialog: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application,
    private val vaultRepository: VaultRepository,
    private val passwordRepository: PasswordRepository,
    private val biometricAuthService: BiometricAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkVaultStatus()
            checkBiometricAvailability()
        }
    }

    private suspend fun checkVaultStatus() {
        val isInitialized = vaultRepository.isVaultInitialized()
        _uiState.value = _uiState.value.copy(
            needsSetup = !isInitialized
        )
    }

    private fun checkBiometricAvailability() {
        val availability = biometricAuthService.isBiometricAvailable(application)
        _uiState.value = _uiState.value.copy(
            biometricAvailable = availability == BiometricAvailability.AVAILABLE
        )
    }

    fun onMasterPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            masterPassword = password,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = password,
            errorMessage = null
        )
    }

    fun authenticate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                if (_uiState.value.isResetMode) {
                    // Set a new master password after identity was verified
                    // (recovery key file or biometric)
                    if (_uiState.value.masterPassword.length < 8) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Password must be at least 8 characters"
                        )
                        return@launch
                    }
                    if (_uiState.value.masterPassword != _uiState.value.confirmPassword) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Passwords do not match"
                        )
                        return@launch
                    }
                    vaultRepository.resetMasterPassword(_uiState.value.masterPassword)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isResetMode = false,
                        isAuthenticated = true
                    )
                } else if (_uiState.value.needsSetup) {
                    // Setup new vault
                    if (_uiState.value.masterPassword.length < 8) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Password must be at least 8 characters"
                        )
                        return@launch
                    }

                    if (_uiState.value.masterPassword != _uiState.value.confirmPassword) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Passwords do not match"
                        )
                        return@launch
                    }

                    vaultRepository.initializeVault(_uiState.value.masterPassword)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                } else {
                    // Unlock existing vault
                    val isValid = vaultRepository.verifyPassword(_uiState.value.masterPassword)
                    if (isValid) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Wrong password"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Authentication failed"
                )
            }
        }
    }

    // --- Forgot-password flow ---

    fun onForgotPassword() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showForgotDialog = true,
                hasRecoveryKey = vaultRepository.hasRecoveryKey(),
                biometricEnabled = _uiState.value.biometricAvailable &&
                    vaultRepository.isBiometricEnabled().first(),
                errorMessage = null
            )
        }
    }

    fun dismissForgotDialog() {
        _uiState.value = _uiState.value.copy(showForgotDialog = false)
    }

    /**
     * Verify an uploaded recovery key file; on success switch to reset mode.
     */
    fun verifyRecoveryFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = application.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }.orEmpty().trim()
                // File is JSON {"key": "..."} but accept a bare key string too
                val key = runCatching { JSONObject(text).getString("key") }.getOrDefault(text)

                if (vaultRepository.verifyRecoveryKey(key)) {
                    enterResetMode()
                } else {
                    _uiState.value = _uiState.value.copy(
                        showForgotDialog = false,
                        errorMessage = "Invalid recovery key file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showForgotDialog = false,
                    errorMessage = "Could not read recovery key file"
                )
            }
        }
    }

    fun authenticateWithBiometricForReset(activity: FragmentActivity) {
        biometricAuthService.authenticate(
            activity = activity,
            title = "Reset Master Password",
            subtitle = "Verify your identity to set a new password",
            negativeButtonText = "Cancel",
            onAuthenticationSuccess = {
                viewModelScope.launch { enterResetMode() }
            },
            onAuthenticationError = { errorCode, errString ->
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        showForgotDialog = false,
                        errorMessage = if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        ) null else errString
                    )
                }
            },
            onAuthenticationFailed = {}
        )
    }

    private fun enterResetMode() {
        _uiState.value = _uiState.value.copy(
            showForgotDialog = false,
            isResetMode = true,
            masterPassword = "",
            confirmPassword = "",
            errorMessage = null
        )
    }

    fun requestWipe() {
        _uiState.value = _uiState.value.copy(
            showForgotDialog = false,
            showWipeConfirmDialog = true
        )
    }

    fun dismissWipeConfirm() {
        _uiState.value = _uiState.value.copy(showWipeConfirmDialog = false)
    }

    /**
     * Last resort: delete everything and start over with a fresh vault.
     */
    fun wipeAndStartOver() {
        viewModelScope.launch {
            passwordRepository.deleteAllPasswords()
            vaultRepository.deleteVault()
            _uiState.value = AuthUiState(
                needsSetup = true,
                biometricAvailable = _uiState.value.biometricAvailable
            )
        }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        biometricAuthService.authenticate(
            activity = activity,
            title = "Unlock Password Manager",
            subtitle = "Use your biometric to unlock",
            negativeButtonText = "Use Password",
            onAuthenticationSuccess = {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true
                    )
                }
            },
            onAuthenticationError = { errorCode, errString ->
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            null // User chose to use password instead
                        } else {
                            errString
                        }
                    )
                }
            },
            onAuthenticationFailed = {
                // Fingerprint not recognized, try again
            }
        )
    }
}
