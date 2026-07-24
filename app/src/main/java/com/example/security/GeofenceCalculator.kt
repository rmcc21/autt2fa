package com.example.security

import kotlin.math.*

object GeofenceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two GPS points using Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks if user position is within office geofence radius.
     */
    fun isWithinGeofence(
        userLat: Double, userLng: Double,
        officeLat: Double, officeLng: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = calculateDistanceMeters(userLat, userLng, officeLat, officeLng)
        return distance <= radiusMeters
    }

    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.roundToInt()} m"
        } else {
            String.format("%.2f km", distanceMeters / 1000.0)
        }
    }
}
