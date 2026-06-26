package com.alhasanah.alhasanahmedia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlumniCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlhasanahDatabase : RoomDatabase() {
    abstract fun alumniCacheDao(): AlumniCacheDao
}
