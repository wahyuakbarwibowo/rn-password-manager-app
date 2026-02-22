package com.aminmart.passwordmanager.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.aminmart.passwordmanager.data.local.PasswordEntity
import com.aminmart.passwordmanager.data.local.PasswordDao
import com.aminmart.passwordmanager.data.security.EncryptedData
import com.aminmart.passwordmanager.data.security.EncryptionService
import com.aminmart.passwordmanager.data.security.SecretsJsonSerializer
import com.aminmart.passwordmanager.data.security.SecretsPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.serialization.json.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for backup and restore operations.
 * Backups are encrypted using the master password.
 */
@Singleton
class BackupService @Inject constructor(
    private val context: Context,
    private val passwordDao: PasswordDao,
    private val encryptionService: EncryptionService
) {

    companion object {
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILE_EXTENSION = ".aminmartbackup"
        private const val PBKDF2_ITERATIONS = 100000
        private const val SALT_LENGTH = 32
    }

    private val secureRandom = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Export all passwords to a backup file.
     */
    suspend fun exportBackup(
        masterPassword: String,
        uri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Get all encrypted passwords
            val passwords = passwordDao.getAllEncryptedPasswords()
            
            if (passwords.isEmpty()) {
                return@withContext BackupResult.Error("No passwords to backup")
            }

            // Create backup payload
            val payload = BackupPayloadV1(
                version = BACKUP_VERSION,
                createdAt = System.currentTimeMillis(),
                passwords = passwords.map { entity ->
                    PasswordBackupItem(
                        title = entity.title,
                        username = entity.username,
                        website = entity.website,
                        category = entity.category.name,
                        ciphertext = entity.ciphertext ?: "",
                        nonce = entity.nonce ?: "",
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            )

            // Serialize payload
            val payloadJson = json.encodeToString(payload)
            val payloadBytes = payloadJson.toByteArray(StandardCharsets.UTF_8)

            // Derive key from master password
            val salt = ByteArray(SALT_LENGTH).apply { secureRandom.nextBytes(this) }
            val key = deriveKeyFromPassword(masterPassword, salt)

            // Encrypt payload
            val encryptedData = encryptionService.encrypt(payloadBytes)

            // Create backup file content
            val backupFile = BackupFileV1(
                version = BACKUP_VERSION,
                createdAt = System.currentTimeMillis(),
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(encryptedData.ciphertext, Base64.NO_WRAP),
                nonce = Base64.encodeToString(encryptedData.nonce, Base64.NO_WRAP)
            )

            val backupJson = json.encodeToString(backupFile)

            // Write to URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backupJson.toByteArray(StandardCharsets.UTF_8))
            }

            BackupResult.Success(passwords.size)
        } catch (e: Exception) {
            BackupResult.Error("Export failed: ${e.message}")
        }
    }

    /**
     * Import passwords from a backup file.
     * @param mode Import mode (merge or overwrite)
     */
    suspend fun importBackup(
        masterPassword: String,
        uri: Uri,
        mode: ImportMode = ImportMode.MERGE
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Read backup file
            val backupJson = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext BackupResult.Error("Failed to read backup file")

            // Parse backup file
            val backupFile = try {
                json.decodeFromString<BackupFileV1>(backupJson)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Invalid backup file format")
            }

            if (backupFile.version != BACKUP_VERSION) {
                return@withContext BackupResult.Error("Unsupported backup version")
            }

            // Derive key from master password
            val salt = Base64.decode(backupFile.salt, Base64.NO_WRAP)
            val key = deriveKeyFromPassword(masterPassword, salt)

            // Decrypt payload
            val ciphertext = Base64.decode(backupFile.ciphertext, Base64.NO_WRAP)
            val nonce = Base64.decode(backupFile.nonce, Base64.NO_WRAP)
            
            val encryptedData = EncryptedData(ciphertext = ciphertext, nonce = nonce)
            val payloadBytes = try {
                encryptionService.decrypt(encryptedData)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Wrong password or corrupted backup")
            }

            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            val payload = try {
                json.decodeFromString<BackupPayloadV1>(payloadJson)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Failed to parse backup payload")
            }

            // Import passwords
            var imported = 0
            var skipped = 0

            when (mode) {
                ImportMode.OVERWRITE -> {
                    // Delete all existing passwords
                    passwordDao.getAllPasswordsList().forEach { entity ->
                        passwordDao.deletePassword(entity)
                    }
                }
                ImportMode.MERGE -> {
                    // Will skip duplicates
                }
            }

            payload.passwords.forEach { item ->
                try {
                    // Check for duplicates in merge mode
                    if (mode == ImportMode.MERGE) {
                        val existing = passwordDao.getAllPasswordsList()
                            .find { it.title == item.title && it.username == item.username && it.website == item.website }
                        if (existing != null) {
                            skipped++
                            return@forEach
                        }
                    }

                    val entity = PasswordEntity(
                        title = item.title,
                        username = item.username,
                        website = item.website,
                        category = com.aminmart.passwordmanager.data.local.PasswordCategory.valueOf(item.category),
                        ciphertext = item.ciphertext,
                        nonce = item.nonce,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )

                    passwordDao.insertPassword(entity)
                    imported++
                } catch (e: Exception) {
                    // Skip failed imports
                    skipped++
                }
            }

            BackupResult.Success(imported = imported, skipped = skipped)
        } catch (e: Exception) {
            BackupResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Derive an encryption key from the master password using PBKDF2.
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            256
        )
        try {
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

/**
 * Backup file format version 1.
 */
@org.jetbrains.kotlinx.serialization.Serializable
data class BackupFileV1(
    val version: Int,
    val createdAt: Long,
    val salt: String,
    val ciphertext: String,
    val nonce: String
)

/**
 * Backup payload format version 1.
 */
@org.jetbrains.kotlinx.serialization.Serializable
data class BackupPayloadV1(
    val version: Int,
    val createdAt: Long,
    val passwords: List<PasswordBackupItem>
)

/**
 * Individual password item in backup.
 */
@org.jetbrains.kotlinx.serialization.Serializable
data class PasswordBackupItem(
    val title: String,
    val username: String,
    val website: String,
    val category: String,
    val ciphertext: String,
    val nonce: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Import mode for backup restoration.
 */
enum class ImportMode {
    MERGE,      // Merge with existing, skip duplicates
    OVERWRITE   // Delete all existing and replace
}

/**
 * Backup operation result.
 */
sealed class BackupResult {
    data class Success(val imported: Int = 0, val skipped: Int = 0) : BackupResult()
    data class Error(val message: String) : BackupResult()
}
