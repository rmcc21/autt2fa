package com.example.data.repository

import com.example.data.dao.AttendanceRecordDao
import com.example.data.dao.EmployeeDao
import com.example.data.dao.OfficeLocationDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.OfficeLocation
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AttendanceRepository(
    private val employeeDao: EmployeeDao,
    private val officeLocationDao: OfficeLocationDao,
    private val attendanceRecordDao: AttendanceRecordDao
) {
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    val allOffices: Flow<List<OfficeLocation>> = officeLocationDao.getAllActiveOffices()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceRecordDao.getAllAttendanceRecords()

    fun getRecordsForEmployee(employeeId: Int): Flow<List<AttendanceRecord>> {
        return attendanceRecordDao.getRecordsForEmployee(employeeId)
    }

    fun getLatestRecordForEmployee(employeeId: Int): Flow<AttendanceRecord?> {
        return attendanceRecordDao.getLatestRecordForEmployee(employeeId)
    }

    fun getTodayAttendanceRecords(): Flow<List<AttendanceRecord>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return attendanceRecordDao.getTodayAttendanceRecords(calendar.timeInMillis)
    }

    suspend fun getEmployeeById(id: Int): Employee? = employeeDao.getEmployeeById(id)
    suspend fun getOfficeById(id: Int): OfficeLocation? = officeLocationDao.getOfficeById(id)

    suspend fun saveEmployee(employee: Employee): Long {
        return employeeDao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) {
        employeeDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) {
        employeeDao.deleteEmployee(employee)
    }

    suspend fun saveOffice(office: OfficeLocation): Long {
        return officeLocationDao.insertOffice(office)
    }

    suspend fun updateOffice(office: OfficeLocation) {
        officeLocationDao.updateOffice(office)
    }

    suspend fun recordAttendance(record: AttendanceRecord): Long {
        return attendanceRecordDao.insertRecord(record)
    }
}
