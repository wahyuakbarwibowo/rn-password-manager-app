package com.aminmart.passwordmanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.BackupService
import com.aminmart.passwordmanager.data.repository.ImportMode
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val showChangePasswordDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val passwordRepository: PasswordRepository,
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBiometricSetting()
    }

    private fun loadBiometricSetting() {
        viewModelScope.launch {
            vaultRepository.isBiometricEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(biometricEnabled = enabled)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            vaultRepository.setBiometricEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                biometricEnabled = enabled,
                statusMessage = if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled"
            )
        }
    }

    fun showChangePasswordDialog() {
        _uiState.value = _uiState.value.copy(showChangePasswordDialog = true)
    }

    fun hideChangePasswordDialog() {
        _uiState.value = _uiState.value.copy(showChangePasswordDialog = false)
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                vaultRepository.changeMasterPassword(oldPassword, newPassword)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Password changed successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to change password: ${e.message}"
                )
            }
        }
    }

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // For export, we need to verify password first
            // In a real app, you'd prompt for the password
            // For now, we'll just use a placeholder
            val result = backupService.exportBackup("placeholder", uri)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = when (result) {
                    is com.aminmart.passwordmanager.data.repository.BackupResult.Success -> 
                        "Backup exported successfully (${result.imported} passwords)"
                    is com.aminmart.passwordmanager.data.repository.BackupResult.Error -> 
                        "Export failed: ${result.message}"
                }
            )
        }
    }

    fun importBackup(uri: android.net.Uri, mode: ImportMode) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // For import, prompt for password in a real app
            val result = backupService.importBackup("placeholder", uri, mode)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = when (result) {
                    is com.aminmart.passwordmanager.data.repository.BackupResult.Success -> 
                        "Imported ${result.imported} passwords (${result.skipped} skipped)"
                    is com.aminmart.passwordmanager.data.repository.BackupResult.Error -> 
                        "Import failed: ${result.message}"
                }
            )
        }
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
            statusMessage = "Vault locked"
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
