package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_locations")
data class OfficeLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 150.0,
    val shiftStartTime: String = "09:00",
    val active: Boolean = true
)
