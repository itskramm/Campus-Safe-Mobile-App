package com.example.campussafeapplication.repository

import com.example.campussafeapplication.models.SafetyTip
import com.example.campussafeapplication.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SafetyTipRepository {
    
    private val client = SupabaseClient.client
    
    /**
     * Get all safety tips
     */
    suspend fun getAllSafetyTips(): Result<List<SafetyTip>> {
        return try {
            val tips = client.from("safety_tips")
                .select {
                    order("category", Order.ASCENDING)
                    order("title", Order.ASCENDING)
                }
                .decodeList<SafetyTip>()
            
            Result.success(tips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get safety tips by category
     */
    suspend fun getSafetyTipsByCategory(category: String): Result<List<SafetyTip>> {
        return try {
            val tips = client.from("safety_tips")
                .select {
                    filter {
                        eq("category", category)
                    }
                    order("title", Order.ASCENDING)
                }
                .decodeList<SafetyTip>()
            
            Result.success(tips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new safety tip (admin function)
     */
    suspend fun createSafetyTip(tip: SafetyTip): Result<SafetyTip> {
        return try {
            val result = client.from("safety_tips")
                .insert(tip) {
                    select()
                }
                .decodeSingle<SafetyTip>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
