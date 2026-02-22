package com.aminmart.passwordmanager.di

import android.content.Context
import androidx.room.Room
import com.aminmart.passwordmanager.data.local.PasswordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePasswordDatabase(
        @ApplicationContext context: Context
    ): PasswordDatabase {
        return Room.databaseBuilder(
            context,
            PasswordDatabase::class.java,
            "passwords.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePasswordDao(database: PasswordDatabase): PasswordDatabase.PasswordDao {
        return database.passwordDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: PasswordDatabase): PasswordDatabase.SettingsDao {
        return database.settingsDao()
    }
}
