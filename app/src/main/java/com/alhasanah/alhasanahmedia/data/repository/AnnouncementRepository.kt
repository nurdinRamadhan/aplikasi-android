package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.Announcement
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc

interface AnnouncementRepository {
    suspend fun getActiveAnnouncements(): List<Announcement>
}

class AnnouncementRepositoryImpl(
    private val postgrest: Postgrest
) : AnnouncementRepository {

    override suspend fun getActiveAnnouncements(): List<Announcement> {
        return postgrest.rpc("get_active_announcements")
            .decodeList()
    }
}
