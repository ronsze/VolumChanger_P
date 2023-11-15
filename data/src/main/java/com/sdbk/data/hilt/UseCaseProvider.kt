package com.sdbk.data.hilt

import com.sdbk.data.usecase.DeleteLocationUseCaseImpl
import com.sdbk.data.usecase.GetLocationUseCaseImpl
import com.sdbk.data.usecase.InsertLocationUseCaseImpl
import com.sdbk.domain.repository.location.LocationRepository
import com.sdbk.domain.usecase.DeleteLocationUseCase
import com.sdbk.domain.usecase.GetLocationUseCase
import com.sdbk.domain.usecase.InsertLocationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseProvider {
    @Provides
    fun provideGetLocationUseCase(repository: LocationRepository): GetLocationUseCase = GetLocationUseCaseImpl(repository)
    @Provides
    fun provideInsertLocationUseCase(repository: LocationRepository): InsertLocationUseCase = InsertLocationUseCaseImpl(repository)
    @Provides
    fun provideDeleteLocationUseCase(repository: LocationRepository): DeleteLocationUseCase = DeleteLocationUseCaseImpl(repository)
}