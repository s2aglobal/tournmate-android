package com.s2aglobal.tournmate.domain.model

import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

data class Tournament(
    val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val date: Date = Date(),
    val participantsCount: Int = 0,
    val location: String = "",
    val locationAddress: String = "",
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val statusRaw: String = TournamentStatus.SCHEDULED.rawValue,
    val formatRaw: String = TournamentFormat.OPEN_DOUBLES.rawValue,
    val matchFormatRaw: String = MatchFormat.SINGLE_ELIMINATION.rawValue,
    val randomPairing: Boolean = false,
    val registrationDeadline: Date = Date(),
    val createdAt: Date = Date(),
    val createdBy: String? = null,
    val entryFee: Double? = null,
    val currency: String = "USD",
    val paymentInfo: String? = null,
    val prizeInfo: String? = null,
    val ageGroupRaw: String = AgeGroup.OPEN.rawValue,
    val durationMinutes: Int? = null,
    val formatConfigData: String? = null,
    val scoringConfigData: String? = null,
    val skillDivision: String? = null,
    val countryCode: String? = null,
    val postalCode: String? = null,
    val timeZone: String? = null,
    val sportType: SportType = SportType.BADMINTON,
    /** Original id when [sportType] is the GENERIC stand-in for a sport this build doesn't know; written back on save. */
    val sportTypeRaw: String? = null,
) {
    val status: TournamentStatus
        get() = TournamentStatus.fromRawValue(statusRaw)

    val format: TournamentFormat
        get() = TournamentFormat.fromRawValue(formatRaw)

    val matchFormat: MatchFormat
        get() = MatchFormat.fromRawValue(matchFormatRaw)

    val ageGroup: AgeGroup
        get() = AgeGroup.fromRawValue(ageGroupRaw)

    val formatConfig: FormatConfig
        get() = FormatConfig.decode(formatConfigData, matchFormat)

    /** `true` when the organizer chose scoring rules; older tournaments keep the lenient score checks. */
    val enforcesScoringRules: Boolean
        get() = scoringConfigData != null

    val scoringConfig: ScoringConfig
        get() = ScoringConfig.decodeOrNull(scoringConfigData) ?: sportType.scoringRules.defaultConfig

    /** Matches iOS: the stored deadline is used as-is (missing ones are filled with `defaultDeadline` on read). */
    val effectiveDeadline: Date
        get() = registrationDeadline

    val isRegistrationClosed: Boolean
        get() = Date().after(effectiveDeadline)

    val isFree: Boolean
        get() = entryFee == null || entryFee == 0.0

    val formattedFee: String?
        get() {
            val fee = entryFee ?: return null
            if (fee == 0.0) return null
            return "$currency ${String.format("%.2f", fee)}"
        }

    val isPast: Boolean
        get() = date.before(Date())

    val formattedDuration: String?
        get() {
            val mins = durationMinutes ?: return null
            val hours = mins / 60
            val remaining = mins % 60
            return when {
                hours > 0 && remaining > 0 -> "${hours}h ${remaining}m"
                hours > 0 -> "${hours}h"
                else -> "${remaining}m"
            }
        }

    val hoursUntilStart: Double
        get() {
            val diffMs = date.time - Date().time
            return diffMs.toDouble() / TimeUnit.HOURS.toMillis(1)
        }

    val canCancelOrDelete: Boolean
        get() = hoursUntilStart >= CANCELLATION_CUTOFF_HOURS

    companion object {
        const val CANCELLATION_CUTOFF_HOURS = 3.0

        /**
         * 11:59:59 PM the day before the tournament; if that has passed, 1 hour before
         * the start (but never earlier than 15 minutes from now).
         */
        fun defaultDeadline(forDate: Date): Date {
            val cal = Calendar.getInstance().apply {
                time = forDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            val now = System.currentTimeMillis()
            if (cal.timeInMillis > now) return cal.time
            val oneHourBefore = forDate.time - TimeUnit.HOURS.toMillis(1)
            val minimumDeadline = now + TimeUnit.MINUTES.toMillis(15)
            return Date(maxOf(oneHourBefore, minimumDeadline))
        }
    }
}
