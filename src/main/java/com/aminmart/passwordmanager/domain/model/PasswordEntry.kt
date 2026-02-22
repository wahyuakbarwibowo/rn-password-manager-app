package com.aminmart.passwordmanager.domain.model

/**
 * Domain model for a password entry.
 * This is the decrypted version used by the UI.
 */
data class PasswordEntry(
    val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val category: PasswordCategory,
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

/**
 * Input for creating a new password entry.
 */
data class CreatePasswordInput(
    val title: String,
    val username: String = "",
    val password: String,
    val website: String = "",
    val notes: String = "",
    val category: PasswordCategory = PasswordCategory.OTHER
)

/**
 * Input for updating a password entry.
 */
data class UpdatePasswordInput(
    val id: Long,
    val title: String? = null,
    val username: String? = null,
    val password: String? = null,
    val website: String? = null,
    val notes: String? = null,
    val category: PasswordCategory? = null
)
