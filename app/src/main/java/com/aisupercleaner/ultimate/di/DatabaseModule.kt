package com.aisupercleaner.ultimate.di

import android.content.Context
import androidx.room.Room
import com.aisupercleaner.ultimate.data.local.database.AppDatabase
import com.aisupercleaner.ultimate.data.local.database.dao.DuplicateDao
import com.aisupercleaner.ultimate.data.local.database.dao.HistoryDao
import com.aisupercleaner.ultimate.data.local.database.dao.VaultDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_super_cleaner.db",
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideDuplicateDao(db: AppDatabase): DuplicateDao = db.duplicateDao()

    @Provides
    @Singleton
    fun provideVaultDao(db: AppDatabase): VaultDao = db.vaultDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()
}
