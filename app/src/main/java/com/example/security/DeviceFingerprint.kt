package com.example.security

import android.os.Build
import java.security.MessageDigest

object DeviceFingerprint {

    /**
     * Derives a persistent mobile hardware fingerprint token signature bound to this device.
     */
    fun getDeviceToken(): String {
        val rawInfo = "${Build.BRAND}-${Build.MODEL}-${Build.HARDWARE}-${Build.MANUFACTURER}"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawInfo.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(12).uppercase()
        } catch (e: Exception) {
            "DEV-39A8F7B012"
        }
    }

    fun getDeviceName(): String {
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model"
    }
}
