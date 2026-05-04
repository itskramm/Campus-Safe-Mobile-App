package com.example.campussafeapplication.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

// Corrected SupabaseClient.kt
object SupabaseClient {
    private const val SUPABASE_URL = "https://uxafytqyohzjqcbbmyir.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV4YWZ5dHF5b2h6anFjYmJteWlyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU3NDAzNTIsImV4cCI6MjA5MTMxNjM1Mn0.6oHdmKIEEEScO8YuceT9tFc7ZxgJLBy0jIdTkh54MiM"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
