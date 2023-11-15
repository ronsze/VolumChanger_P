package com.sdbk.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sdbk.domain.location.LocationEntity

@Database(entities = [LocationEntity::class], version = 1)
abstract class LocationDatabase: RoomDatabase() {
    abstract fun locationDao(): com.sdbk.domain.dao.location.LocationDao
}