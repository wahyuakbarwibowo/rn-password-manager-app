package com.aminmart.passwordmanager.data.repository

import com.aminmart.passwordmanager.data.local.PasswordCategoryConverter
import com.aminmart.passwordmanager.data.local.PasswordDao
import com.aminmart.passwordmanager.data.local.PasswordEntity
import com.aminmart.passwordmanager.data.security.SecretEncryptionService
import com.aminmart.passwordmanager.domain.model.CreatePasswordInput
import com.aminmart.passwordmanager.domain.model.PasswordCategory
import com.aminmart.passwordmanager.domain.model.PasswordEntry
import com.aminmart.passwordmanager.domain.model.UpdatePasswordInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for password operations.
 * Handles encryption/decryption of sensitive data.
 */
@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao,
    private val secretEncryptionService: SecretEncryptionService
) {

    /**
     * Get all passwords as a Flow.
     * Passwords are decrypted when read.
     */
    fun getAllPasswords(): Flow<List<PasswordEntry>> {
        return passwordDao.getAllPasswords().map { entities ->
            entities.map { decryptPassword(it) }
        }
    }

    /**
     * Search passwords by query.
     */
    fun searchPasswords(query: String): Flow<List<PasswordEntry>> {
        val searchQuery = "%$query%"
        return passwordDao.searchPasswords(searchQuery).map { entities ->
            entities.map { decryptPassword(it) }
        }
    }

    /**
     * Get a password by ID.
     */
    suspend fun getPasswordById(id: Long): PasswordEntry? {
        val entity = passwordDao.getPasswordById(id)
        return entity?.let { decryptPassword(it) }
    }

    /**
     * Create a new password entry.
     * Password and notes are encrypted before storage.
     */
    suspend fun createPassword(input: CreatePasswordInput): Long {
        val encryptedSecrets = secretEncryptionService.encryptSecrets(
            password = input.password,
            notes = input.notes
        )

        val entity = PasswordEntity(
            title = input.title,
            username = input.username,
            website = input.website,
            category = input.category.toEntityCategory(),
            ciphertext = encryptedSecrets.ciphertext,
            nonce = encryptedSecrets.nonce
        )

        return passwordDao.insertPassword(entity)
    }

    /**
     * Update an existing password entry.
     */
    suspend fun updatePassword(input: UpdatePasswordInput) {
        val existing = passwordDao.getPasswordById(input.id)
            ?: throw IllegalArgumentException("Password not found with id: ${input.id}")

        val title = input.title ?: existing.title
        val username = input.username ?: existing.username
        val password = input.password ?: decryptPassword(existing).password
        val website = input.website ?: existing.website
        val notes = input.notes ?: decryptPassword(existing).notes
        val category = input.category ?: existing.category.toDomainCategory()

        val encryptedSecrets = secretEncryptionService.encryptSecrets(
            password = password,
            notes = notes
        )

        val updated = existing.copy(
            title = title,
            username = username,
            website = website,
            category = category.toEntityCategory(),
            ciphertext = encryptedSecrets.ciphertext,
            nonce = encryptedSecrets.nonce,
            updatedAt = System.currentTimeMillis()
        )

        passwordDao.updatePassword(updated)
    }

    /**
     * Delete a password entry.
     */
    suspend fun deletePassword(id: Long) {
        passwordDao.deletePasswordById(id)
    }

    /**
     * Delete all passwords.
     */
    suspend fun deleteAllPasswords() {
        passwordDao.getAllPasswordsList().forEach { entity ->
            passwordDao.deletePassword(entity)
        }
    }

    /**
     * Decrypt a password entity to a domain model.
     */
    private fun decryptPassword(entity: PasswordEntity): PasswordEntry {
        val (password, notes) = if (!entity.ciphertext.isNullOrEmpty() && !entity.nonce.isNullOrEmpty()) {
            try {
                val payload = secretEncryptionService.decryptSecrets(
                    ciphertext = entity.ciphertext,
                    nonce = entity.nonce
                )
                payload.password to payload.notes
            } catch (e: Exception) {
                // If decryption fails, return empty strings
                // In production, you might want to log this
                "" to ""
            }
        } else {
            "" to ""
        }

        return PasswordEntry(
            id = entity.id,
            title = entity.title,
            username = entity.username,
            password = password,
            website = entity.website,
            notes = notes,
            category = entity.category.toDomainCategory(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}

/**
 * Extension functions to convert between domain and entity categories.
 */
private fun PasswordCategory.toEntityCategory(): com.aminmart.passwordmanager.data.local.PasswordCategory {
    return com.aminmart.passwordmanager.data.local.PasswordCategory.valueOf(this.name)
}

private fun com.aminmart.passwordmanager.data.local.PasswordCategory.toDomainCategory(): PasswordCategory {
    return PasswordCategory.valueOf(this.name)
}
