package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.OfficeLocation
import com.example.security.DeviceFingerprint
import com.example.security.TotpGenerator
import com.example.ui.AttendanceViewModel
import com.example.ui.UserAttendanceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentSettingsScreen(
    viewModel: AttendanceViewModel,
    uiState: UserAttendanceUiState,
    employees: List<Employee>,
    offices: List<OfficeLocation>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Employee Enrollment Form States
    var empName by remember { mutableStateOf("") }
    var empCode by remember { mutableStateOf("") }
    var empDept by remember { mutableStateOf("Engineering") }
    var empEmail by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf("EMPLOYEE") }
    var empPin by remember { mutableStateOf("1234") }
    var empOfficeId by remember { mutableIntStateOf(offices.firstOrNull()?.id ?: 1) }

    // Office Geofence Form States
    var officeName by remember { mutableStateOf("") }
    var officeAddress by remember { mutableStateOf("") }
    var officeLat by remember { mutableStateOf("37.7749") }
    var officeLng by remember { mutableStateOf("-122.4194") }
    var officeRadius by remember { mutableStateOf("150") }
    var officeShiftTime by remember { mutableStateOf("09:00") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "System Configuration & Enrollment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Enroll employees, bind 2FA credentials, & configure office geofence zones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sub Tab Selector (Enroll Staff / Geofence Office / Enrolled Directory)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Enroll Staff", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Office Geofence", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Staff Directory", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Enrollment Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "New Employee 2FA Registration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = empName,
                                onValueChange = { empName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = empCode,
                                    onValueChange = { empCode = it },
                                    label = { Text("Employee Code") },
                                    placeholder = { Text("EMP-1003") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = empPin,
                                    onValueChange = { empPin = it.take(4) },
                                    label = { Text("Security PIN") },
                                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = empEmail,
                                onValueChange = { empEmail = it },
                                label = { Text("Work Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Department selection
                            Text(
                                text = "Department & Role:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Engineering", "Human Resources", "Sales", "Marketing").forEach { dept ->
                                    FilterChip(
                                        selected = empDept == dept,
                                        onClick = { empDept = dept },
                                        label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = empRole == "EMPLOYEE",
                                    onClick = { empRole = "EMPLOYEE" },
                                    label = { Text("Role: Employee") }
                                )
                                FilterChip(
                                    selected = empRole == "HR_ADMIN",
                                    onClick = { empRole = "HR_ADMIN" },
                                    label = { Text("Role: HR Admin") }
                                )
                            }

                            // Device & 2FA Generated Preview Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "2FA Credentials Generated on Enrollment:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• TOTP Secret Seed: Random RFC 6238 Key\n• Mobile Device Token: Bound Hardware Fingerprint\n• Initial Security Status: 2FA ENROLLED",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (empName.isNotBlank()) {
                                        viewModel.enrollEmployee(
                                            name = empName,
                                            code = empCode,
                                            department = empDept,
                                            email = empEmail,
                                            phone = empPhone,
                                            role = empRole,
                                            pin = empPin,
                                            officeId = empOfficeId
                                        )
                                        empName = ""
                                        empCode = ""
                                        empEmail = ""
                                    }
                                },
                                enabled = empName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REGISTER & BIND EMPLOYEE 2FA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Office Geofence Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Configure Office Geofence Target",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Preset Office Pickers
                            Text(
                                text = "Quick Location Presets:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        officeName = "San Francisco HQ"
                                        officeAddress = "100 Innovation Way"
                                        officeLat = "37.7749"
                                        officeLng = "-122.4194"
                                        officeRadius = "150"
                                    },
                                    label = { Text("San Francisco HQ") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        officeName = "New York Hub"
                                        officeAddress = "350 5th Avenue"
                                        officeLat = "40.7484"
                                        officeLng = "-73.9857"
                                        officeRadius = "200"
                                    },
                                    label = { Text("New York Hub") }
                                )
                            }

                            OutlinedTextField(
                                value = officeName,
                                onValueChange = { officeName = it },
                                label = { Text("Office Name *") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = officeAddress,
                                onValueChange = { officeAddress = it },
                                label = { Text("Street Address") },
                                leadingIcon = { Icon(Icons.Default.HomeWork, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = officeLat,
                                    onValueChange = { officeLat = it },
                                    label = { Text("Latitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = officeLng,
                                    onValueChange = { officeLng = it },
                                    label = { Text("Longitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = officeRadius,
                                    onValueChange = { officeRadius = it },
                                    label = { Text("Geofence Radius (m)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = officeShiftTime,
                                    onValueChange = { officeShiftTime = it },
                                    label = { Text("Shift Start (HH:mm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    val lat = officeLat.toDoubleOrNull() ?: 37.7749
                                    val lng = officeLng.toDoubleOrNull() ?: -122.4194
                                    val rad = officeRadius.toDoubleOrNull() ?: 150.0

                                    if (officeName.isNotBlank()) {
                                        viewModel.createOfficeLocation(
                                            name = officeName,
                                            address = officeAddress,
                                            lat = lat,
                                            lng = lng,
                                            radiusMeters = rad,
                                            shiftTime = officeShiftTime
                                        )
                                        officeName = ""
                                        officeAddress = ""
                                    }
                                },
                                enabled = officeName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddLocation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SAVE GEOFENCE LOCATION", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Active Office Locations (${offices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(offices) { office ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = office.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = office.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lat: ${office.latitude}, Lng: ${office.longitude} • Shift: ${office.shiftStartTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${office.radiusMeters.toInt()}m Radius",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Staff Directory List
                item {
                    Text(
                        text = "Enrolled Employees Directory (${employees.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(employees) { emp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = emp.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = emp.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${emp.employeeCode} • ${emp.department} • ${emp.role}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "2FA ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "TOTP Seed: ${emp.totpSecret}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Security PIN: ${emp.securityPin} • Hardware Token: ${emp.deviceToken}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
