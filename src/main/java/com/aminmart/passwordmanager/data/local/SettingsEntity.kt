package com.aminmart.passwordmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings entity for storing app configuration.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
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
}
