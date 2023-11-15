package com.sdbk.data.hilt

import com.sdbk.data.repository.location.LocalLocationDataSource
import com.sdbk.data.repository.location.LocationRepositoryImpl
import com.sdbk.domain.repository.location.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {
    @Provides
    @Singleton
    fun provideLocationRepository(localLocationDataSource: LocalLocationDataSource): LocationRepository =
        LocationRepositoryImpl(localLocationDataSource)
}