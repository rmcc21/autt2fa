package com.example.data.dao

import androidx.room.*
import com.example.data.model.OfficeLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeLocationDao {
    @Query("SELECT * FROM office_locations WHERE active = 1 ORDER BY name ASC")
    fun getAllActiveOffices(): Flow<List<OfficeLocation>>

    @Query("SELECT * FROM office_locations WHERE id = :id")
    suspend fun getOfficeById(id: Int): OfficeLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffice(office: OfficeLocation): Long

    @Update
    suspend fun updateOffice(office: OfficeLocation)

    @Delete
    suspend fun deleteOffice(office: OfficeLocation)
}
