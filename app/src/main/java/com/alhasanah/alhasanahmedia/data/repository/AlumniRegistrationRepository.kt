package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.RegisterAlumniRequest
import com.alhasanah.alhasanahmedia.data.remote.AlumniRegistrationRemoteDataSource

interface AlumniRegistrationRepository {
    suspend fun register(request: RegisterAlumniRequest): String
}

class AlumniRegistrationRepositoryImpl(
    private val remoteDataSource: AlumniRegistrationRemoteDataSource
) : AlumniRegistrationRepository {

    override suspend fun register(request: RegisterAlumniRequest): String {
        return remoteDataSource.register(request).message
            .ifBlank { "Pendaftaran alumni berhasil dikirim. Akun Anda menunggu verifikasi admin." }
    }
}
