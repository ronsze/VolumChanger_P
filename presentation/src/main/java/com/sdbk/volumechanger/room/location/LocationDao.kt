package com.sdbk.volumechanger.room.location

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationDao {
    @Query("SELECT * FROM Location")
    fun getAll(): List<Location>

    @Query("SELECT * FROM Location WHERE latLng == (:input)")
    fun getLocationBy(input: String): List<Location>

    @Insert
    fun insertLocation(location: Location)

    @Delete
    fun deleteLocationBy(location: Location)
}