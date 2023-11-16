package com.sdbk.data.repository.location

import com.sdbk.domain.dao.location.LocationDao
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.repository.location.LocationRepository
import javax.inject.Inject

class LocalLocationDataSource @Inject constructor(
    private val dao: LocationDao
): LocationRepository {
    override suspend fun getLocationAll(): List<LocationEntity> =
        dao.getLocationAll()

    override suspend fun getLocation(id: Int): LocationEntity =
        dao.getLocationById(id)

    override suspend fun insertLocation(location: LocationEntity): LocationEntity =
        location.apply { this.id = dao.insertLocation(location) }

    override suspend fun deleteLocation(location: LocationEntity) =
        dao.deleteLocation(location)
}