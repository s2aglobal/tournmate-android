package com.s2aglobal.tournmate.service.calorie

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.s2aglobal.tournmate.domain.model.SportType
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

    /**
     * Active calories recorded between [start] and [end], or null when unavailable / none recorded.
     * When [sport] is set, calories from exercise sessions recorded as that sport are preferred;
     * everything in the window is used only if none match.
     */
    suspend fun queryCalories(start: Instant, end: Instant, sport: SportType? = null): Double? {
        if (!hasPermissions()) return null
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            matchingSessionCalories(client, start, end, sport)?.let { return it }
            activeCalories(client, start, end).takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    /** Calories inside exercise sessions recorded as [sport]; a walk or run in the same window shouldn't count as court time. */
    private suspend fun matchingSessionCalories(client: HealthConnectClient, start: Instant, end: Instant, sport: SportType?): Double? {
        val exerciseType = sport?.healthConnectExerciseType ?: return null
        val sessions = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        ).records.filter { it.exerciseType == exerciseType }
        if (sessions.isEmpty()) return null
        val total = sessions.sumOf { activeCalories(client, it.startTime, it.endTime) }
        return total.takeIf { it > 0 }
    }

    private suspend fun activeCalories(client: HealthConnectClient, start: Instant, end: Instant): Double =
        client.readRecords(
            ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        ).records.sumOf { it.energy.inKilocalories }

    suspend fun queryWorkoutCalories(durationMinutes: Int, sport: SportType? = null): HealthConnectCalorieResult {
        if (!isAvailable) return HealthConnectCalorieResult(0.0, false)

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val now = Instant.now()
            val start = now.minus((durationMinutes + 30).toLong(), ChronoUnit.MINUTES)

            val totalCalories = matchingSessionCalories(client, start, now, sport)
                ?: activeCalories(client, start, now)

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

/** Health Connect exercise type for this sport, if one exists (there is no pickleball type). */
val SportType.healthConnectExerciseType: Int?
    get() = when (this) {
        SportType.BADMINTON -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
        SportType.TENNIS -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
        SportType.TABLE_TENNIS -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
        SportType.VOLLEYBALL -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
        SportType.BASKETBALL -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
        SportType.SOCCER -> ExerciseSessionRecord.EXERCISE_TYPE_SOCCER
        SportType.FOOTBALL -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
        SportType.CRICKET -> ExerciseSessionRecord.EXERCISE_TYPE_CRICKET
        SportType.PICKLEBALL, SportType.GENERIC -> null
    }
