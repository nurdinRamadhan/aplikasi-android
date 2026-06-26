package com.alhasanah.alhasanahmedia.di

import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.remote.devotion.AhmadSanusiApiService
import com.alhasanah.alhasanahmedia.data.remote.hadith.HadithApiService
import com.alhasanah.alhasanahmedia.data.remote.islamiccalendar.IslamicCalendarApiService
import com.alhasanah.alhasanahmedia.data.remote.prayer.PrayerScheduleApiService
import com.alhasanah.alhasanahmedia.data.remote.qibla.QiblaApiService
import com.alhasanah.alhasanahmedia.data.remote.quran.QuranApiService
import com.alhasanah.alhasanahmedia.data.remote.weather.WeatherApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single<Retrofit> {
        val gson = GsonBuilder().create()

        Retrofit.Builder()
            .baseUrl("https://api.myquran.com/v3/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(get())
            .build()
    }

    single<Retrofit>(qualifier = org.koin.core.qualifier.named("bmkgRetrofit")) {
        val gson = GsonBuilder().create()

        Retrofit.Builder()
            .baseUrl("https://api.bmkg.go.id/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(get())
            .build()
    }

    single<Retrofit>(qualifier = org.koin.core.qualifier.named("ahmadSanusiRetrofit")) {
        val gson = GsonBuilder().create()

        Retrofit.Builder()
            .baseUrl("https://api.ahmadsanusi.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(get())
            .build()
    }

    single { get<Retrofit>().create(QuranApiService::class.java) }
    single { get<Retrofit>().create(QiblaApiService::class.java) }
    single { get<Retrofit>().create(HadithApiService::class.java) }
    single { get<Retrofit>().create(PrayerScheduleApiService::class.java) }
    single { get<Retrofit>().create(IslamicCalendarApiService::class.java) }
    single { get<Retrofit>(qualifier = org.koin.core.qualifier.named("bmkgRetrofit")).create(WeatherApiService::class.java) }
    single { get<Retrofit>(qualifier = org.koin.core.qualifier.named("ahmadSanusiRetrofit")).create(AhmadSanusiApiService::class.java) }
}
