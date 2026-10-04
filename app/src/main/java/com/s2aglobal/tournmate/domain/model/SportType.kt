package com.s2aglobal.tournmate.domain.model

enum class SportType(val rawValue: String) {
    BADMINTON("badminton"),
    TENNIS("tennis"),
    TABLE_TENNIS("table_tennis"),
    PICKLEBALL("pickleball"),
    VOLLEYBALL("volleyball"),
    BASKETBALL("basketball"),
    FOOTBALL("football"),
    SOCCER("soccer"),
    CRICKET("cricket"),
    PADEL("padel"),
    SQUASH("squash"),
    BEACH_VOLLEYBALL("beach_volleyball"),
    GOLF("golf"),
    BOWLING("bowling"),
    DARTS("darts"),

    /** Display-only bucket ("Other") for values this build doesn't know. Never selectable. */
    GENERIC("generic");

    val displayName: String
        get() = when (this) {
            BADMINTON -> "Badminton"
            TENNIS -> "Tennis"
            TABLE_TENNIS -> "Table Tennis"
            PICKLEBALL -> "Pickleball"
            VOLLEYBALL -> "Volleyball"
            BASKETBALL -> "Basketball"
            FOOTBALL -> "Football"
            SOCCER -> "Soccer"
            CRICKET -> "Cricket"
            PADEL -> "Padel"
            SQUASH -> "Squash"
            BEACH_VOLLEYBALL -> "Beach Volleyball"
            GOLF -> "Golf"
            BOWLING -> "Bowling"
            DARTS -> "Darts"
            GENERIC -> "Other"
        }

    val icon: String
        get() = when (this) {
            BADMINTON -> "sports_tennis"
            TENNIS -> "sports_tennis"
            TABLE_TENNIS -> "sports_tennis"
            PICKLEBALL -> "sports_tennis"
            VOLLEYBALL -> "sports_volleyball"
            BASKETBALL -> "sports_basketball"
            FOOTBALL -> "sports_football"
            SOCCER -> "sports_soccer"
            CRICKET -> "sports_cricket"
            PADEL -> "sports_tennis"
            SQUASH -> "sports_tennis"
            BEACH_VOLLEYBALL -> "sports_volleyball"
            GOLF -> "sports_golf"
            BOWLING -> "sports"
            DARTS -> "adjust"
            GENERIC -> "sports"
        }

    val inlineName: String
        get() = displayName.lowercase()

    val scoreUnitName: String
        get() = when (this) {
            SOCCER -> "goals"
            CRICKET -> "runs"
            GOLF -> "strokes"
            BOWLING -> "pins"
            else -> "points"
        }

    val setName: String
        get() = when (this) {
            BADMINTON -> "games"
            TENNIS -> "sets"
            TABLE_TENNIS -> "games"
            PICKLEBALL -> "games"
            VOLLEYBALL -> "sets"
            BASKETBALL -> "quarters"
            FOOTBALL -> "quarters"
            SOCCER -> "halves"
            CRICKET -> "innings"
            PADEL -> "sets"
            SQUASH -> "games"
            BEACH_VOLLEYBALL -> "sets"
            GOLF -> "rounds"
            BOWLING -> "games"
            DARTS -> "legs"
            GENERIC -> "sets"
        }

    val setNameSingular: String
        get() = when (this) {
            BADMINTON -> "game"
            TENNIS -> "set"
            TABLE_TENNIS -> "game"
            PICKLEBALL -> "game"
            VOLLEYBALL -> "set"
            BASKETBALL -> "quarter"
            FOOTBALL -> "quarter"
            SOCCER -> "half"
            CRICKET -> "innings"
            PADEL -> "set"
            SQUASH -> "game"
            BEACH_VOLLEYBALL -> "set"
            GOLF -> "round"
            BOWLING -> "game"
            DARTS -> "leg"
            GENERIC -> "set"
        }

    val usesSetScoring: Boolean
        get() = this in listOf(BADMINTON, TENNIS, TABLE_TENNIS, PICKLEBALL, VOLLEYBALL, PADEL, SQUASH, BEACH_VOLLEYBALL)

    val defaultBestOf: Int
        get() = scoringRules.defaultConfig.gamesPerMatch

    val defaultPointsPerSet: Int
        get() = scoringRules.defaultConfig.pointsToWin

    companion object {
        /**
         * Launch-day live sports, in display order. Prefer `SportCatalog` / `LocalSportCatalog`
         * (server-driven `config/sports`); this is only the bundled fallback.
         */
        val SELECTABLE: List<SportType> = listOf(PICKLEBALL, BADMINTON, TENNIS)

        /**
         * Decodes a stored `sportType` / `preferredSport`.
         * Missing (legacy docs) → [BADMINTON]; unknown (newer server/app) → [GENERIC], for display only.
         * Never maps an unknown value to badminton, so it can't leak into the badminton feed.
         */
        fun fromRawValue(raw: String?): SportType {
            if (raw.isNullOrBlank()) return BADMINTON
            return fromKnownRawValue(raw) ?: GENERIC
        }

        /** Exact match on a known id, or null. */
        fun fromKnownRawValue(raw: String?): SportType? =
            entries.firstOrNull { it.rawValue == raw }
    }
}

/** Age divisions organizers can pick for this sport, in display order (BWF / USA Pickleball / USTA). */
val SportType.ageGroups: List<AgeGroup>
    get() = when (this) {
        SportType.PICKLEBALL -> listOf(
            AgeGroup.OPEN, AgeGroup.U12, AgeGroup.U14, AgeGroup.U16, AgeGroup.U18, AgeGroup.SENIOR,
            AgeGroup.VETERANS_35, AgeGroup.MASTERS_50, AgeGroup.GRAND_MASTERS_55, AgeGroup.AGE_60,
            AgeGroup.AGE_65, AgeGroup.AGE_70, AgeGroup.AGE_75, AgeGroup.AGE_80,
        )
        SportType.TENNIS -> listOf(
            AgeGroup.OPEN, AgeGroup.U12, AgeGroup.U14, AgeGroup.U16, AgeGroup.U18, AgeGroup.ADULT_18,
            AgeGroup.MASTERS_40, AgeGroup.GRAND_MASTERS_55, AgeGroup.AGE_65,
        )
        else -> listOf(
            AgeGroup.OPEN, AgeGroup.U13, AgeGroup.U15, AgeGroup.U17, AgeGroup.U19, AgeGroup.U24, AgeGroup.SENIOR,
            AgeGroup.VETERANS_35, AgeGroup.MASTERS_40, AgeGroup.MASTERS_50, AgeGroup.GRAND_MASTERS_55,
        )
    }

/** Self-rated skill divisions (DUPR / USA Pickleball, NTRP). Empty when the sport doesn't use them. */
val SportType.skillDivisions: List<String>
    get() = when (this) {
        SportType.PICKLEBALL -> listOf("2.5", "3.0", "3.5", "4.0", "4.5", "5.0+", "Pro")
        SportType.TENNIS -> listOf("2.5", "3.0", "3.5", "4.0", "4.5", "5.0+")
        else -> emptyList()
    }

/** Name of the rating scale behind [skillDivisions]. */
val SportType.skillRatingName: String
    get() = when (this) {
        SportType.PICKLEBALL -> "DUPR / USA Pickleball rating"
        SportType.TENNIS -> "NTRP rating"
        else -> "Skill rating"
    }

/** `ageGroups` plus [current] when an older event uses a group outside this sport's list. */
fun SportType.ageGroupsIncluding(current: AgeGroup): List<AgeGroup> =
    if (current in ageGroups) ageGroups else ageGroups + current

/** The stored id when [raw] is a sport this build doesn't know (decoded as GENERIC), else null. */
fun unknownSportRaw(raw: String?): String? =
    raw?.takeIf { it.isNotBlank() && SportType.fromKnownRawValue(it) == null }

/** Id to write back: the original unknown id while the model still holds the GENERIC stand-in. */
fun SportType.storedRawValue(originalRaw: String?): String =
    if (this == SportType.GENERIC && !originalRaw.isNullOrBlank()) originalRaw else rawValue
