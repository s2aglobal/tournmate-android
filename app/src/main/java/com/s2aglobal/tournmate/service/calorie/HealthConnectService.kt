package com.s2aglobal.tournmate.service.calorie

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class HealthConnectCalorieResult(
    val calories: Double,
    val fromHealthConnect: Boolean,
)

@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun getRequiredPermissions(): Set<String> = permissions

    suspend fun hasPermissions(): Boolean {
        if (!isAvailable) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            permissions.all { it in granted }
        } catch (_: Exception) {
            false
        }
    }

    /** Active calories recorded between [start] and [end], or null when unavailable / none recorded. */
    suspend fun queryCalories(start: Instant, end: Instant): Double? {
        if (!hasPermissions()) return null
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.sumOf { it.energy.inKilocalories }.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun queryWorkoutCalories(durationMinutes: Int): HealthConnectCalorieResult {
        if (!isAvailable) return HealthConnectCalorieResult(0.0, false)

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val now = Instant.now()
            val start = now.minus((durationMinutes + 30).toLong(), ChronoUnit.MINUTES)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now),
                )
            )

            val totalCalories = response.records.sumOf {
                it.energy.inKilocalories
            }

            if (totalCalories > 0) {
                HealthConnectCalorieResult(totalCalories, true)
            } else {
                HealthConnectCalorieResult(0.0, false)
            }
        } catch (_: Exception) {
            HealthConnectCalorieResult(0.0, false)
        }
    }
}
