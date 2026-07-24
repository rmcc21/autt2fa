package com.example.data.dao

import androidx.room.*
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getRecordsForEmployee(employeeId: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecordForEmployee(employeeId: Int): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :startOfDayTimestamp ORDER BY timestamp DESC")
    fun getTodayAttendanceRecords(startOfDayTimestamp: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}
