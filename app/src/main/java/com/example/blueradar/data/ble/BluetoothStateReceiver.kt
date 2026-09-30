package com.example.blueradar.data.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Broadcast Receiver untuk mendeteksi perubahan state Bluetooth
 * Emit Flow<Boolean> untuk observe state changes
 */
@Singleton
class BluetoothStateReceiver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Observe Bluetooth state changes
     * @return Flow<Boolean> - true if enabled, false if disabled
     */
    fun observeBluetoothState(): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        val isEnabled = state == BluetoothAdapter.STATE_ON
                        trySend(isEnabled)
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        // Send initial state
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        trySend(bluetoothAdapter?.isEnabled == true)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver not registered
            }
        }
    }
}
