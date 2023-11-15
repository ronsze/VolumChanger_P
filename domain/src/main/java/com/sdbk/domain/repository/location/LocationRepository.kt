package com.sdbk.domain.repository.location

import com.sdbk.domain.location.LocationEntity

interface LocationRepository {
    suspend fun getLocationAll(): List<LocationEntity>
    suspend fun getLocation(id: Int): LocationEntity
    suspend fun insertLocation(location: LocationEntity): LocationEntity
    suspend fun deleteLocation(location: LocationEntity)
}