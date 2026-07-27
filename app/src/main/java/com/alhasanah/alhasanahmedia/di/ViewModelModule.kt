package com.alhasanah.alhasanahmedia.di

import com.alhasanah.alhasanahmedia.MainViewModel
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniChatViewModel
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniDirectoryViewModel
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniProfileViewModel
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniRegisterViewModel
import com.alhasanah.alhasanahmedia.ui.alumni.ForumAlumniViewModel
import com.alhasanah.alhasanahmedia.ui.auth.AuthViewModel
import com.alhasanah.alhasanahmedia.ui.berita.BeritaDetailViewModel
import com.alhasanah.alhasanahmedia.ui.devotion.DevotionViewModel
import com.alhasanah.alhasanahmedia.ui.falak.FalakEphemerisViewModel
import com.alhasanah.alhasanahmedia.ui.falak.GerhanaBulanViewModel
import com.alhasanah.alhasanahmedia.ui.falak.HisabHilalViewModel
import com.alhasanah.alhasanahmedia.ui.hadith.HadithViewModel
import com.alhasanah.alhasanahmedia.ui.home.HomeViewModel
import com.alhasanah.alhasanahmedia.ui.ibadah.IbadahGuideViewModel
import com.alhasanah.alhasanahmedia.ui.islamiccalendar.IslamicCalendarViewModel
import com.alhasanah.alhasanahmedia.ui.notifikasi.NotificationViewModel
import com.alhasanah.alhasanahmedia.ui.keuangan.KeuanganViewModel
import com.alhasanah.alhasanahmedia.ui.prayer.PrayerScheduleViewModel
import com.alhasanah.alhasanahmedia.ui.absensilengkap.AbsensiLengkapViewModel
import com.alhasanah.alhasanahmedia.ui.santri.AbsensiViewModel
import com.alhasanah.alhasanahmedia.ui.santri.SantriActivityViewModel
import com.alhasanah.alhasanahmedia.ui.santri.PrestasiViewModel
import com.alhasanah.alhasanahmedia.ui.santri.SantriDetailViewModel
import com.alhasanah.alhasanahmedia.ui.santri.SantriListViewModel
import com.alhasanah.alhasanahmedia.ui.quran.JuzDetailViewModel
import com.alhasanah.alhasanahmedia.ui.quran.QuranViewModel
import com.alhasanah.alhasanahmedia.ui.quran.SurahDetailViewModel
import com.alhasanah.alhasanahmedia.ui.qibla.QiblaViewModel
import com.alhasanah.alhasanahmedia.ui.rag.RagChatViewModel
import com.alhasanah.alhasanahmedia.ui.wallet.KantinWalletViewModel
import com.alhasanah.alhasanahmedia.ui.wallet.WalletDisputeViewModel
import com.alhasanah.alhasanahmedia.ui.wallet.WalletWaliViewModel
import com.alhasanah.alhasanahmedia.ui.weather.WeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::ForumAlumniViewModel)
    viewModelOf(::AlumniDirectoryViewModel)
    viewModelOf(::AlumniProfileViewModel)
    viewModelOf(::AlumniRegisterViewModel)
    viewModelOf(::AlumniChatViewModel)
    // Memperbarui MainViewModel dengan dependensi baru
    viewModelOf(::MainViewModel)
    viewModelOf(::BeritaDetailViewModel)
    viewModel { (santriNis: String) -> KeuanganViewModel(santriNis, get(), get(), get(), get()) }
    viewModel { (santriNis: String) -> SantriDetailViewModel(santriNis, get()) }
    viewModelOf(::SantriActivityViewModel)
    viewModelOf(::PrestasiViewModel)
    viewModelOf(::AbsensiViewModel)
    viewModelOf(::AbsensiLengkapViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SantriListViewModel)
    viewModelOf(::NotificationViewModel)
    viewModel { (santriNis: String) -> WalletWaliViewModel(santriNis, get()) }
    viewModelOf(::KantinWalletViewModel)
    viewModel { (ledgerId: Long) -> WalletDisputeViewModel(ledgerId, get()) }
    viewModelOf(::RagChatViewModel)
    viewModel { QuranViewModel(get<com.alhasanah.alhasanahmedia.data.repository.QuranRepository>(), get<com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository>()) }
    viewModel { (nomor: Int) -> SurahDetailViewModel(get<com.alhasanah.alhasanahmedia.data.repository.QuranRepository>(), get<com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository>(), nomor) }
    viewModel { (nomor: Int) -> JuzDetailViewModel(get<com.alhasanah.alhasanahmedia.data.repository.QuranRepository>(), get<com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository>(), nomor) }
    viewModelOf(::QiblaViewModel)
    viewModelOf(::HadithViewModel)
    viewModelOf(::PrayerScheduleViewModel)
    viewModelOf(::DevotionViewModel)
    viewModelOf(::IbadahGuideViewModel)
    viewModelOf(::IslamicCalendarViewModel)
    viewModelOf(::WeatherViewModel)
    viewModelOf(::FalakEphemerisViewModel)
    viewModelOf(::HisabHilalViewModel)
    viewModelOf(::GerhanaBulanViewModel)
}
