package com.aminmart.passwordmanager.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Service for password hashing and verification.
 * Uses PBKDF2 with SHA-256 for secure password hashing.
 */
class PasswordHashingService {

    companion object {
        private const val PBKDF2_ITERATIONS = 100000
        private const val SALT_LENGTH = 32 // 256 bits
        private const val HASH_LENGTH = 32 // 256 bits
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    private val secureRandom = SecureRandom()

    /**
     * Hash a master password with a random salt.
     * Returns the salt and hash for storage.
     */
    fun hashPassword(password: String): PasswordHash {
        val salt = ByteArray(SALT_LENGTH).apply {
            secureRandom.nextBytes(this)
        }

        val hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_LENGTH * 8)

        return PasswordHash(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            hash = Base64.encodeToString(hash, Base64.NO_WRAP)
        )
    }

    /**
     * Verify a password against a stored salt and hash.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val expectedHashBytes = Base64.decode(expectedHash, Base64.NO_WRAP)

        val computedHash = pbkdf2(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, HASH_LENGTH * 8)

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(computedHash, expectedHashBytes)
    }

    private fun pbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        outputLength: Int
    ): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, outputLength)
        try {
            val factory = javax.crypto.SecretKeyFactory.getInstance(ALGORITHM)
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

/**
 * Stored password hash with salt.
 */
data class PasswordHash(
    val salt: String,
    val hash: String
)
