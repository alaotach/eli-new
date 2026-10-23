package com.alaotach.eli5.di

import android.content.Context
import androidx.room.Room
import com.alaotach.eli5.data.local.AppDatabase
import com.alaotach.eli5.data.local.HistoryDao
import com.alaotach.eli5.data.remote.OpenRouterApi
import com.alaotach.eli5.data.repository.AppRepository
import com.alaotach.eli5.data.remote.RetrofitClient
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent.class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "eli5-database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }
}

@Module
@InstallIn(SingletonComponent.class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOpenRouterApi(): OpenRouterApi {
        return RetrofitClient.api
    }
}

@Module
@InstallIn(SingletonComponent.class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAppRepository(
        api: OpenRouterApi,
        historyDao: HistoryDao
    ): AppRepository {
        return AppRepository(api, historyDao)
    }
}