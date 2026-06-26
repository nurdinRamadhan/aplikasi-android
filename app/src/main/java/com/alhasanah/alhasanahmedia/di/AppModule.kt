package com.alhasanah.alhasanahmedia.di

import androidx.room.Room
import com.alhasanah.alhasanahmedia.data.local.AlhasanahDatabase
import com.alhasanah.alhasanahmedia.data.repository.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AlhasanahDatabase::class.java,
            "alhasanah_offline.db"
        )
            .build()
    }
    single { get<AlhasanahDatabase>().alumniCacheDao() }

    // Repositories
    single { com.alhasanah.alhasanahmedia.data.remote.RagRemoteDataSource(get()) }
    single { com.alhasanah.alhasanahmedia.data.remote.AlumniRegistrationRemoteDataSource() }
    single<RagRepository> { RagRepositoryImpl(androidContext(), get()) }
    single { PublicRepository(get()) }
    single { ThemeRepository(androidContext()) }
    single { AlumniLocalCacheStore(androidContext(), get()) }
    single { OfflineFirstCacheStore(androidContext(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<AlumniRepository> { AlumniRepositoryImpl(androidContext(), get(), get(), get(), get()) }
    single<AlumniRegistrationRepository> { AlumniRegistrationRepositoryImpl(get()) }
    single<IndonesiaRegionRepository> { IndonesiaRegionRepositoryImpl() }
    single<ForumRepository> { ForumRepositoryImpl(androidContext(), get(), get(), get()) }
    single { ChatOutboxStore(androidContext()) }
    single { ChatE2eeCrypto(androidContext()) }
    single { WalletDeviceCrypto(androidContext()) }
    single { WalletSecurityGuard() }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get()) }
    single<WaliSantriRepository> { WaliSantriRepositoryImpl(get(), get(), get()) }
    single<KeuanganRepository> { KeuanganRepositoryImpl(get(), get()) }
    single<SantriActivityRepository> { SantriActivityRepositoryImpl(get(), get()) }
    single<BeritaRepository> { BeritaRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
    single<WalletRepository> { WalletRepositoryImpl(get(), get(), get()) }
    single<QuranRepository> { QuranRepositoryImpl(get()) }
    single<QiblaRepository> { QiblaRepositoryImpl(get()) }
    single<HadithRepository> { HadithRepositoryImpl(get(), get()) }
    single<PrayerScheduleRepository> { PrayerScheduleRepositoryImpl(get(), get()) }
    single<DevotionRepository> { DevotionRepositoryImpl(get(), get()) }
    single<IbadahGuideRepository> { IbadahGuideRepositoryImpl(androidContext()) }
    single<IslamicCalendarRepository> { IslamicCalendarRepositoryImpl(get(), get()) }
    single<WeatherRepository> { WeatherRepositoryImpl(get()) }
    single<FalakRepository> { FalakRepositoryImpl(androidContext(), get(), get()) }
    single { com.alhasanah.alhasanahmedia.domain.falak.HisabHilalEphemerisCalculator() }
    single { com.alhasanah.alhasanahmedia.domain.falak.GerhanaBulanEphemerisCalculator() }
    single<HisabHilalRepository> { HisabHilalRepositoryImpl(get(), get()) }
    single<GerhanaBulanRepository> { GerhanaBulanRepositoryImpl(get(), get()) }
    single { QuranBookmarkRepository(androidContext()) }
    single { com.alhasanah.alhasanahmedia.util.PrayerLocationManager(androidContext()) }
    single { com.alhasanah.alhasanahmedia.util.PrayerManager(get(), get()) }
    single { com.alhasanah.alhasanahmedia.util.PrayerReminderScheduler(androidContext()) }
    single { com.alhasanah.alhasanahmedia.util.QiblaDeviceManager(androidContext()) }
    single { com.alhasanah.alhasanahmedia.util.WeatherLocationManager(androidContext()) }
    single { com.alhasanah.alhasanahmedia.util.FalakMarkazProvider(androidContext(), get()) }
}
