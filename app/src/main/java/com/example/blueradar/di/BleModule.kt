package com.example.blueradar.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.blueradar.data.ble.BleScanner
import com.example.blueradar.data.repository.BleRepository
import com.example.blueradar.data.repository.BleRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(
        @ApplicationContext context: Context
    ): BluetoothAdapter? {
        return (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter
    }

    @Provides
    @Singleton
    fun provideBleScanner(
        bluetoothAdapter: BluetoothAdapter?
    ): BleScanner {
        return BleScanner(bluetoothAdapter)
    }

    @Provides
    @Singleton
    fun provideBleRepository(
        bleRepositoryImpl: BleRepositoryImpl
    ): BleRepository = bleRepositoryImpl
}
