package com.wheelscreener.di

import android.content.Context
import androidx.room.Room
import com.wheelscreener.data.local.WheelScreenerDatabase
import com.wheelscreener.data.local.dao.ScanResultDao
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.remote.MarketDataProvider
import com.wheelscreener.data.remote.MockMarketDataProvider
import com.wheelscreener.data.repository.MarketDataRepository
import com.wheelscreener.data.repository.MarketDataRepositoryImpl
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
    fun provideWheelScreenerDatabase(
        @ApplicationContext context: Context
    ): WheelScreenerDatabase {
        return Room.databaseBuilder(
            context,
            WheelScreenerDatabase::class.java,
            "wheel_screener_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideWatchlistDao(database: WheelScreenerDatabase): WatchlistDao {
        return database.watchlistDao()
    }
    
    @Provides
    fun provideScanResultDao(database: WheelScreenerDatabase): ScanResultDao {
        return database.scanResultDao()
    }
    
    @Provides
    fun provideSettingsDao(database: WheelScreenerDatabase): SettingsDao {
        return database.settingsDao()
    }
    
    @Provides
    @Singleton
    fun provideMockMarketDataProvider(): MarketDataProvider {
        return MockMarketDataProvider()
    }
    
    @Provides
    @Singleton
    fun provideMarketDataRepository(
        marketDataProvider: MarketDataProvider
    ): MarketDataRepository {
        return MarketDataRepositoryImpl(marketDataProvider)
    }
}