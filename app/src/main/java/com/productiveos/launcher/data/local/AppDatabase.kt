package com.productiveos.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppEntity::class,
        ClipEntity::class,
        MicroLogEntity::class,
        PathCheckLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun microOsDao(): MicroOsDao
}
