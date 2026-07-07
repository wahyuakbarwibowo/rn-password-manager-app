package com.aminmart.passwordmanager.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.aminmart.passwordmanager.data.local.PasswordDatabase
import com.aminmart.passwordmanager.data.local.PasswordEntity
import com.aminmart.passwordmanager.data.local.PasswordCategory
import com.aminmart.passwordmanager.data.security.EncryptedData
import com.aminmart.passwordmanager.data.security.EncryptionService
import com.aminmart.passwordmanager.domain.model.CreatePasswordInput
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for backup and restore operations.
 * Backups are encrypted using the master password.
 */
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PasswordDatabase,
    private val encryptionService: EncryptionService,
    private val passwordRepository: PasswordRepository
) {

    companion object {
        private const val BACKUP_VERSION = 2
        private const val PBKDF2_ITERATIONS = 100000
        private const val SALT_LENGTH = 32
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val secureRandom = java.security.SecureRandom()

    /**
     * Export all passwords to a backup file.
     *
     * Version 2 format: the payload holds plaintext entries and is encrypted
     * with a key derived from the master password (PBKDF2 + AES-GCM), so the
     * backup can be restored on any device — unlike v1, which was tied to
     * this device's Keystore key.
     */
    suspend fun exportBackup(
        masterPassword: String,
        uri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val passwords = passwordRepository.getAllPasswordsList()

            if (passwords.isEmpty()) {
                return@withContext BackupResult.Error("No passwords to backup")
            }

            val passwordsArray = JSONArray()
            passwords.forEach { entry ->
                passwordsArray.put(
                    JSONObject()
                        .put("title", entry.title)
                        .put("username", entry.username)
                        .put("password", entry.password)
                        .put("website", entry.website)
                        .put("notes", entry.notes)
                        .put("category", entry.category.name)
                        .put("createdAt", entry.createdAt)
                        .put("updatedAt", entry.updatedAt)
                )
            }

            val payloadBytes = JSONObject()
                .put("version", BACKUP_VERSION)
                .put("createdAt", System.currentTimeMillis())
                .put("passwords", passwordsArray)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)

            // Encrypt payload with a key derived from the master password
            val salt = ByteArray(SALT_LENGTH).apply { secureRandom.nextBytes(this) }
            val key = deriveKeyFromPassword(masterPassword, salt)
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
            val nonce = cipher.iv
            val ciphertext = cipher.doFinal(payloadBytes)

            val backupJson = JSONObject()
                .put("version", BACKUP_VERSION)
                .put("createdAt", System.currentTimeMillis())
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .put("nonce", Base64.encodeToString(nonce, Base64.NO_WRAP))
                .toString()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backupJson.toByteArray(StandardCharsets.UTF_8))
            } ?: return@withContext BackupResult.Error("Failed to write backup file")

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
            val backupObj = try {
                JSONObject(backupJson)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Invalid backup file format")
            }

            val version = backupObj.optInt("version", -1)
            val salt = Base64.decode(backupObj.getString("salt"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(backupObj.getString("ciphertext"), Base64.NO_WRAP)
            val nonce = Base64.decode(backupObj.getString("nonce"), Base64.NO_WRAP)

            val payloadBytes = try {
                when (version) {
                    // v2: key derived from the password the backup was created with
                    2 -> {
                        val key = deriveKeyFromPassword(masterPassword, salt)
                        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(
                            javax.crypto.Cipher.DECRYPT_MODE,
                            key,
                            javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, nonce)
                        )
                        cipher.doFinal(ciphertext)
                    }
                    // v1 (legacy): encrypted with this device's Keystore key,
                    // only restorable on the same device/installation
                    1 -> encryptionService.decrypt(EncryptedData(ciphertext = ciphertext, nonce = nonce))
                    else -> return@withContext BackupResult.Error("Unsupported backup version")
                }
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Wrong password or corrupted backup")
            }

            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            val payloadObj = try {
                JSONObject(payloadJson)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Failed to parse backup payload")
            }

            // Import passwords
            var imported = 0
            var skipped = 0

            when (mode) {
                ImportMode.OVERWRITE -> {
                    // Delete all existing passwords
                    database.getAllPasswordsList().forEach { entity ->
                        database.deletePassword(entity)
                    }
                }
                ImportMode.MERGE -> {
                    // Will skip duplicates
                }
            }

            val passwordsArray = payloadObj.getJSONArray("passwords")
            for (i in 0 until passwordsArray.length()) {
                try {
                    val item = passwordsArray.getJSONObject(i)
                    val title = item.getString("title")
                    val username = item.getString("username")
                    val website = item.getString("website")
                    val category = item.getString("category")
                    val createdAt = item.getLong("createdAt")
                    val updatedAt = item.getLong("updatedAt")

                    // Check for duplicates in merge mode
                    if (mode == ImportMode.MERGE) {
                        val existing = database.getAllPasswordsList()
                            .find { it.title == title && it.username == username && it.website == website }
                        if (existing != null) {
                            skipped++
                            continue
                        }
                    }

                    if (version == 2) {
                        // Plaintext entry: re-encrypt with this device's key
                        passwordRepository.createPassword(
                            CreatePasswordInput(
                                title = title,
                                username = username,
                                password = item.getString("password"),
                                website = website,
                                notes = item.optString("notes"),
                                category = com.aminmart.passwordmanager.domain.model.PasswordCategory.valueOf(category),
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                    } else {
                        // v1: entry ciphertext already encrypted with this device's Keystore key
                        val ciphertext1 = item.getString("ciphertext")
                        database.insertPassword(
                            PasswordEntity(
                                title = title,
                                username = username,
                                passwordEncrypted = ciphertext1,
                                website = website,
                                notesEncrypted = "",
                                category = PasswordCategory.valueOf(category),
                                icon = "",
                                ciphertext = ciphertext1,
                                nonce = item.getString("nonce"),
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                    }
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
     * Import passwords from a plain (unencrypted) JSON file.
     *
     * Accepts either a top-level array or an object with a "passwords" array.
     * Each item needs "title" and "password"; "username", "website", "notes",
     * and "category" are optional. Always merges: duplicates
     * (same title + username + website) are skipped.
     */
    suspend fun importJson(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext BackupResult.Error("Failed to read file")

            val passwordsArray = try {
                val trimmed = text.trim()
                if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).getJSONArray("passwords")
            } catch (e: Exception) {
                return@withContext BackupResult.Error("Invalid JSON format")
            }

            val existing = database.getAllPasswordsList()
            var imported = 0
            var skipped = 0

            for (i in 0 until passwordsArray.length()) {
                val item = passwordsArray.optJSONObject(i)
                val title = item?.optString("title").orEmpty()
                val password = item?.optString("password").orEmpty()
                if (item == null || title.isBlank() || password.isEmpty()) {
                    skipped++
                    continue
                }

                val username = item.optString("username")
                val website = item.optString("website")
                val isDuplicate = existing.any {
                    it.title == title && it.username == username && it.website == website
                }
                if (isDuplicate) {
                    skipped++
                    continue
                }

                val category = runCatching {
                    com.aminmart.passwordmanager.domain.model.PasswordCategory.valueOf(
                        item.optString("category").uppercase()
                    )
                }.getOrDefault(com.aminmart.passwordmanager.domain.model.PasswordCategory.OTHER)

                passwordRepository.createPassword(
                    CreatePasswordInput(
                        title = title,
                        username = username,
                        password = password,
                        website = website,
                        notes = item.optString("notes"),
                        category = category
                    )
                )
                imported++
            }

            BackupResult.Success(imported = imported, skipped = skipped)
        } catch (e: Exception) {
            BackupResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Derive an AES key from the backup password using PBKDF2.
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): javax.crypto.SecretKey {
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            256
        )
        try {
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return javax.crypto.spec.SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}

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
