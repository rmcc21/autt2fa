package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.ui.AttendanceViewModel
import com.example.ui.UserAttendanceUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrAdminDashboardScreen(
    viewModel: AttendanceViewModel,
    uiState: UserAttendanceUiState,
    employees: List<Employee>,
    allRecords: List<AttendanceRecord>,
    todayRecords: List<AttendanceRecord>,
    modifier: Modifier = Modifier
) {
    var selectedRecordForAudit by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val departments = remember(employees) {
        listOf("All") + employees.map { it.department }.distinct()
    }

    // Compute HR Metrics
    val totalEmployeesCount = employees.size
    val todayCheckInRecords = todayRecords.filter { it.type == "CHECK_IN" }
    val uniqueCheckedInTodayCount = todayCheckInRecords.map { it.employeeId }.distinct().size

    val onTimeCount = todayCheckInRecords.count { !it.isLate }
    val onTimePercentage = if (todayCheckInRecords.isNotEmpty()) {
        ((onTimeCount.toFloat() / todayCheckInRecords.size) * 100).toInt()
    } else 100

    val flaggedOutOfGeofenceCount = todayRecords.count { !it.inGeofence }

    // Filter logs according to search & department selections
    val filteredRecords = remember(allRecords, uiState.searchQuery, uiState.departmentFilter, uiState.statusFilter) {
        allRecords.filter { record ->
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    record.employeeName.contains(uiState.searchQuery, ignoreCase = true) ||
                    record.employeeCode.contains(uiState.searchQuery, ignoreCase = true) ||
                    record.department.contains(uiState.searchQuery, ignoreCase = true)

            val matchesDepartment = uiState.departmentFilter == "All" || record.department == uiState.departmentFilter

            val matchesStatus = when (uiState.statusFilter) {
                "Check-In" -> record.type == "CHECK_IN"
                "Check-Out" -> record.type == "CHECK_OUT"
                "Out-Of-Geofence" -> !record.inGeofence
                else -> true
            }

            matchesSearch && matchesDepartment && matchesStatus
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HR Admin Header & Quick Report Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HR Administrator Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time 2FA Attendance & Geofence Audit Stream",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // HR Metrics 4-Grid Dashboard
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Enrolled Staff",
                        value = "$totalEmployeesCount",
                        subtitle = "Active Profiles",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Present Today",
                        value = "$uniqueCheckedInTodayCount / $totalEmployeesCount",
                        subtitle = "${if (totalEmployeesCount > 0) ((uniqueCheckedInTodayCount.toFloat() / totalEmployeesCount) * 100).toInt() else 0}% Attendance Rate",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "On-Time Rate",
                        value = "$onTimePercentage%",
                        subtitle = "$onTimeCount On-Time Checkins",
                        icon = Icons.Default.AccessTime,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Geofence Alerts",
                        value = "$flaggedOutOfGeofenceCount",
                        subtitle = "Out-of-Range Flags",
                        icon = Icons.Default.GpsOff,
                        color = if (flaggedOutOfGeofenceCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Search & Filter Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search staff name, code, or department...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Department Filter Chips
                    Text(
                        text = "Filter by Department:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(departments) { dept ->
                            FilterChip(
                                selected = uiState.departmentFilter == dept,
                                onClick = { viewModel.setDepartmentFilter(dept) },
                                label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        listOf("All", "Check-In", "Check-Out", "Out-Of-Geofence").forEach { status ->
                            FilterChip(
                                selected = uiState.statusFilter == status,
                                onClick = { viewModel.setStatusFilter(status) },
                                label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }

        // Live Attendance Audit Logs Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Audit Trail (${filteredRecords.size} Records)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tap log for full audit sheet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Attendance Record Cards Feed
        if (filteredRecords.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No attendance records match your filter criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                HrAttendanceAuditCard(
                    record = record,
                    onClick = { selectedRecordForAudit = record }
                )
            }
        }
    }

    // Inspection Modal Sheet for selected Audit Record
    selectedRecordForAudit?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedRecordForAudit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "2FA Audit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security Audit Verification")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = record.employeeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${record.employeeCode} • ${record.department}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    AuditDetailRow(
                        label = "Action Event",
                        value = "${record.type} at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))}"
                    )
                    AuditDetailRow(
                        label = "2FA Method",
                        value = record.verificationMethod
                    )
                    AuditDetailRow(
                        label = "Office Target",
                        value = record.officeName
                    )
                    AuditDetailRow(
                        label = "Geofence Distance",
                        value = "${record.distanceMeters.toInt()} meters (${if (record.inGeofence) "PASSED" else "OUTSIDE BOUNDARY"})"
                    )
                    AuditDetailRow(
                        label = "GPS Coordinates",
                        value = "${record.latitude}, ${record.longitude}"
                    )
                    AuditDetailRow(
                        label = "Mobile Device Token",
                        value = record.deviceToken
                    )
                    AuditDetailRow(
                        label = "Security Flags",
                        value = record.securityFlags
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecordForAudit = null }) {
                    Text("Close Audit Sheet")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val clipboardManager = LocalClipboardManager.current
        val csvSummary = remember(allRecords) {
            val sb = StringBuilder("EmployeeCode,EmployeeName,Department,Type,Timestamp,Office,DistanceMeters,InGeofence,2FAMethod,DeviceToken\n")
            allRecords.forEach { r ->
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(r.timestamp))
                sb.append("${r.employeeCode},\"${r.employeeName}\",${r.department},${r.type},$timeStr,\"${r.officeName}\",${r.distanceMeters.toInt()},${r.inGeofence},${r.verificationMethod},${r.deviceToken}\n")
            }
            sb.toString()
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Summarize, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Attendance Audit CSV")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Copy the formatted 2FA attendance logs to clipboard or export to HR portal:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Text(
                            text = csvSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(csvSummary))
                        showExportDialog = false
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy CSV to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun HrAttendanceAuditCard(
    record: AttendanceRecord,
    onClick: () -> Unit
) {
    val isCheckIn = record.type == "CHECK_IN"
    val timeFormatted = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = record.employeeName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = record.employeeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${record.employeeCode} • ${record.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCheckIn) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCheckIn) "CHECK IN" else "CHECK OUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCheckIn) Color(0xFF059669) else Color(0xFF4F46E5),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.officeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $timeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (record.inGeofence) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (record.inGeofence) "GEOFENCE MATCH" else "${record.distanceMeters.toInt()}m OUTSIDE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (record.inGeofence) Color(0xFF166534) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2FA: ${record.verificationMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Device Token: ${record.deviceToken.take(8)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AuditDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}
