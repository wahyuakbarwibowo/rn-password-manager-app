package com.aminmart.passwordmanager.data.security

import android.util.Base64
import androidx.security.crypto.Encryptor
import androidx.security.crypto.Decryptor
import java.nio.charset.StandardCharsets

/**
 * Service for encrypting sensitive text data (passwords, notes).
 * Uses AES-256-GCM for authenticated encryption.
 */
class SecretEncryptionService(
    private val encryptionService: EncryptionService
) {
    /**
     * Encrypt a secrets payload (password and notes).
     */
    fun encryptSecrets(password: String, notes: String): EncryptedSecrets {
        val payload = SecretsPayload(
            password = password,
            notes = notes
        )
        val json = SecretsJsonSerializer.serialize(payload)
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
        val json = String(plaintext, StandardCharsets.UTF_8)
        
        return SecretsJsonSerializer.deserialize(json)
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

/**
 * Simple JSON serializer for secrets payload.
 * Using manual serialization to avoid external dependencies.
 */
object SecretsJsonSerializer {
    
    fun serialize(payload: SecretsPayload): String {
        // Simple JSON escaping
        val escapedPassword = escapeJson(payload.password)
        val escapedNotes = escapeJson(payload.notes)
        return """{"password":"$escapedPassword","notes":"$escapedNotes"}"""
    }

    fun deserialize(json: String): SecretsPayload {
        // Simple JSON parsing (assumes well-formed JSON from our serializer)
        val password = extractJsonValue(json, "password")
        val notes = extractJsonValue(json, "notes")
        return SecretsPayload(
            password = unescapeJson(password),
            notes = unescapeJson(notes)
        )
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun extractJsonValue(json: String, key: String): String {
        val keyPattern = "\"$key\":"
        val startIndex = json.indexOf(keyPattern)
        if (startIndex == -1) return ""
        
        val valueStart = startIndex + keyPattern.length
        if (valueStart >= json.length) return ""
        
        // Check if value is a string (starts with quote)
        return if (json[valueStart] == '"') {
            extractQuotedString(json, valueStart + 1)
        } else {
            // Handle empty string or other edge cases
            ""
        }
    }

    private fun extractQuotedString(json: String, start: Int): String {
        val result = StringBuilder()
        var i = start
        while (i < json.length) {
            val char = json[i]
            when {
                char == '\\' && i + 1 < json.length -> {
                    // Escape sequence
                    result.append(char)
                    result.append(json[i + 1])
                    i += 2
                }
                char == '"' -> {
                    // End of string
                    break
                }
                else -> {
                    result.append(char)
                    i++
                }
            }
        }
        return result.toString()
    }
}
