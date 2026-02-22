package com.aminmart.passwordmanager.data.repository

import com.aminmart.passwordmanager.data.local.SettingsDao
import com.aminmart.passwordmanager.data.local.SettingsEntity
import com.aminmart.passwordmanager.data.local.SettingsKeys
import com.aminmart.passwordmanager.data.security.PasswordHashingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for vault and settings operations.
 * Handles master password verification and vault state.
 */
@Singleton
class VaultRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val passwordHashingService: PasswordHashingService
) {

    /**
     * Check if the vault has been initialized.
     */
    suspend fun isVaultInitialized(): Boolean {
        return settingsDao.hasSetting(SettingsKeys.MASTER_PASSWORD_HASH)
    }

    /**
     * Initialize the vault with a master password.
     */
    suspend fun initializeVault(masterPassword: String) {
        if (isVaultInitialized()) {
            throw IllegalStateException("Vault is already initialized")
        }

        if (masterPassword.length < 8) {
            throw IllegalArgumentException("Master password must be at least 8 characters")
        }

        val passwordHash = passwordHashingService.hashPassword(masterPassword)

        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_SALT,
                value = passwordHash.salt
            )
        )
        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_HASH,
                value = passwordHash.hash
            )
        )
        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.VAULT_INITIALIZED,
                value = "true"
            )
        )
    }

    /**
     * Verify the master password.
     */
    suspend fun verifyPassword(masterPassword: String): Boolean {
        val saltSetting = settingsDao.getSetting(SettingsKeys.MASTER_PASSWORD_SALT)
        val hashSetting = settingsDao.getSetting(SettingsKeys.MASTER_PASSWORD_HASH)

        if (saltSetting == null || hashSetting == null) {
            return false
        }

        return passwordHashingService.verifyPassword(
            password = masterPassword,
            salt = saltSetting.value,
            expectedHash = hashSetting.value
        )
    }

    /**
     * Change the master password.
     * Requires the old password for verification.
     */
    suspend fun changeMasterPassword(oldPassword: String, newPassword: String) {
        if (!verifyPassword(oldPassword)) {
            throw SecurityException("Old password is incorrect")
        }

        if (newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters")
        }

        val passwordHash = passwordHashingService.hashPassword(newPassword)

        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_SALT,
                value = passwordHash.salt
            )
        )
        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_HASH,
                value = passwordHash.hash
            )
        )
    }

    /**
     * Check if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Flow<Boolean> {
        return settingsDao.getSettingFlow(SettingsKeys.BIOMETRIC_ENABLED)
            .map { it?.value == "true" }
    }

    /**
     * Set biometric authentication enabled/disabled.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        settingsDao.saveSetting(
            SettingsEntity(
                key = SettingsKeys.BIOMETRIC_ENABLED,
                value = enabled.toString()
            )
        )
    }

    /**
     * Delete the vault (resets everything).
     * WARNING: This will make all data inaccessible.
     */
    suspend fun deleteVault() {
        settingsDao.deleteSetting(SettingsKeys.MASTER_PASSWORD_SALT)
        settingsDao.deleteSetting(SettingsKeys.MASTER_PASSWORD_HASH)
        settingsDao.deleteSetting(SettingsKeys.VAULT_INITIALIZED)
        settingsDao.deleteSetting(SettingsKeys.BIOMETRIC_ENABLED)
    }
}
