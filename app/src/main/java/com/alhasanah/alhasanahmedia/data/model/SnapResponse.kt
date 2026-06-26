package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SnapResponse(
    val token: String? = null,
    val error_messages: List<String>? = null,
    val error: String? = null,
    val details: MidtransErrorDetails? = null
)

@Serializable
data class MidtransErrorDetails(
    val error_messages: List<String>? = null
)
