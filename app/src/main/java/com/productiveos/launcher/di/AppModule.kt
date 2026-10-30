package com.productiveos.launcher.di

import android.content.Context
import androidx.room.Room
import com.productiveos.launcher.data.local.AppDao
import com.productiveos.launcher.data.local.AppDatabase
import com.productiveos.launcher.data.local.MicroOsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "productive_os.db"
        )
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    @Singleton
    fun provideMicroOsDao(database: AppDatabase): MicroOsDao {
        return database.microOsDao()
    }
}
