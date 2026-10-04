package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class Player(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val genderRaw: String = Gender.PREFER_NOT_TO_SAY.rawValue,
    val createdAt: Date = Date(),
    val eloRatings: Map<String, Double> = mapOf("badminton" to 1200.0),
    val preferredSport: SportType = SportType.BADMINTON,
    /** Original id when [preferredSport] is the GENERIC stand-in for a sport this build doesn't know; written back on save. */
    val preferredSportRaw: String? = null,
    val streak: Int = 0,
    val firebaseUid: String? = null,
    val avatarId: String = PlayerAvatar.DEFAULT.id,
    val homeCountryCode: String? = null,
    val homePostalCode: String? = null,
    val fcmToken: String? = null,
    val weightKg: Double? = null,
    val dateOfBirth: Date? = null,
    /** Dominant hand from profile setup ("left" / "right"); null if never set. */
    val playingHand: String? = null,
    /** Self-reported level from profile setup ("beginner" / "intermediate" / "advanced" / "pro"). */
    val skillLevel: String? = null,
) {
    val gender: Gender
        get() = Gender.fromRawValue(genderRaw)

    val elo: Double
        get() = eloRatings[preferredSport.rawValue] ?: 1200.0

    val avatar: PlayerAvatar
        get() = PlayerAvatar.fromId(avatarId)

    val age: Int?
        get() {
            val dob = dateOfBirth ?: return null
            val now = Date()
            val diffMs = now.time - dob.time
            return (diffMs / (365.25 * 24 * 60 * 60 * 1000)).toInt()
        }

    fun elo(forSport: SportType): Double =
        eloRatings[forSport.rawValue] ?: 1200.0

    fun withElo(elo: Double, forSport: SportType): Player =
        copy(eloRatings = eloRatings + (forSport.rawValue to elo))
}
