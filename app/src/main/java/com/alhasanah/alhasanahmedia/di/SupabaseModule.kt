@file:OptIn(SupabaseInternal::class)
package com.alhasanah.alhasanahmedia.di

import com.alhasanah.alhasanahmedia.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import org.koin.dsl.module
import io.github.jan.supabase.annotations.SupabaseInternal

val supabaseModule = module {
    single<SupabaseClient> {
        require(BuildConfig.SUPABASE_URL.startsWith("https://") && "localhost" !in BuildConfig.SUPABASE_URL) {
            "SUPABASE_URL belum valid. Isi supabase.url di local.properties."
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY belum valid. Isi supabase.anon.key di local.properties."
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {

            httpEngine = Android.create()

            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30000
                }
                install(ContentEncoding) {
                    gzip()
                    deflate()
                }
            }

            // Install plugin Supabase
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
                enableLifecycleCallbacks = true
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    // Deklarasi eksplisit agar Koin tahu cara menyediakan plugin
    single<Auth> { get<SupabaseClient>().auth }
    single<Postgrest> { get<SupabaseClient>().postgrest }
    single<Storage> { get<SupabaseClient>().storage }
    single<Realtime> { get<SupabaseClient>().realtime }
}
