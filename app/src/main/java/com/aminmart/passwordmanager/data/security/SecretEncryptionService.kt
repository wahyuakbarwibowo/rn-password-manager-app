package com.aminmart.passwordmanager.data.security

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for encrypting sensitive text data (passwords, notes).
 * Uses AES-256-GCM for authenticated encryption.
 */
@Singleton
class SecretEncryptionService @Inject constructor(
    private val encryptionService: EncryptionService
) {
    /**
     * Encrypt a secrets payload (password and notes).
     */
    fun encryptSecrets(password: String, notes: String): EncryptedSecrets {
        val json = JSONObject()
            .put("password", password)
            .put("notes", notes)
            .toString()
        val plaintext = json.toByteArray(StandardCharsets.UTF_8)
        
        val encryptedData = encryptionService.encrypt(plaintext)
        
        return EncryptedSecrets(
            ciphertext = Base64.encodeToString(encryptedData.ciphertext, Base64.NO_WRAP),
            nonce = Base64.encodeToString(encryptedData.nonce, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypt secrets payload.
     */
    fun decryptSecrets(ciphertext: String, nonce: String): SecretsPayload {
        val cipherBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
        val nonceBytes = Base64.decode(nonce, Base64.NO_WRAP)
        
        val encryptedData = EncryptedData(
            ciphertext = cipherBytes,
            nonce = nonceBytes
        )
        
        val plaintext = encryptionService.decrypt(encryptedData)
        val json = JSONObject(String(plaintext, StandardCharsets.UTF_8))

        return SecretsPayload(
            password = json.optString("password"),
            notes = json.optString("notes")
        )
    }
}

/**
 * Payload containing sensitive data.
 */
data class SecretsPayload(
    val password: String,
    val notes: String
)

/**
 * Encrypted secrets stored in the database.
 */
data class EncryptedSecrets(
    val ciphertext: String,
    val nonce: String
)
