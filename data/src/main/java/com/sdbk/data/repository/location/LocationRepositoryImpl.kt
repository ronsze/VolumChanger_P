package com.sdbk.data.repository.location

import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.repository.location.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val localDataSource: LocalLocationDataSource
): LocationRepository {
    override suspend fun getLocationAll(): List<LocationEntity> = localDataSource.getLocationAll()
    override suspend fun getLocation(id: Int): LocationEntity = localDataSource.getLocation(id)
    override suspend fun insertLocation(location: LocationEntity) = localDataSource.insertLocation(location)
    override suspend fun deleteLocation(location: LocationEntity) = localDataSource.deleteLocation(location)
}