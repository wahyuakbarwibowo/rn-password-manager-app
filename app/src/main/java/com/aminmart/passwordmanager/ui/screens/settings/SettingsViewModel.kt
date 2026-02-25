package com.aminmart.passwordmanager.ui.screens.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.BackupService
import com.aminmart.passwordmanager.data.repository.ImportMode
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.data.repository.VaultRepository
import com.aminmart.passwordmanager.data.security.BiometricAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val showChangePasswordDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showPasswordDialogForBiometric: Boolean = false,
    val showPasswordDialogForExport: Boolean = false,
    val showPasswordDialogForImport: Boolean = false,
    val pendingBiometricState: Boolean = false,
    val pendingExportUri: android.net.Uri? = null,
    val pendingImportUri: android.net.Uri? = null,
    val pendingImportMode: ImportMode = ImportMode.MERGE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val vaultRepository: VaultRepository,
    private val passwordRepository: PasswordRepository,
    private val backupService: BackupService,
    private val biometricAuthService: BiometricAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBiometricSetting()
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val availability = biometricAuthService.isBiometricAvailable(application)
        _uiState.value = _uiState.value.copy(
            biometricAvailable = availability == com.aminmart.passwordmanager.data.security.BiometricAvailability.AVAILABLE
        )
    }

    private fun loadBiometricSetting() {
        viewModelScope.launch {
            vaultRepository.isBiometricEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        if (!enabled) {
            // Disabling biometrics - just update setting
            viewModelScope.launch {
                vaultRepository.setBiometricEnabled(false)
                _uiState.value = _uiState.value.copy(
                    biometricEnabled = false,
                    statusMessage = "Biometric unlock disabled"
                )
            }
            return
        }

        // Enabling biometrics - check availability first
        val availability = biometricAuthService.isBiometricAvailable(application)
        
        when (availability) {
            com.aminmart.passwordmanager.data.security.BiometricAvailability.AVAILABLE -> {
                // Show password verification dialog first
                _uiState.value = _uiState.value.copy(
                    pendingBiometricState = true,
                    showPasswordDialogForBiometric = true
                )
            }
            com.aminmart.passwordmanager.data.security.BiometricAvailability.NONE_ENROLLED -> {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "No biometric enrolled. Please set up fingerprint or face unlock in device settings."
                )
            }
            com.aminmart.passwordmanager.data.security.BiometricAvailability.NO_HARDWARE,
            com.aminmart.passwordmanager.data.security.BiometricAvailability.HARDWARE_UNAVAILABLE -> {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Biometric hardware not available"
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Biometric authentication not available"
                )
            }
        }
    }

    fun enableBiometricAfterVerification(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Verify password first
                val isValid = vaultRepository.verifyPassword(password)
                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Invalid password"
                    )
                    return@launch
                }

                // Enable biometric
                vaultRepository.setBiometricEnabled(true)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    biometricEnabled = true,
                    pendingBiometricState = false,
                    showPasswordDialogForBiometric = false,
                    statusMessage = "Biometric unlock enabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to enable biometric: ${e.message}"
                )
            }
        }
    }

    fun cancelBiometricSetup() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialogForBiometric = false,
            pendingBiometricState = false
        )
    }

    fun showChangePasswordDialog() {
        _uiState.value = _uiState.value.copy(showChangePasswordDialog = true)
    }

    fun hideChangePasswordDialog() {
        _uiState.value = _uiState.value.copy(showChangePasswordDialog = false)
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (newPassword != confirmPassword) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "New passwords do not match"
                )
                return@launch
            }

            if (newPassword.length < 8) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Password must be at least 8 characters"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                vaultRepository.changeMasterPassword(oldPassword, newPassword)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showChangePasswordDialog = false,
                    statusMessage = "Password changed successfully"
                )
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Old password is incorrect"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to change password: ${e.message}"
                )
            }
        }
    }

    fun requestExportBackup(uri: android.net.Uri) {
        _uiState.value = _uiState.value.copy(
            pendingExportUri = uri,
            showPasswordDialogForExport = true
        )
    }

    fun exportBackup(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Verify password first
                val isValid = vaultRepository.verifyPassword(password)
                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Invalid password"
                    )
                    return@launch
                }

                val uri = _uiState.value.pendingExportUri
                if (uri == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "No export location selected"
                    )
                    return@launch
                }

                val result = backupService.exportBackup(password, uri)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showPasswordDialogForExport = false,
                    pendingExportUri = null,
                    statusMessage = when (result) {
                        is com.aminmart.passwordmanager.data.repository.BackupResult.Success ->
                            "Backup exported successfully (${result.imported} passwords)"
                        is com.aminmart.passwordmanager.data.repository.BackupResult.Error ->
                            "Export failed: ${result.message}"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun cancelExport() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialogForExport = false,
            pendingExportUri = null
        )
    }

    fun requestImportBackup(uri: android.net.Uri, mode: ImportMode = ImportMode.MERGE) {
        _uiState.value = _uiState.value.copy(
            pendingImportUri = uri,
            pendingImportMode = mode,
            showPasswordDialogForImport = true
        )
    }

    fun importBackup(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Verify password first
                val isValid = vaultRepository.verifyPassword(password)
                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Invalid password"
                    )
                    return@launch
                }

                val uri = _uiState.value.pendingImportUri
                if (uri == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "No import file selected"
                    )
                    return@launch
                }

                val result = backupService.importBackup(password, uri, _uiState.value.pendingImportMode)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showPasswordDialogForImport = false,
                    pendingImportUri = null,
                    statusMessage = when (result) {
                        is com.aminmart.passwordmanager.data.repository.BackupResult.Success ->
                            "Imported ${result.imported} passwords (${result.skipped} skipped)"
                        is com.aminmart.passwordmanager.data.repository.BackupResult.Error ->
                            "Import failed: ${result.message}"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Import failed: ${e.message}"
                )
            }
        }
    }

    fun cancelImport() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialogForImport = false,
            pendingImportUri = null
        )
    }

    fun showDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun hideDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                passwordRepository.deleteAllPasswords()
                vaultRepository.deleteVault()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showDeleteConfirmDialog = false,
                    statusMessage = "All data deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to delete data: ${e.message}"
                )
            }
        }
    }

    fun lockVault() {
        // In a real app, this would clear the decryption key from memory
        _uiState.value = _uiState.value.copy(
            statusMessage = "Vault locked. You will need to enter your password to access passwords again."
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
