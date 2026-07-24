package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeCode: String,
    val employeeName: String,
    val department: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // CHECK_IN or CHECK_OUT
    val latitude: Double,
    val longitude: Double,
    val officeName: String,
    val distanceMeters: Double,
    val inGeofence: Boolean,
    val verificationMethod: String, // 2FA_TOTP, SECURITY_PIN, BIOMETRIC_2FA
    val isLate: Boolean = false,
    val deviceToken: String = "",
    val securityFlags: String = "VERIFIED_2FA"
)
