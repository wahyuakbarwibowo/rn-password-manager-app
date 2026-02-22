package com.aminmart.passwordmanager.data.security

import java.security.SecureRandom

/**
 * Service for generating secure random passwords.
 */
class PasswordGeneratorService {

    companion object {
        private const val DEFAULT_LENGTH = 16
        private const val MIN_LENGTH = 4
        private const val MAX_LENGTH = 128
        
        private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val DIGITS = "0123456789"
        private const val SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        
        private const val ALL_CHARS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL
    }

    private val secureRandom = SecureRandom()

    /**
     * Generate a random password.
     * 
     * @param length Password length (4-128 characters)
     * @param includeLowercase Include lowercase letters
     * @param includeUppercase Include uppercase letters
     * @param includeDigits Include digits
     * @param includeSpecial Include special characters
     * @return Generated password
     */
    fun generatePassword(
        length: Int = DEFAULT_LENGTH,
        includeLowercase: Boolean = true,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSpecial: Boolean = true
    ): String {
        val actualLength = length.coerceIn(MIN_LENGTH, MAX_LENGTH)
        
        // Build character set
        val charSet = buildString {
            if (includeLowercase) append(LOWERCASE)
            if (includeUppercase) append(UPPERCASE)
            if (includeDigits) append(DIGITS)
            if (includeSpecial) append(SPECIAL)
        }

        if (charSet.isEmpty()) {
            throw IllegalArgumentException("At least one character type must be selected")
        }

        // Ensure at least one character from each selected type
        val requiredChars = buildString {
            if (includeLowercase) append(LOWERCASE.random(secureRandom))
            if (includeUppercase) append(UPPERCASE.random(secureRandom))
            if (includeDigits) append(DIGITS.random(secureRandom))
            if (includeSpecial) append(SPECIAL.random(secureRandom))
        }

        // Generate remaining characters
        val remainingLength = if (actualLength > requiredChars.length) {
            actualLength - requiredChars.length
        } else {
            0
        }

        val randomChars = (1..remainingLength)
            .map { charSet.random(secureRandom) }
            .joinToString("")

        // Combine and shuffle
        val password = (requiredChars + randomChars)
            .shuffled(secureRandom)
            .joinToString("")

        return password.take(actualLength)
    }

    /**
     * Generate a memorable password (easier to remember but still secure).
     */
    fun generateMemorablePassword(
        wordCount: Int = 3,
        includeNumbers: Boolean = true,
        includeSpecial: Boolean = true
    ): String {
        val commonWords = listOf(
            "apple", "blue", "cloud", "dance", "eagle", "flame", "green", "house",
            "island", "jungle", "kite", "lemon", "mountain", "night", "ocean", "piano",
            "quiet", "river", "storm", "tree", "urban", "violet", "water", "xenon",
            "yellow", "zebra", "amber", "bronze", "crystal", "diamond", "emerald"
        )

        val words = (1..wordCount)
            .map { commonWords.random(secureRandom) }
            .joinToString("-")

        val suffix = when {
            includeNumbers && includeSpecial -> "${(100..999).random(secureRandom)}!"
            includeNumbers -> "${(100..999).random(secureRandom)}"
            includeSpecial -> "!"
            else -> ""
        }

        return words + suffix
    }

    /**
     * Calculate password entropy (bits).
     */
    fun calculateEntropy(password: String): Double {
        val charSetSize = when {
            password.all { it.isDigit() } -> 10.0
            password.all { it.isLowerCase() } -> 26.0
            password.all { it.isUpperCase() } -> 26.0
            password.all { it.isLetter() } -> 52.0
            password.all { it.isLetterOrDigit() } -> 62.0
            else -> ALL_CHARS.length.toDouble()
        }

        return password.length * kotlin.math.log2(charSetSize)
    }

    /**
     * Get password strength description based on entropy.
     */
    fun getPasswordStrength(entropy: Double): PasswordStrength {
        return when {
            entropy < 35 -> PasswordStrength.WEAK
            entropy < 50 -> PasswordStrength.FAIR
            entropy < 60 -> PasswordStrength.GOOD
            entropy < 80 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}

/**
 * Password strength levels.
 */
enum class PasswordStrength {
    WEAK,
    FAIR,
    GOOD,
    STRONG,
    VERY_STRONG
}
