package com.sdbk.data.hilt

import com.sdbk.data.repository.location.LocalLocationDataSource
import com.sdbk.domain.dao.location.LocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataSourceModule {
    @Provides
    @Singleton
    fun provideLocalLocationDataSource(locationDao: LocationDao): LocalLocationDataSource =
        LocalLocationDataSource(locationDao)
}