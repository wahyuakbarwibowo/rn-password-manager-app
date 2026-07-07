package com.aminmart.passwordmanager.data.repository

import com.aminmart.passwordmanager.data.local.PasswordDatabase
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
    private val database: PasswordDatabase,
    private val passwordHashingService: PasswordHashingService
) {

    /**
     * Check if the vault has been initialized.
     */
    suspend fun isVaultInitialized(): Boolean {
        return database.hasSetting(SettingsKeys.MASTER_PASSWORD_HASH)
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

        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_SALT,
                value = passwordHash.salt
            )
        )
        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_HASH,
                value = passwordHash.hash
            )
        )
        database.saveSetting(
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
        val saltSetting = database.getSetting(SettingsKeys.MASTER_PASSWORD_SALT)
        val hashSetting = database.getSetting(SettingsKeys.MASTER_PASSWORD_HASH)

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

        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_SALT,
                value = passwordHash.salt
            )
        )
        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_HASH,
                value = passwordHash.hash
            )
        )
    }

    /**
     * Reset the master password without knowing the old one (forgot-password flow).
     * Caller MUST have verified the user's identity first (e.g. biometric).
     * Safe for stored entries: they are encrypted with the Keystore key, not this password.
     */
    suspend fun resetMasterPassword(newPassword: String) {
        if (newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters")
        }

        val passwordHash = passwordHashingService.hashPassword(newPassword)

        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.MASTER_PASSWORD_SALT,
                value = passwordHash.salt
            )
        )
        database.saveSetting(
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
        return database.getSettingFlow(SettingsKeys.BIOMETRIC_ENABLED)
            .map { it?.value == "true" }
    }

    /**
     * Set biometric authentication enabled/disabled.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.BIOMETRIC_ENABLED,
                value = enabled.toString()
            )
        )
    }

    /**
     * Generate a recovery key for the forgot-password flow.
     * Returns the key string the user must keep (as a file); only its
     * PBKDF2 hash is stored, so the key cannot be recovered from the app.
     */
    suspend fun generateRecoveryKey(): String {
        val keyBytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val key = android.util.Base64.encodeToString(keyBytes, android.util.Base64.NO_WRAP)
        val keyHash = passwordHashingService.hashPassword(key)

        database.saveSetting(
            SettingsEntity(key = SettingsKeys.RECOVERY_KEY_SALT, value = keyHash.salt)
        )
        database.saveSetting(
            SettingsEntity(key = SettingsKeys.RECOVERY_KEY_HASH, value = keyHash.hash)
        )
        return key
    }

    suspend fun hasRecoveryKey(): Boolean {
        return database.hasSetting(SettingsKeys.RECOVERY_KEY_HASH)
    }

    suspend fun verifyRecoveryKey(key: String): Boolean {
        val saltSetting = database.getSetting(SettingsKeys.RECOVERY_KEY_SALT) ?: return false
        val hashSetting = database.getSetting(SettingsKeys.RECOVERY_KEY_HASH) ?: return false
        return passwordHashingService.verifyPassword(
            password = key.trim(),
            salt = saltSetting.value,
            expectedHash = hashSetting.value
        )
    }

    /**
     * Auto-lock timeout: how long the app may sit in background before requiring re-auth.
     */
    suspend fun getAutoLockTimeoutMs(): Long {
        return database.getSetting(SettingsKeys.AUTO_LOCK_TIMEOUT_MS)?.value?.toLongOrNull()
            ?: DEFAULT_AUTO_LOCK_TIMEOUT_MS
    }

    suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
        database.saveSetting(
            SettingsEntity(
                key = SettingsKeys.AUTO_LOCK_TIMEOUT_MS,
                value = timeoutMs.toString()
            )
        )
    }

    /**
     * Delete the vault (resets everything).
     * WARNING: This will make all data inaccessible.
     */
    suspend fun deleteVault() {
        database.deleteSetting(SettingsKeys.MASTER_PASSWORD_SALT)
        database.deleteSetting(SettingsKeys.MASTER_PASSWORD_HASH)
        database.deleteSetting(SettingsKeys.VAULT_INITIALIZED)
        database.deleteSetting(SettingsKeys.BIOMETRIC_ENABLED)
        database.deleteSetting(SettingsKeys.AUTO_LOCK_TIMEOUT_MS)
        database.deleteSetting(SettingsKeys.RECOVERY_KEY_SALT)
        database.deleteSetting(SettingsKeys.RECOVERY_KEY_HASH)
    }

    companion object {
        const val DEFAULT_AUTO_LOCK_TIMEOUT_MS = 60_000L
    }
}
