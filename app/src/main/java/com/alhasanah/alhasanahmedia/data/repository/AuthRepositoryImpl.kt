package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus // Sesuai dokumentasi v3
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val notificationRepository: NotificationRepository? = null
) : AuthRepository {

    override fun getAuthState(): Flow<AuthenticationState> = supabaseClient.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthenticationState.Authenticated(status.session.user?.id ?: "")
                else -> AuthenticationState.NotAuthenticated
            }
        }
        .distinctUntilChanged()

    override fun getCurrentUser(): Flow<UserInfo?> = supabaseClient.auth.sessionStatus.map {
        (it as? SessionStatus.Authenticated)?.session?.user
    }

    override suspend fun getCurrentUserRole(): String? {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return null
        return supabaseClient.from("profiles")
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeList<ProfileRoleDto>()
            .firstOrNull()
            ?.role
    }

    override suspend fun signIn(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signOut() {
        notificationRepository?.let { repository ->
            runCatching {
                repository.deactivateMyFcmDevice(FirebaseMessaging.getInstance().token.await())
            }
        }
        supabaseClient.auth.signOut()
    }
}

@Serializable
private data class ProfileRoleDto(
    val role: String? = null
)
