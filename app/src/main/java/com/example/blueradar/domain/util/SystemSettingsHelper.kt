package com.example.blueradar.domain.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class untuk handle Bluetooth dan Location settings
 * Provides utilities untuk check dan request enable
 */
@Singleton
class SystemSettingsHelper @Inject constructor() {

    /**
     * Check apakah Bluetooth enabled
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Check apakah Location (GPS) enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    /**
     * Check apakah semua Bluetooth permissions sudah granted
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check apakah Location permissions sudah granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request enable Bluetooth via system dialog
     * Returns Intent untuk launch via ActivityResultLauncher
     */
    fun getEnableBluetoothIntent(): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    /**
     * Request enable Location via Google Play Services dialog
     * Throws ResolvableApiException jika user perlu enable Location
     * 
     * @param context Context
     * @param locationSettingsLauncher Launcher untuk handle resolution
     * @return true jika sudah enabled, false jika perlu request
     */
    suspend fun requestEnableLocation(
        context: Context,
        locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean {
        // Buat LocationRequest dengan priority HIGH_ACCURACY
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 detik interval
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show dialog even if location already enabled

        val client = LocationServices.getSettingsClient(context)
        val settingsResponse = client.checkLocationSettings(builder.build())

        return try {
            // Tunggu result - jika success berarti location sudah enabled
            settingsResponse.await()
            true
        } catch (exception: ResolvableApiException) {
            // Location not enabled, show dialog untuk enable
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(
                    exception.resolution.intentSender
                ).build()
                locationSettingsLauncher.launch(intentSenderRequest)
                false
            } catch (e: Exception) {
                // Fallback: buka Settings page manual
                openLocationSettings(context)
                false
            }
        } catch (e: Exception) {
            // Error lain, fallback ke settings
            openLocationSettings(context)
            false
        }
    }

    /**
     * Buka Location Settings page manual
     * Fallback jika Google Play Services dialog gagal
     */
    private fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Check apakah semua requirements sudah terpenuhi untuk BLE scan
     * Returns Pair<Boolean, String> - (isReady, errorMessage)
     */
    fun checkBleReadiness(context: Context): Pair<Boolean, String?> {
        return when {
            !hasBluetoothPermissions(context) -> 
                false to "Bluetooth permissions not granted"
            
            !hasLocationPermissions(context) -> 
                false to "Location permission not granted"
            
            !isBluetoothEnabled() -> 
                false to "Bluetooth is disabled"
            
            !isLocationEnabled(context) -> 
                false to "Location is disabled"
            
            else -> 
                true to null
        }
    }
}
