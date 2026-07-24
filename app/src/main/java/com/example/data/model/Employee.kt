package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeCode: String,
    val name: String,
    val department: String,
    val email: String,
    val role: String = "EMPLOYEE", // EMPLOYEE or HR_ADMIN
    val phone: String = "",
    val deviceToken: String = "",
    val totpSecret: String = "",
    val securityPin: String = "1234",
    val officeId: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
