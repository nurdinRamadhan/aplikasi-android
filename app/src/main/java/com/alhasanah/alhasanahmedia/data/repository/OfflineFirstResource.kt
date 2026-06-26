package com.alhasanah.alhasanahmedia.data.repository

data class OfflineFirstResource<T>(
    val data: T,
    val isFromCache: Boolean,
    val updatedAt: Long? = null,
    val notice: String? = null
)
