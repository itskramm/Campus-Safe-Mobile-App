package com.example.campussafeapplication.repository

import com.example.campussafeapplication.models.HazardReport
import com.example.campussafeapplication.models.User
import com.example.campussafeapplication.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class HazardReportRepository {
    
    private val client = SupabaseClient.client
    
    /**
     * Create a new hazard report
     */
    suspend fun createReport(report: HazardReport): Result<HazardReport> {
        return try {
            val result = client.from("hazard_reports")
                .insert(report) {
                    select()
                }
                .decodeSingle<HazardReport>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all reports for a specific user
     */
    suspend fun getUserReports(userId: String): Result<List<HazardReport>> {
        return try {
            val reports = client.from("hazard_reports")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<HazardReport>()
            
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all nearby reports (you can add location filtering)
     */
    suspend fun getAllReports(): Result<List<HazardReport>> {
        return try {
            val reports = client.from("hazard_reports")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<HazardReport>()
            
            Result.success(enrichReportsWithReporterNames(reports))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific report by ID
     */
    suspend fun getReportById(reportId: String): Result<HazardReport> {
        return try {
            val report = client.from("hazard_reports")
                .select {
                    filter {
                        eq("id", reportId)
                    }
                }
                .decodeSingle<HazardReport>()
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update a hazard report
     */
    suspend fun updateReport(reportId: String, updates: Map<String, Any>): Result<HazardReport> {
        return try {
            val result = client.from("hazard_reports")
                .update(updates) {
                    filter {
                        eq("id", reportId)
                    }
                    select()
                }
                .decodeSingle<HazardReport>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a hazard report
     */
    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            client.from("hazard_reports")
                .delete {
                    filter {
                        eq("id", reportId)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get reports by status
     */
    suspend fun getReportsByStatus(status: String): Result<List<HazardReport>> {
        return try {
            val reports = client.from("hazard_reports")
                .select {
                    filter {
                        eq("status", status)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<HazardReport>()
            
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun enrichReportsWithReporterNames(reports: List<HazardReport>): List<HazardReport> {
        if (reports.isEmpty()) return reports

        val fallback: (HazardReport) -> String = { report ->
            report.userId?.takeLast(6)?.let { "Reporter $it" } ?: "Campus Reporter"
        }

        return try {
            val users = client.from("users")
                .select()
                .decodeList<User>()

            val namesById = users
                .mapNotNull { user ->
                    val id = user.id ?: return@mapNotNull null
                    val name = user.fullName?.takeIf { it.isNotBlank() }
                        ?: user.email.substringBefore("@")
                    id to name
                }
                .toMap()

            reports.map { report ->
                report.copy(reporterName = namesById[report.userId] ?: fallback(report))
            }
        } catch (_: Exception) {
            reports.map { report ->
                report.copy(reporterName = fallback(report))
            }
        }
    }
}
