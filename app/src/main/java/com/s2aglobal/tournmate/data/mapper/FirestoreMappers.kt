package com.s2aglobal.tournmate.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.s2aglobal.tournmate.domain.model.*
import java.util.Date
import java.util.UUID

fun DocumentSnapshot.toPlayer(): Player? {
    if (!exists()) return null
    val legacyElo = getDouble("elo") ?: 1200.0
    val storedRatings = (get("eloRatings") as? Map<*, *>)?.mapNotNull { (k, v) ->
        val key = k as? String ?: return@mapNotNull null
        val value = (v as? Number)?.toDouble() ?: return@mapNotNull null
        key to value
    }?.toMap() ?: emptyMap()
    val preferredSport = SportType.fromRawValue(getString("preferredSport"))
    return Player(
        id = id.toUuidOrNull() ?: return null,
        name = getString("name") ?: "",
        phone = getString("phone") ?: "",
        email = getString("email") ?: "",
        genderRaw = getString("genderRaw") ?: Gender.PREFER_NOT_TO_SAY.rawValue,
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
        eloRatings = resolveEloRatings(legacyElo, storedRatings, preferredSport),
        preferredSport = preferredSport,
        preferredSportRaw = unknownSportRaw(getString("preferredSport")),
        streak = getLong("streak")?.toInt() ?: 0,
        firebaseUid = getString("firebaseUid"),
        avatarId = getString("avatarId") ?: PlayerAvatar.DEFAULT.id,
        homeCountryCode = getString("homeCountryCode"),
        homePostalCode = getString("homePostalCode"),
        fcmToken = getString("fcmToken"),
        weightKg = getDouble("weightKg"),
        dateOfBirth = getTimestamp("dateOfBirth")?.toDate(),
        playingHand = getString("playingHand"),
        skillLevel = getString("skillLevel"),
    )
}

/**
 * Merges the legacy `elo` field and the `eloRatings` map. `elo` is the source of truth for
 * badminton, unless it was written as the preferred sport's rating (older Android builds).
 */
fun resolveEloRatings(
    legacyElo: Double,
    stored: Map<String, Double>,
    preferredSport: SportType,
): Map<String, Double> {
    val badmintonKey = SportType.BADMINTON.rawValue
    val writtenAsPreferredSport = preferredSport != SportType.BADMINTON &&
        stored[preferredSport.rawValue] == legacyElo &&
        stored.containsKey(badmintonKey)
    return if (writtenAsPreferredSport) stored else stored + (badmintonKey to legacyElo)
}

fun Player.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("name", name)
    put("phone", phone)
    put("email", email.lowercase())
    put("genderRaw", genderRaw)
    put("createdAt", Timestamp(createdAt))
    put("eloRatings", eloRatings)
    put("elo", elo(SportType.BADMINTON))
    put("preferredSport", preferredSport.storedRawValue(preferredSportRaw))
    put("streak", streak)
    put("avatarId", avatarId)
    firebaseUid?.let { put("firebaseUid", it) }
    homeCountryCode?.let { put("homeCountryCode", it) }
    homePostalCode?.let { put("homePostalCode", it) }
    fcmToken?.let { put("fcmToken", it) }
    weightKg?.let { put("weightKg", it) }
    dateOfBirth?.let { put("dateOfBirth", Timestamp(it)) }
    playingHand?.let { put("playingHand", it) }
    skillLevel?.let { put("skillLevel", it) }
}

fun DocumentSnapshot.toTournament(): Tournament? {
    if (!exists()) return null
    return Tournament(
        id = id.toUuidOrNull() ?: return null,
        title = getString("title") ?: "",
        date = getTimestamp("date")?.toDate() ?: Date(),
        participantsCount = getLong("participantsCount")?.toInt() ?: 0,
        location = getString("location") ?: "",
        locationAddress = getString("locationAddress") ?: "",
        locationLatitude = getDouble("locationLatitude"),
        locationLongitude = getDouble("locationLongitude"),
        statusRaw = getString("statusRaw") ?: TournamentStatus.SCHEDULED.rawValue,
        formatRaw = getString("formatRaw") ?: TournamentFormat.OPEN_DOUBLES.rawValue,
        matchFormatRaw = getString("matchFormatRaw") ?: MatchFormat.SINGLE_ELIMINATION.rawValue,
        randomPairing = getBoolean("randomPairing") ?: false,
        registrationDeadline = getTimestamp("registrationDeadline")?.toDate()
            ?: Tournament.defaultDeadline(getTimestamp("date")?.toDate() ?: Date()),
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
        createdBy = getString("createdBy"),
        entryFee = getDouble("entryFee"),
        currency = getString("currency") ?: "USD",
        paymentInfo = getString("paymentInfo"),
        prizeInfo = getString("prizeInfo"),
        ageGroupRaw = getString("ageGroupRaw") ?: AgeGroup.OPEN.rawValue,
        durationMinutes = getLong("durationMinutes")?.toInt(),
        formatConfigData = getString("formatConfigData"),
        scoringConfigData = getString("scoringConfigData"),
        skillDivision = getString("skillDivision"),
        countryCode = getString("countryCode"),
        postalCode = getString("postalCode"),
        timeZone = getString("timeZone"),
        sportType = SportType.fromRawValue(getString("sportType")),
        sportTypeRaw = unknownSportRaw(getString("sportType")),
        registrationCount = getLong("registrationCount")?.toInt(),
    )
}

/**
 * Never includes `registrationCount`: the server's registration triggers own it.
 * Create uses a plain set on a brand-new doc; updates use SetOptions.merge(), so the
 * server-maintained count is left untouched.
 */
fun Tournament.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("title", title)
    put("date", Timestamp(date))
    put("participantsCount", participantsCount)
    put("location", location)
    put("locationAddress", locationAddress)
    locationLatitude?.let { put("locationLatitude", it) }
    locationLongitude?.let { put("locationLongitude", it) }
    put("statusRaw", statusRaw)
    put("formatRaw", formatRaw)
    put("matchFormatRaw", matchFormatRaw)
    put("randomPairing", randomPairing)
    put("registrationDeadline", Timestamp(registrationDeadline))
    put("createdAt", Timestamp(createdAt))
    createdBy?.let { put("createdBy", it) }
    entryFee?.let { put("entryFee", it) }
    put("currency", currency)
    paymentInfo?.let { put("paymentInfo", it) }
    prizeInfo?.let { put("prizeInfo", it) }
    put("ageGroupRaw", ageGroupRaw)
    durationMinutes?.let { put("durationMinutes", it) }
    formatConfigData?.let { put("formatConfigData", it) }
    scoringConfigData?.let { put("scoringConfigData", it) }
    skillDivision?.let { put("skillDivision", it) }
    countryCode?.let { put("countryCode", it) }
    postalCode?.let { put("postalCode", it) }
    timeZone?.let { put("timeZone", it) }
    put("sportType", sportType.storedRawValue(sportTypeRaw))
}

fun Match.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("tournamentId", tournamentId)
    put("teamAId", teamAId)
    put("teamBId", teamBId)
    put("statusRaw", statusRaw)
    put("createdAt", Timestamp(createdAt))
    round?.let { put("round", it) }
    bracketPosition?.let { put("bracketPosition", it) }
    groupLabel?.let { put("groupLabel", it) }
    put("sportType", sportType.storedRawValue(sportTypeRaw))
    scoreA?.let { put("scoreA", it) }
    scoreB?.let { put("scoreB", it) }
    winnerRegistrationId?.let { put("winnerRegistrationId", it) }
    submittedBy?.let { put("submittedBy", it) }
    confirmedBy?.let { put("confirmedBy", it) }
    if (setScores.isNotEmpty()) {
        put("setScores", setScores.map { mapOf("teamAPoints" to it.teamAPoints, "teamBPoints" to it.teamBPoints) })
    }
}

fun DocumentSnapshot.toRegistration(): Registration? {
    if (!exists()) return null
    return Registration(
        id = id.toUuidOrNull() ?: return null,
        tournamentId = getString("tournamentId") ?: "",
        playerId = getString("playerId") ?: "",
        partnerId = getString("partnerId"),
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
    )
}

fun DocumentSnapshot.toMatch(): Match? {
    if (!exists()) return null
    val setScoresRaw = get("setScores") as? List<*>
    val setScores = setScoresRaw?.mapNotNull { raw ->
        when (raw) {
            is Map<*, *> -> {
                val a = (raw["teamAPoints"] as? Number)?.toInt() ?: return@mapNotNull null
                val b = (raw["teamBPoints"] as? Number)?.toInt() ?: return@mapNotNull null
                SetScore(a, b)
            }
            is List<*> -> {
                val a = (raw.getOrNull(0) as? Number)?.toInt() ?: return@mapNotNull null
                val b = (raw.getOrNull(1) as? Number)?.toInt() ?: return@mapNotNull null
                SetScore(a, b)
            }
            else -> null
        }
    } ?: emptyList()

    return Match(
        id = id.toUuidOrNull() ?: return null,
        tournamentId = getString("tournamentId") ?: "",
        teamAId = getString("teamAId") ?: "",
        teamBId = getString("teamBId") ?: "",
        winnerRegistrationId = getString("winnerRegistrationId"),
        scoreA = getLong("scoreA")?.toInt(),
        scoreB = getLong("scoreB")?.toInt(),
        setScores = setScores,
        statusRaw = getString("statusRaw") ?: MatchStatus.SCHEDULED.rawValue,
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
        round = getLong("round")?.toInt(),
        bracketPosition = getLong("bracketPosition")?.toInt(),
        groupLabel = getString("groupLabel"),
        sportType = SportType.fromRawValue(getString("sportType")),
        sportTypeRaw = unknownSportRaw(getString("sportType")),
        submittedBy = getString("submittedBy"),
        confirmedBy = getString("confirmedBy"),
    )
}

fun DocumentSnapshot.toPlaySession(): PlaySession? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val attendeeIds = get("attendeeIds") as? List<String> ?: emptyList()

    return PlaySession(
        id = id.toUuidOrNull() ?: return null,
        title = getString("title") ?: "",
        venue = getString("venue") ?: "",
        venueAddress = getString("venueAddress") ?: "",
        venueLatitude = getDouble("venueLatitude"),
        venueLongitude = getDouble("venueLongitude"),
        countryCode = getString("countryCode"),
        postalCode = getString("postalCode"),
        date = getTimestamp("date")?.toDate() ?: Date(),
        durationMinutes = getLong("durationMinutes")?.toInt(),
        skillLevelRaw = getString("skillLevel") ?: SkillLevel.ALL_LEVELS.rawValue,
        gameTypeRaw = getString("gameType") ?: CasualGameType.ANY.rawValue,
        costPerPerson = getDouble("costPerPerson"),
        currency = getString("currency") ?: "USD",
        notes = getString("notes"),
        preferredAgeGroupRaw = getString("preferredAgeGroup") ?: AgeGroup.OPEN.rawValue,
        statusRaw = getString("status") ?: PlaySessionStatus.ACTIVE.rawValue,
        hostId = getString("hostId"),
        hostName = getString("hostName"),
        hostAvatarId = getString("hostAvatarId"),
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
        timeZone = getString("timeZone"),
        sportType = SportType.fromRawValue(getString("sportType")),
        sportTypeRaw = unknownSportRaw(getString("sportType")),
        attendeeIds = attendeeIds,
    )
}

fun DocumentSnapshot.toPlayerRating(): PlayerRating? {
    if (!exists()) return null
    return PlayerRating(
        id = id.toUuidOrNull() ?: return null,
        playerId = getString("playerId") ?: "",
        raterId = getString("raterId") ?: "",
        tournamentId = getString("tournamentId"),
        stars = getLong("stars")?.toInt()?.coerceIn(1, 5) ?: 3,
        comment = getString("comment"),
        createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
    )
}

fun DocumentSnapshot.toCalorieRecord(): CalorieRecord? {
    if (!exists()) return null
    return CalorieRecord(
        id = id,
        sessionId = getString("sessionId")?.toUuidOrNull() ?: UUID.randomUUID(),
        playerId = getString("playerId")?.toUuidOrNull() ?: UUID.randomUUID(),
        calories = getDouble("calories") ?: 0.0,
        source = CalorieSource.fromRawValue(getString("source")),
        weightUsedKg = getDouble("weightUsedKg") ?: 70.0,
        durationMinutes = getLong("durationMinutes")?.toInt() ?: 120,
        date = getTimestamp("date")?.toDate() ?: Date(),
        sessionTitle = getString("sessionTitle"),
        activityType = CalorieActivityType.fromRawValue(getString("activityType")),
    )
}

private fun String.toUuidOrNull(): UUID? = try {
    UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    try {
        UUID.fromString(this.lowercase())
    } catch (_: IllegalArgumentException) {
        null
    }
}
