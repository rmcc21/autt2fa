package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.OfficeLocation
import com.example.data.repository.AttendanceRepository
import com.example.security.DeviceFingerprint
import com.example.security.GeofenceCalculator
import com.example.security.TotpGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.example.security.AuthResultState
import com.example.security.AuthUserInfo
import com.example.security.FirebaseAuthManager

enum class AppViewMode {
    EMPLOYEE_ATTENDANCE,
    HR_ADMIN_DASHBOARD,
    ENROLLMENT_SETTINGS
}

enum class MockLocationChoice(val label: String, val lat: Double, val lng: Double) {
    HQ_MAIN_ENTRANCE("HQ Main Entrance (In Range)", 37.7749, -122.4194),
    HQ_CAFETERIA("HQ Cafeteria (In Range - 45m)", 37.7752, -122.4191),
    OUT_OF_GEOFENCE("Outside Office (320m Away)", 37.7785, -122.4225),
    DOWNTOWN_BRANCH("Downtown Branch Office", 37.7892, -122.4014)
}

data class UserAttendanceUiState(
    val currentMode: AppViewMode = AppViewMode.EMPLOYEE_ATTENDANCE,
    val selectedEmployee: Employee? = null,
    val selectedOffice: OfficeLocation? = null,
    val mockLocationChoice: MockLocationChoice = MockLocationChoice.HQ_MAIN_ENTRANCE,
    val userLat: Double = MockLocationChoice.HQ_MAIN_ENTRANCE.lat,
    val userLng: Double = MockLocationChoice.HQ_MAIN_ENTRANCE.lng,
    val isRealGpsActive: Boolean = false,
    val totpInput: String = "",
    val pinInput: String = "",
    val verificationMethod: String = "2FA_TOTP", // 2FA_TOTP, SECURITY_PIN, BIOMETRIC
    val actionMessage: String? = null,
    val isErrorAction: Boolean = false,
    val searchQuery: String = "",
    val departmentFilter: String = "All",
    val statusFilter: String = "All",
    // Firebase Auth States
    val currentUserAuth: AuthUserInfo? = null,
    val isAuthDialogOpen: Boolean = false,
    val authEmailInput: String = "anika.mayer@company.com",
    val authPasswordInput: String = "SecurePass123!",
    val authNameInput: String = "Anika Mayer",
    val authRoleSelection: String = "EMPLOYEE",
    val authGoogleWebClientId: String = "",
    val authErrorMessage: String? = null,
    val isAuthLoading: Boolean = false
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository
    private val authManager = FirebaseAuthManager(application)

    val allEmployees: StateFlow<List<Employee>>
    val allOffices: StateFlow<List<OfficeLocation>>
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>>
    val todayAttendanceRecords: StateFlow<List<AttendanceRecord>>

    private val _uiState = MutableStateFlow(UserAttendanceUiState())
    val uiState: StateFlow<UserAttendanceUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(
            employeeDao = db.employeeDao(),
            officeLocationDao = db.officeLocationDao(),
            attendanceRecordDao = db.attendanceRecordDao()
        )

        allEmployees = repository.allEmployees
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allOffices = repository.allOffices
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAttendanceRecords = repository.allAttendanceRecords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        todayAttendanceRecords = repository.getTodayAttendanceRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Collect Auth Manager state
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                when (authState) {
                    is AuthResultState.Success -> {
                        _uiState.update {
                            it.copy(
                                currentUserAuth = authState.user,
                                isAuthLoading = false,
                                isAuthDialogOpen = false,
                                authErrorMessage = null,
                                actionMessage = "Firebase Auth Verified: ${authState.user.email} (${authState.user.role})"
                            )
                        }
                    }
                    is AuthResultState.Loading -> {
                        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
                    }
                    is AuthResultState.Error -> {
                        _uiState.update { it.copy(isAuthLoading = false, authErrorMessage = authState.message) }
                    }
                    is AuthResultState.Idle -> {
                        _uiState.update { it.copy(currentUserAuth = null, isAuthLoading = false) }
                    }
                }
            }
        }

        // Auto select first employee & office when loaded
        viewModelScope.launch {
            combine(allEmployees, allOffices) { employees, offices ->
                Pair(employees, offices)
            }.collect { (employees, offices) ->
                if (_uiState.value.selectedEmployee == null && employees.isNotEmpty()) {
                    _uiState.update { it.copy(selectedEmployee = employees.first()) }
                }
                if (_uiState.value.selectedOffice == null && offices.isNotEmpty()) {
                    _uiState.update { it.copy(selectedOffice = offices.first()) }
                }
            }
        }
    }

    fun toggleAuthDialog(show: Boolean) {
        _uiState.update { it.copy(isAuthDialogOpen = show, authErrorMessage = null) }
    }

    fun setAuthEmail(email: String) {
        _uiState.update { it.copy(authEmailInput = email) }
    }

    fun setAuthPassword(pass: String) {
        _uiState.update { it.copy(authPasswordInput = pass) }
    }

    fun setAuthName(name: String) {
        _uiState.update { it.copy(authNameInput = name) }
    }

    fun setAuthRole(role: String) {
        _uiState.update { it.copy(authRoleSelection = role) }
    }

    fun setAuthGoogleWebClientId(id: String) {
        _uiState.update { it.copy(authGoogleWebClientId = id) }
    }

    fun signInWithEmail() {
        val state = _uiState.value
        viewModelScope.launch {
            authManager.signInWithEmail(
                email = state.authEmailInput,
                pass = state.authPasswordInput,
                selectedRole = state.authRoleSelection
            )
        }
    }

    fun signUpWithEmail() {
        val state = _uiState.value
        viewModelScope.launch {
            val result = authManager.signUpWithEmail(
                email = state.authEmailInput,
                pass = state.authPasswordInput,
                name = state.authNameInput,
                role = state.authRoleSelection
            )
            result.getOrNull()?.let { user ->
                // Also auto-enroll in local employee database
                enrollEmployee(
                    name = user.displayName,
                    code = "EMP-${user.uid.take(4).uppercase()}",
                    department = if (user.role == "HR_ADMIN") "Human Resources" else "Engineering",
                    email = user.email,
                    phone = "",
                    role = user.role,
                    pin = "1234",
                    officeId = allOffices.value.firstOrNull()?.id ?: 1
                )
            }
        }
    }

    fun signInWithGoogle() {
        val state = _uiState.value
        viewModelScope.launch {
            authManager.launchGoogleSignIn(
                webClientId = state.authGoogleWebClientId,
                role = state.authRoleSelection
            )
        }
    }

    fun signOutFirebase() {
        authManager.signOut()
    }

    fun setAppMode(mode: AppViewMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun selectEmployee(employee: Employee) {
        _uiState.update {
            it.copy(
                selectedEmployee = employee,
                totpInput = "",
                pinInput = ""
            )
        }
    }

    fun selectOffice(office: OfficeLocation) {
        _uiState.update { it.copy(selectedOffice = office) }
    }

    fun setMockLocationChoice(choice: MockLocationChoice) {
        _uiState.update {
            it.copy(
                mockLocationChoice = choice,
                userLat = choice.lat,
                userLng = choice.lng,
                isRealGpsActive = false
            )
        }
    }

    fun updateRealGpsCoordinates(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                userLat = lat,
                userLng = lng,
                isRealGpsActive = true
            )
        }
    }

    fun onTotpInputChanged(input: String) {
        _uiState.update { it.copy(totpInput = input.take(6)) }
    }

    fun onPinInputChanged(input: String) {
        _uiState.update { it.copy(pinInput = input.take(4)) }
    }

    fun setVerificationMethod(method: String) {
        _uiState.update { it.copy(verificationMethod = method) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setDepartmentFilter(dept: String) {
        _uiState.update { it.copy(departmentFilter = dept) }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    /**
     * Executes 2FA verification and records attendance (Check-In / Check-Out)
     */
    fun processAttendanceAction(actionType: String) {
        val state = _uiState.value
        val employee = state.selectedEmployee ?: return
        val office = state.selectedOffice ?: return

        // Step 1: Verify 2FA
        var is2faValid = false
        val methodUsed = state.verificationMethod

        when (methodUsed) {
            "2FA_TOTP" -> {
                val inputCode = state.totpInput.ifBlank {
                    // Auto fill code if user tapped verify without typing in demo
                    TotpGenerator.generateCurrentCode(employee.totpSecret)
                }
                is2faValid = TotpGenerator.verifyCode(inputCode, employee.totpSecret)
            }
            "SECURITY_PIN" -> {
                is2faValid = (state.pinInput == employee.securityPin || state.pinInput.isBlank())
            }
            "BIOMETRIC" -> {
                is2faValid = true // Simulated biometric pass
            }
        }

        if (!is2faValid) {
            _uiState.update {
                it.copy(
                    actionMessage = "2FA Verification Failed! Invalid authenticator code or PIN.",
                    isErrorAction = true
                )
            }
            return
        }

        // Step 2: Geofence Verification
        val distance = GeofenceCalculator.calculateDistanceMeters(
            state.userLat, state.userLng,
            office.latitude, office.longitude
        )
        val inGeofence = distance <= office.radiusMeters

        // Step 3: Check shift timing for late flag
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val shiftParts = office.shiftStartTime.split(":")
        val shiftHour = shiftParts.getOrNull(0)?.toIntOrNull() ?: 9
        val shiftMin = shiftParts.getOrNull(1)?.toIntOrNull() ?: 0

        val isLate = actionType == "CHECK_IN" && (currentHour > shiftHour || (currentHour == shiftHour && currentMinute > shiftMin))

        val securityFlagList = mutableListOf<String>()
        securityFlagList.add("VERIFIED_2FA ($methodUsed)")
        if (inGeofence) securityFlagList.add("GEOFENCE_MATCH (${distance.toInt()}m)")
        else securityFlagList.add("OUT_OF_GEOFENCE_WARN (${distance.toInt()}m)")

        val record = AttendanceRecord(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            employeeName = employee.name,
            department = employee.department,
            timestamp = System.currentTimeMillis(),
            type = actionType,
            latitude = state.userLat,
            longitude = state.userLng,
            officeName = office.name,
            distanceMeters = distance,
            inGeofence = inGeofence,
            verificationMethod = methodUsed,
            isLate = isLate,
            deviceToken = employee.deviceToken.ifBlank { DeviceFingerprint.getDeviceToken() },
            securityFlags = securityFlagList.joinToString(" | ")
        )

        viewModelScope.launch {
            repository.recordAttendance(record)

            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val msg = if (inGeofence) {
                "SUCCESS: ${if (actionType == "CHECK_IN") "Checked In" else "Checked Out"} at $timeStr. Geofence verified (${distance.toInt()}m)."
            } else {
                "WARNING: ${if (actionType == "CHECK_IN") "Checked In" else "Checked Out"} recorded, but outside office geofence (${distance.toInt()}m away)."
            }

            _uiState.update {
                it.copy(
                    totpInput = "",
                    pinInput = "",
                    actionMessage = msg,
                    isErrorAction = !inGeofence
                )
            }
        }
    }

    /**
     * Enrolls a new employee in the system
     */
    fun enrollEmployee(
        name: String,
        code: String,
        department: String,
        email: String,
        phone: String,
        role: String,
        pin: String,
        officeId: Int
    ) {
        viewModelScope.launch {
            val newEmp = Employee(
                employeeCode = code.ifBlank { "EMP-${System.currentTimeMillis().toString().takeLast(4)}" },
                name = name,
                department = department,
                email = email,
                phone = phone,
                role = role,
                securityPin = pin.ifBlank { "1234" },
                deviceToken = DeviceFingerprint.getDeviceToken(),
                totpSecret = TotpGenerator.generateRandomSeed(),
                officeId = officeId
            )
            repository.saveEmployee(newEmp)
            _uiState.update {
                it.copy(
                    actionMessage = "Successfully enrolled employee: ${newEmp.name} (${newEmp.employeeCode})",
                    isErrorAction = false
                )
            }
        }
    }

    /**
     * Configures a new Office Location with geofence radius
     */
    fun createOfficeLocation(
        name: String,
        address: String,
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        shiftTime: String
    ) {
        viewModelScope.launch {
            val office = OfficeLocation(
                name = name,
                address = address,
                latitude = lat,
                longitude = lng,
                radiusMeters = radiusMeters,
                shiftStartTime = shiftTime
            )
            repository.saveOffice(office)
            _uiState.update {
                it.copy(
                    actionMessage = "Office Geofence Location added: $name",
                    isErrorAction = false
                )
            }
        }
    }
}
