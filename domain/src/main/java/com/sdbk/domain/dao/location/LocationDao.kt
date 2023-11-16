package com.sdbk.domain.dao.location

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sdbk.domain.location.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM Location")
    suspend fun getLocationAll(): List<LocationEntity>

    @Query("SELECT * FROM Location WHERE id == (:id)")
    suspend fun getLocationById(id: Int): LocationEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}