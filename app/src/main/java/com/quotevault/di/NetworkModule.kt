package com.quotevault.di

import com.quotevault.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                sessionManager = com.quotevault.data.auth.SharedPreferencesSessionManager(context)
            }
            install(Postgrest)
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
    }
}
