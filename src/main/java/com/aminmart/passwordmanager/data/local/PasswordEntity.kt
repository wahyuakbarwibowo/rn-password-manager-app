package com.aminmart.passwordmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Password entry entity stored in the database.
 * Sensitive fields (password, notes) are stored encrypted in ciphertext/nonce.
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    val username: String,
    val website: String,
    val category: PasswordCategory,
    
    // Encrypted fields - stored as base64 strings
    val ciphertext: String? = null,
    val nonce: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Categories for organizing passwords.
 */
enum class PasswordCategory {
    SOCIAL,
    EMAIL,
    SHOPPING,
    FINANCE,
    ENTERTAINMENT,
    WORK,
    OTHER
}
