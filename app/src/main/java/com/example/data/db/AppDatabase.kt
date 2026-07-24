package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AttendanceRecordDao
import com.example.data.dao.EmployeeDao
import com.example.data.dao.OfficeLocationDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.OfficeLocation
import com.example.security.DeviceFingerprint
import com.example.security.TotpGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Employee::class, OfficeLocation::class, AttendanceRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun officeLocationDao(): OfficeLocationDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_2fa_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                val officeDao = db.officeLocationDao()
                val employeeDao = db.employeeDao()
                val recordDao = db.attendanceRecordDao()

                val officeHq = OfficeLocation(
                    id = 1,
                    name = "HQ Tech Park",
                    address = "100 Innovation Way, Suite 400",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    radiusMeters = 150.0,
                    shiftStartTime = "09:00",
                    active = true
                )
                val officeBranch = OfficeLocation(
                    id = 2,
                    name = "Downtown Tech Hub",
                    address = "550 Market Street",
                    latitude = 37.7892,
                    longitude = -122.4014,
                    radiusMeters = 200.0,
                    shiftStartTime = "08:30",
                    active = true
                )

                officeDao.insertOffice(officeHq)
                officeDao.insertOffice(officeBranch)

                val emp1 = Employee(
                    id = 1,
                    employeeCode = "EMP-1001",
                    name = "Alex Mercer",
                    department = "Engineering",
                    email = "alex.mercer@company.com",
                    role = "EMPLOYEE",
                    phone = "+1 (555) 019-2831",
                    deviceToken = DeviceFingerprint.getDeviceToken(),
                    totpSecret = TotpGenerator.generateRandomSeed(),
                    securityPin = "1234",
                    officeId = 1
                )

                val emp2 = Employee(
                    id = 2,
                    employeeCode = "HR-2001",
                    name = "Sarah Jenkins",
                    department = "Human Resources",
                    email = "sarah.jenkins@company.com",
                    role = "HR_ADMIN",
                    phone = "+1 (555) 014-9988",
                    deviceToken = DeviceFingerprint.getDeviceToken(),
                    totpSecret = TotpGenerator.generateRandomSeed(),
                    securityPin = "9999",
                    officeId = 1
                )

                val emp3 = Employee(
                    id = 3,
                    employeeCode = "EMP-1002",
                    name = "Marcus Vance",
                    department = "Sales",
                    email = "marcus.vance@company.com",
                    role = "EMPLOYEE",
                    phone = "+1 (555) 018-4422",
                    deviceToken = "DEV-88B112AA",
                    totpSecret = TotpGenerator.generateRandomSeed(),
                    securityPin = "5678",
                    officeId = 2
                )

                employeeDao.insertEmployee(emp1)
                employeeDao.insertEmployee(emp2)
                employeeDao.insertEmployee(emp3)

                // Add sample historical check-in records
                val now = System.currentTimeMillis()
                val hourMillis = 3600 * 1000L

                recordDao.insertRecord(
                    AttendanceRecord(
                        employeeId = 1,
                        employeeCode = "EMP-1001",
                        employeeName = "Alex Mercer",
                        department = "Engineering",
                        timestamp = now - (2 * hourMillis),
                        type = "CHECK_IN",
                        latitude = 37.7750,
                        longitude = -122.4193,
                        officeName = "HQ Tech Park",
                        distanceMeters = 18.5,
                        inGeofence = true,
                        verificationMethod = "2FA_TOTP",
                        isLate = false,
                        deviceToken = emp1.deviceToken,
                        securityFlags = "2FA_VERIFIED | GEOFENCE_MATCH"
                    )
                )

                recordDao.insertRecord(
                    AttendanceRecord(
                        employeeId = 3,
                        employeeCode = "EMP-1002",
                        employeeName = "Marcus Vance",
                        department = "Sales",
                        timestamp = now - (1 * hourMillis + 15 * 60 * 1000L),
                        type = "CHECK_IN",
                        latitude = 37.7910,
                        longitude = -122.4030,
                        officeName = "Downtown Tech Hub",
                        distanceMeters = 240.0,
                        inGeofence = false,
                        verificationMethod = "SECURITY_PIN",
                        isLate = true,
                        deviceToken = "DEV-88B112AA",
                        securityFlags = "2FA_VERIFIED | OUT_OF_GEOFENCE"
                    )
                )
            }
        }
    }
}
