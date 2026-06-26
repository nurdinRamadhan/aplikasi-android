package com.alhasanah.alhasanahmedia.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alumni_cache_entries",
    indices = [
        Index(value = ["domain"]),
        Index(value = ["updatedAt"])
    ]
)
data class AlumniCacheEntity(
    @PrimaryKey val cacheKey: String,
    val domain: String,
    val json: String,
    val updatedAt: Long,
    val expiresAt: Long? = null
)
