package com.alhasanah.alhasanahmedia.data.repository

import android.util.Log
import com.alhasanah.alhasanahmedia.data.model.FCMTokenDto
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface NotificationRepository {
    suspend fun updateFCMToken(userId: String, token: String)
    suspend fun registerMyFcmDevice(token: String, deviceId: String, appInstanceId: String?)
    suspend fun deactivateMyFcmDevice(token: String)
}

class NotificationRepositoryImpl(
    private val postgrest: Postgrest,
    private val auth: Auth
) : NotificationRepository {

    override suspend fun updateFCMToken(userId: String, token: String) {
        registerMyFcmDevice(token = token, deviceId = "", appInstanceId = null)
    }

    override suspend fun registerMyFcmDevice(token: String, deviceId: String, appInstanceId: String?) {
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext
            runCatching {
                postgrest.rpc(
                    "register_my_fcm_device",
                    RegisterFcmDeviceParams(
                        fcmToken = token,
                        deviceId = deviceId.ifBlank { null },
                        platform = "android",
                        appInstanceId = appInstanceId
                    )
                )
            }.onFailure { error ->
                Log.e("NotificationRepository", "Gagal register FCM device", error)
            }
        }
    }

    override suspend fun deactivateMyFcmDevice(token: String) {
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext
            runCatching {
                postgrest.rpc("deactivate_my_fcm_device", DeactivateFcmDeviceParams(token))
            }.onFailure { error ->
                Log.e("NotificationRepository", "Gagal deactivate FCM device", error)
            }
        }
    }
}

@Serializable
private data class RegisterFcmDeviceParams(
    @SerialName("p_fcm_token")
    val fcmToken: String,
    @SerialName("p_device_id")
    val deviceId: String?,
    @SerialName("p_platform")
    val platform: String,
    @SerialName("p_app_instance_id")
    val appInstanceId: String?
)

@Serializable
private data class DeactivateFcmDeviceParams(
    @SerialName("p_fcm_token")
    val fcmToken: String
)
