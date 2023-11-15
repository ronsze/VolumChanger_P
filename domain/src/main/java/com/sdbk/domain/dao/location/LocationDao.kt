package com.sdbk.domain.dao.location

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.sdbk.domain.location.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM Location")
    fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM Location WHERE latitude == (:latitude) AND longitude == (:longitude)")
    fun getLocationBy(latitude: Float, longitude: Float): List<LocationEntity>

    @Insert
    fun insertLocation(location: LocationEntity): LocationEntity

    @Delete
    fun deleteLocationBy(location: LocationEntity)
}