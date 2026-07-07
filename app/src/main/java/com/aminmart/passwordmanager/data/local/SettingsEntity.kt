package com.aminmart.passwordmanager.data.local

/**
 * Settings entity for storing app configuration.
 */
data class SettingsEntity(
    val id: Long = 0,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Settings keys.
 */
object SettingsKeys {
    const val MASTER_PASSWORD_SALT = "master_password_salt"
    const val MASTER_PASSWORD_HASH = "master_password_hash"
    const val BIOMETRIC_ENABLED = "biometric_enabled"
    const val VAULT_INITIALIZED = "vault_initialized"
    const val AUTO_LOCK_TIMEOUT_MS = "auto_lock_timeout_ms"
    const val RECOVERY_KEY_SALT = "recovery_key_salt"
    const val RECOVERY_KEY_HASH = "recovery_key_hash"
}
