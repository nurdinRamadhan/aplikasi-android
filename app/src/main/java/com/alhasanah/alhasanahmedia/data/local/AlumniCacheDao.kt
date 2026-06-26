package com.alhasanah.alhasanahmedia.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AlumniCacheDao {
    @Query("select * from alumni_cache_entries where cacheKey = :key limit 1")
    suspend fun get(key: String): AlumniCacheEntity?

    @Upsert
    suspend fun upsert(entity: AlumniCacheEntity)

    @Query("delete from alumni_cache_entries where cacheKey = :key")
    suspend fun delete(key: String)

    @Query("delete from alumni_cache_entries where domain = :domain")
    suspend fun deleteDomain(domain: String)

    @Query("delete from alumni_cache_entries where expiresAt is not null and expiresAt < :now")
    suspend fun deleteExpired(now: Long)
}
