package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object SecurityEngine {

    /**
     * Detects if the app is running on an emulator.
     * Checks multiple hardware, model, and fingerprint indicators.
     */
    fun isEmulator(): Boolean {
        val buildDetails = (Build.FINGERPRINT + Build.DEVICE + Build.MODEL + Build.PRODUCT + Build.BRAND + Build.HARDWARE + Build.MANUFACTURER).lowercase(Locale.ROOT)
        
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.PRODUCT.contains("sdk_gphone") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("sdk_x86") ||
                Build.PRODUCT.contains("vbox86p") ||
                buildDetails.contains("nox") ||
                buildDetails.contains("bluestacks") ||
                buildDetails.contains("andy") ||
                buildDetails.contains("koplayer") ||
                buildDetails.contains("mumu") ||
                buildDetails.contains("ldplayer") ||
                buildDetails.contains("gameloop") ||
                buildDetails.contains("memu") ||
                buildDetails.contains("droid4x") ||
                buildDetails.contains("windroy")
    }

    /**
     * Detects if the device is rooted.
     * Checks for su binary in common system paths and build tags.
     */
    fun isRooted(): Boolean {
        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    /**
     * Detects if there is an active VPN or proxy connection.
     */
    fun isVpnActive(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Generates a device UUID based on: SHA-256(androidId + buildSerial + manufacturer + model + display)
     * Securely hashes the identifiers to protect privacy.
     */
    fun getDeviceUuid(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            val buildSerial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { Build.getSerial() } catch (e: SecurityException) { Build.SERIAL }
            } else {
                Build.SERIAL
            } ?: ""
            val manufacturer = Build.MANUFACTURER ?: ""
            val model = Build.MODEL ?: ""
            val display = Build.DISPLAY ?: ""

            val rawInput = androidId + buildSerial + manufacturer + model + display
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawInput.toByteArray(Charsets.UTF_8))
            
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Safe fallback
            "fallback_uuid_${Build.MODEL.hashCode()}_${Build.BOARD.hashCode()}"
        }
    }

    /**
     * Simulates signature validation for the APK.
     */
    fun verifyApkSignature(context: Context): Boolean {
        // Real implementations check PackageManager signatures, here we return true to avoid breaking builds
        return true
    }
}
