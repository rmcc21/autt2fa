package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.AppViewMode
import com.example.ui.AttendanceViewModel
import com.example.ui.components.FirebaseAuthDialog
import com.example.ui.screens.EmployeeAttendanceScreen
import com.example.ui.screens.EnrollmentSettingsScreen
import com.example.ui.screens.HrAdminDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAttendanceApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAttendanceApp(viewModel: AttendanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val offices by viewModel.allOffices.collectAsStateWithLifecycle()
    val allRecords by viewModel.allAttendanceRecords.collectAsStateWithLifecycle()
    val todayRecords by viewModel.todayAttendanceRecords.collectAsStateWithLifecycle()

    val currentEmployee = uiState.selectedEmployee
    val userRecords = remember(allRecords, currentEmployee) {
        if (currentEmployee != null) allRecords.filter { it.employeeId == currentEmployee.id } else emptyList()
    }

    val authUser = uiState.currentUserAuth

    if (uiState.isAuthDialogOpen) {
        FirebaseAuthDialog(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = { viewModel.toggleAuthDialog(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "App Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Attendance 2FA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = when (uiState.currentMode) {
                                    AppViewMode.EMPLOYEE_ATTENDANCE -> "Main Office Hub • 2FA Stream"
                                    AppViewMode.HR_ADMIN_DASHBOARD -> "HR Administrator Portal"
                                    AppViewMode.ENROLLMENT_SETTINGS -> "System Config & Enrollment"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Firebase Auth Chip / Sign In Button
                    Surface(
                        onClick = { viewModel.toggleAuthDialog(true) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (authUser != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (authUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (authUser != null) Icons.Default.Verified else Icons.Default.Lock,
                                contentDescription = "Firebase Auth",
                                tint = if (authUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = authUser?.displayName?.take(12) ?: "Firebase 2FA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (authUser != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Mode Switcher Button
                    IconButton(
                        onClick = {
                            val nextMode = when (uiState.currentMode) {
                                AppViewMode.EMPLOYEE_ATTENDANCE -> AppViewMode.HR_ADMIN_DASHBOARD
                                AppViewMode.HR_ADMIN_DASHBOARD -> AppViewMode.ENROLLMENT_SETTINGS
                                AppViewMode.ENROLLMENT_SETTINGS -> AppViewMode.EMPLOYEE_ATTENDANCE
                            }
                            viewModel.setAppMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = when (uiState.currentMode) {
                                AppViewMode.EMPLOYEE_ATTENDANCE -> Icons.Default.AdminPanelSettings
                                AppViewMode.HR_ADMIN_DASHBOARD -> Icons.Default.Settings
                                AppViewMode.ENROLLMENT_SETTINGS -> Icons.Default.PersonPinCircle
                            },
                            contentDescription = "Switch Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = uiState.currentMode == AppViewMode.EMPLOYEE_ATTENDANCE,
                    onClick = { viewModel.setAppMode(AppViewMode.EMPLOYEE_ATTENDANCE) },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "2FA Attendance") },
                    label = { Text("Attendance", fontWeight = FontWeight.Bold) }
                )

                NavigationBarItem(
                    selected = uiState.currentMode == AppViewMode.HR_ADMIN_DASHBOARD,
                    onClick = { viewModel.setAppMode(AppViewMode.HR_ADMIN_DASHBOARD) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "HR Dashboard") },
                    label = { Text("HR Portal", fontWeight = FontWeight.Bold) }
                )

                NavigationBarItem(
                    selected = uiState.currentMode == AppViewMode.ENROLLMENT_SETTINGS,
                    onClick = { viewModel.setAppMode(AppViewMode.ENROLLMENT_SETTINGS) },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Enrollment") },
                    label = { Text("Enrollment", fontWeight = FontWeight.Bold) }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentMode,
                label = "ScreenTransition"
            ) { targetMode ->
                when (targetMode) {
                    AppViewMode.EMPLOYEE_ATTENDANCE -> {
                        EmployeeAttendanceScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            employees = employees,
                            offices = offices,
                            userRecords = userRecords
                        )
                    }

                    AppViewMode.HR_ADMIN_DASHBOARD -> {
                        HrAdminDashboardScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            employees = employees,
                            allRecords = allRecords,
                            todayRecords = todayRecords
                        )
                    }

                    AppViewMode.ENROLLMENT_SETTINGS -> {
                        EnrollmentSettingsScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            employees = employees,
                            offices = offices
                        )
                    }
                }
            }
        }
    }
}
