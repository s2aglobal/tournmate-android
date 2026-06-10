package com.s2aglobal.tournmate.domain.model

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
    val countryCode: String? = null,
    val postalCode: String? = null,
    val timeZone: String? = null,
    val sportType: SportType = SportType.BADMINTON,
) {
    val status: TournamentStatus
        get() = TournamentStatus.fromRawValue(statusRaw)

    val format: TournamentFormat
        get() = TournamentFormat.fromRawValue(formatRaw)

    val matchFormat: MatchFormat
        get() = MatchFormat.fromRawValue(matchFormatRaw)

    val ageGroup: AgeGroup
        get() = AgeGroup.fromRawValue(ageGroupRaw)

    val effectiveDeadline: Date
        get() {
            val oneHourBefore = Date(date.time - TimeUnit.HOURS.toMillis(1))
            return if (registrationDeadline.before(oneHourBefore)) registrationDeadline else oneHourBefore
        }

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

        fun defaultDeadline(forDate: Date): Date =
            Date(forDate.time - TimeUnit.HOURS.toMillis(1))
    }
}
