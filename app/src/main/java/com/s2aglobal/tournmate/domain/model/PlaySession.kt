package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class PlaySession(
    val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val venue: String = "",
    val venueAddress: String = "",
    val venueLatitude: Double? = null,
    val venueLongitude: Double? = null,
    val countryCode: String? = null,
    val postalCode: String? = null,
    val date: Date = Date(),
    val durationMinutes: Int? = null,
    val skillLevelRaw: String = SkillLevel.ALL_LEVELS.rawValue,
    val gameTypeRaw: String = CasualGameType.ANY.rawValue,
    val costPerPerson: Double? = null,
    val currency: String = "USD",
    val notes: String? = null,
    val preferredAgeGroupRaw: String = AgeGroup.OPEN.rawValue,
    val statusRaw: String = PlaySessionStatus.ACTIVE.rawValue,
    val hostId: String? = null,
    val hostName: String? = null,
    val hostAvatarId: String? = null,
    val createdAt: Date = Date(),
    val timeZone: String? = null,
    val sportType: SportType = SportType.BADMINTON,
    /** Original id when [sportType] is the GENERIC stand-in for a sport this build doesn't know; written back on save. */
    val sportTypeRaw: String? = null,
    val attendeeIds: List<String> = emptyList(),
    val attendees: List<Player> = emptyList(),
) {
    val skillLevel: SkillLevel
        get() = SkillLevel.fromRawValue(skillLevelRaw)

    val gameType: CasualGameType
        get() = CasualGameType.fromRawValue(gameTypeRaw)

    val preferredAgeGroup: AgeGroup
        get() = AgeGroup.fromRawValue(preferredAgeGroupRaw)

    val status: PlaySessionStatus
        get() = PlaySessionStatus.fromRawValue(statusRaw)

    val hostAvatar: PlayerAvatar
        get() = PlayerAvatar.fromId(hostAvatarId ?: PlayerAvatar.DEFAULT.id)

    val isPast: Boolean
        get() = date.before(Date())

    val isJoinable: Boolean
        get() = !isPast && status == PlaySessionStatus.ACTIVE

    val attendeeCount: Int
        get() = attendeeIds.size

    val hasCost: Boolean
        get() = costPerPerson != null && costPerPerson > 0

    val formattedCost: String?
        get() {
            val cost = costPerPerson ?: return null
            if (cost <= 0) return null
            return "$currency ${String.format("%.2f", cost)}"
        }

    val formattedDuration: String?
        get() {
            val mins = durationMinutes ?: return null
            if (mins <= 0) return null
            if (mins < 60) return "$mins min"
            val hours = mins / 60.0
            return if (hours == kotlin.math.floor(hours)) {
                "${hours.toInt()} hrs"
            } else {
                String.format("%.1f hrs", hours)
            }
        }
}
