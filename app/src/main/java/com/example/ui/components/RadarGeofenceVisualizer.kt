package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.GeofenceCalculator
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarGeofenceVisualizer(
    userLat: Double,
    userLng: Double,
    officeName: String,
    officeLat: Double,
    officeLng: Double,
    geofenceRadiusMeters: Double,
    modifier: Modifier = Modifier
) {
    val distance = GeofenceCalculator.calculateDistanceMeters(userLat, userLng, officeLat, officeLng)
    val isInGeofence = distance <= geofenceRadiusMeters

    // Infinite radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val primaryStatusColor = if (isInGeofence) Color(0xFF10B981) else Color(0xFFEF4444)
    val radarBgColor = Color(0xFF0F172A)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = radarBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Radar Geofence",
                        tint = primaryStatusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE GEOFENCE RADAR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = primaryStatusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryStatusColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryStatusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isInGeofence) "IN RANGE" else "OUT OF GEOFENCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryStatusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Radar Canvas
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPx = Offset(size.width / 2f, size.height / 2f)
                    val maxRadiusPx = size.width / 2f * 0.85f

                    // Concentric grid circles
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = maxRadiusPx * 0.33f,
                        center = centerPx,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = maxRadiusPx * 0.66f,
                        center = centerPx,
                        style = Stroke(width = 1f)
                    )

                    // Geofence boundary circle (Max Radius = geofence boundary)
                    val geofenceRadiusPx = maxRadiusPx * 0.75f
                    drawCircle(
                        color = primaryStatusColor,
                        radius = geofenceRadiusPx,
                        center = centerPx,
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )

                    // Pulse effect inside geofence
                    drawCircle(
                        color = primaryStatusColor.copy(alpha = pulseAlpha),
                        radius = geofenceRadiusPx * pulseRadiusScale,
                        center = centerPx
                    )

                    // Crosshair lines
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(centerPx.x, 0f),
                        end = Offset(centerPx.x, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(0f, centerPx.y),
                        end = Offset(size.width, centerPx.y),
                        strokeWidth = 1f
                    )

                    // Office position (Center Node)
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 8f,
                        center = centerPx
                    )

                    // Compute relative offsets for user node on canvas
                    val dLat = userLat - officeLat
                    val dLng = userLng - officeLng

                    // Scale position proportional to distance vs geofence radius
                    val distRatio = (distance / geofenceRadiusMeters).coerceAtMost(1.3)
                    val angle = kotlin.math.atan2(dLat, dLng)
                    val userOffsetRadius = (geofenceRadiusPx * distRatio).toFloat()

                    val userX = centerPx.x + userOffsetRadius * cos(angle).toFloat()
                    val userY = centerPx.y - userOffsetRadius * sin(angle).toFloat()
                    val userPos = Offset(userX, userY)

                    // Distance connecting line
                    drawLine(
                        color = primaryStatusColor.copy(alpha = 0.8f),
                        start = centerPx,
                        end = userPos,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // User location node
                    drawCircle(
                        color = primaryStatusColor,
                        radius = 12f,
                        center = userPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = userPos
                    )
                }

                // Center Icon label for Office
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = officeName,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Office & Distance Summary Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = officeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Geofence Radius: ${geofenceRadiusMeters.toInt()}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = GeofenceCalculator.formatDistance(distance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryStatusColor
                    )
                    Text(
                        text = "Target Distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
