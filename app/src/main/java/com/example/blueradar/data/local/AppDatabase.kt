package com.example.blueradar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database untuk BlueRadar
 * Version 1: initial schema dengan device_history table
 */
@Database(
    entities = [DeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        const val DATABASE_NAME = "blueradar_db"
    }
}
