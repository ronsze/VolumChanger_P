package com.sdbk.volumechanger.room.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Location", primaryKeys = ["latLng"])
data class Location(
    @ColumnInfo(name = "latLng") val latLng: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "range") val range: Int,
    @ColumnInfo(name = "bell_volume") val bellVolume: Int,
    @ColumnInfo(name = "media_volume") val mediaVolume: Int
): java.io.Serializable