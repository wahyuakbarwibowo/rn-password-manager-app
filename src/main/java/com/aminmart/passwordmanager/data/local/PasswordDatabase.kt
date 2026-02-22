package com.aminmart.passwordmanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room database for the password manager.
 */
@Database(
    entities = [PasswordEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PasswordCategoryConverter::class)
abstract class PasswordDatabase : RoomDatabase() {
    
    abstract fun passwordDao(): PasswordDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "passwords.db"
    }
}

/**
 * Type converter for PasswordCategory enum.
 */
class PasswordCategoryConverter {
    @TypeConverter
    fun fromCategory(category: PasswordCategory): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): PasswordCategory {
        return PasswordCategory.valueOf(value)
    }
}

/**
 * Data Access Object for password operations.
 */
@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAllPasswordsList(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntity?

    @Query("SELECT * FROM passwords WHERE title LIKE :query OR username LIKE :query OR website LIKE :query OR category LIKE :query ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity): Long

    @Update
    suspend fun updatePassword(password: PasswordEntity)

    @Delete
    suspend fun deletePassword(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: Long)

    @Query("SELECT * FROM passwords WHERE ciphertext IS NOT NULL AND nonce IS NOT NULL")
    suspend fun getAllEncryptedPasswords(): List<PasswordEntity>
}

/**
 * Data Access Object for settings operations.
 */
@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): SettingsEntity?

    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingsEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Query("SELECT EXISTS(SELECT 1 FROM settings WHERE `key` = :key)")
    suspend fun hasSetting(key: String): Boolean
}
