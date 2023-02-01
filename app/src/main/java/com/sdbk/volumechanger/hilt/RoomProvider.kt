package com.sdbk.volumechanger.hilt

import android.content.Context
import androidx.room.Room
import com.sdbk.volumechanger.room.location.LocationDao
import com.sdbk.volumechanger.room.location.LocationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RoomProvider {
    @Provides
    @Singleton
    fun provideLocationDatabase(@ApplicationContext context: Context): LocationDatabase =
        Room.databaseBuilder(
            context,
            LocationDatabase::class.java, "location_database"
        ).build()

    @Provides
    @Singleton
    fun provideLocationDao(locationDatabase: LocationDatabase): LocationDao = locationDatabase.locationDao()
}