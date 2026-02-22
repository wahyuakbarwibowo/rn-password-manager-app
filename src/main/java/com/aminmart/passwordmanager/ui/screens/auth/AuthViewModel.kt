package com.aminmart.passwordmanager.ui.screens.auth

import android.app.Application
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.VaultRepository
import com.aminmart.passwordmanager.data.security.BiometricAuthService
import com.aminmart.passwordmanager.data.security.BiometricAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val needsSetup: Boolean = true,
    val masterPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val biometricAvailable: Boolean = false,
    val showBiometricPrompt: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application,
    private val vaultRepository: VaultRepository,
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
                if (_uiState.value.needsSetup) {
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

    fun authenticateWithBiometric() {
        val activity = application as? FragmentActivity ?: return
        
        _uiState.value = _uiState.value.copy(showBiometricPrompt = true)
        
        biometricAuthService.authenticate(
            activity = activity,
            title = "Unlock Password Manager",
            subtitle = "Use your biometric to unlock",
            negativeButtonText = "Use Password",
            onAuthenticationSuccess = {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        showBiometricPrompt = false,
                        isAuthenticated = true
                    )
                }
            },
            onAuthenticationError = { errorCode, errString ->
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        showBiometricPrompt = false,
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
