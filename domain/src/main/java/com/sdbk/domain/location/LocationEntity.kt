package com.sdbk.domain.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Location")
data class LocationEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "range") val range: Int,
    @ColumnInfo(name = "bell_volume") val bellVolume: Int,
    @ColumnInfo(name = "media_volume") val mediaVolume: Int
): Serializable {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}