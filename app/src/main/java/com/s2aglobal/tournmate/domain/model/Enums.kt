package com.s2aglobal.tournmate.domain.model

enum class TournamentStatus(val rawValue: String) {
    SCHEDULED("scheduled"),
    CANCELLED("cancelled");

    companion object {
        fun fromRawValue(raw: String?): TournamentStatus =
            entries.firstOrNull { it.rawValue == raw } ?: SCHEDULED
    }
}

enum class TournamentFormat(val rawValue: String) {
    MENS_SINGLES("mensSingles"),
    WOMENS_SINGLES("womensSingles"),
    OPEN_SINGLES("openSingles"),
    MENS_DOUBLES("mensDoubles"),
    WOMENS_DOUBLES("womensDoubles"),
    MIXED_DOUBLES("mixedDoubles"),
    OPEN_DOUBLES("openDoubles"),
    FIXED_DOUBLES("fixedDoubles");

    val isSingles: Boolean
        get() = this in listOf(MENS_SINGLES, WOMENS_SINGLES, OPEN_SINGLES)

    val isDoubles: Boolean
        get() = !isSingles

    val displayName: String
        get() = when (this) {
            MENS_SINGLES -> "Men's Singles"
            WOMENS_SINGLES -> "Women's Singles"
            OPEN_SINGLES -> "Open Singles"
            MENS_DOUBLES -> "Men's Doubles"
            WOMENS_DOUBLES -> "Women's Doubles"
            MIXED_DOUBLES -> "Mixed Doubles"
            OPEN_DOUBLES -> "Open Doubles"
            FIXED_DOUBLES -> "Open Doubles"
        }

    val shortName: String
        get() = when (this) {
            MENS_SINGLES -> "MS"
            WOMENS_SINGLES -> "WS"
            OPEN_SINGLES -> "OS"
            MENS_DOUBLES -> "MD"
            WOMENS_DOUBLES -> "WD"
            MIXED_DOUBLES -> "XD"
            OPEN_DOUBLES -> "OD"
            FIXED_DOUBLES -> "OD"
        }

    companion object {
        fun fromRawValue(raw: String?): TournamentFormat =
            entries.firstOrNull { it.rawValue == raw } ?: OPEN_DOUBLES

        val singlesTypes: List<TournamentFormat>
            get() = listOf(MENS_SINGLES, WOMENS_SINGLES, OPEN_SINGLES)

        val doublesTypes: List<TournamentFormat>
            get() = listOf(MENS_DOUBLES, WOMENS_DOUBLES, MIXED_DOUBLES, OPEN_DOUBLES)
    }
}

enum class MatchFormat(val rawValue: String) {
    SINGLE_ELIMINATION("singleElimination"),
    DOUBLE_ELIMINATION("doubleElimination"),
    ROUND_ROBIN("roundRobin"),
    GROUP_KNOCKOUT("groupKnockout"),
    SWISS("swiss"),
    MANUAL_DRAW("manualDraw");

    val displayName: String
        get() = when (this) {
            SINGLE_ELIMINATION -> "Single Elimination"
            DOUBLE_ELIMINATION -> "Double Elimination"
            ROUND_ROBIN -> "Round Robin"
            GROUP_KNOCKOUT -> "Group + Knockout"
            SWISS -> "Swiss System"
            MANUAL_DRAW -> "Manual Draw"
        }

    companion object {
        fun fromRawValue(raw: String?): MatchFormat =
            entries.firstOrNull { it.rawValue == raw } ?: SINGLE_ELIMINATION
    }
}

enum class MatchStatus(val rawValue: String) {
    SCHEDULED("scheduled"),
    SCORE_SUBMITTED("scoreSubmitted"),
    DISPUTED("disputed"),
    FINISHED("finished");

    companion object {
        fun fromRawValue(raw: String?): MatchStatus =
            entries.firstOrNull { it.rawValue == raw } ?: SCHEDULED
    }
}

enum class Gender(val rawValue: String) {
    MALE("male"),
    FEMALE("female"),
    PREFER_NOT_TO_SAY("preferNotToSay");

    val displayName: String
        get() = when (this) {
            MALE -> "Male"
            FEMALE -> "Female"
            PREFER_NOT_TO_SAY -> "Prefer not to say"
        }

    companion object {
        fun fromRawValue(raw: String?): Gender =
            entries.firstOrNull { it.rawValue == raw } ?: PREFER_NOT_TO_SAY
    }
}

enum class AgeGroup(val rawValue: String) {
    OPEN("open"),
    U13("u13"),
    U15("u15"),
    U17("u17"),
    U19("u19"),
    U24("u24"),
    SENIOR("senior"),
    VETERANS_35("veterans35"),
    MASTERS_40("masters40"),
    MASTERS_50("masters50"),
    GRAND_MASTERS_55("grandMasters55");

    val displayName: String
        get() = when (this) {
            OPEN -> "Open (All Ages)"
            U13 -> "Under 13"
            U15 -> "Under 15 (Sub-Junior)"
            U17 -> "Under 17 (Junior)"
            U19 -> "Under 19 (Youth)"
            U24 -> "Under 24 (Young Adult)"
            SENIOR -> "Senior (19+)"
            VETERANS_35 -> "Veterans (35+)"
            MASTERS_40 -> "Masters (40+)"
            MASTERS_50 -> "Masters (50+)"
            GRAND_MASTERS_55 -> "Grand Masters (55+)"
        }

    val minAge: Int?
        get() = when (this) {
            OPEN -> null
            SENIOR, VETERANS_35 -> 35
            MASTERS_40 -> 40
            MASTERS_50 -> 50
            GRAND_MASTERS_55 -> 55
            else -> null
        }

    val maxAge: Int?
        get() = when (this) {
            OPEN -> null
            U13 -> 12
            U15 -> 14
            U17 -> 16
            U19 -> 18
            U24 -> 23
            else -> null
        }

    fun isEligible(age: Int): Boolean {
        if (this == OPEN) return true
        val min = minAge
        val max = maxAge
        return when {
            min != null && max != null -> age in min..max
            min != null -> age >= min
            max != null -> age <= max
            else -> true
        }
    }

    companion object {
        fun fromRawValue(raw: String?): AgeGroup =
            entries.firstOrNull { it.rawValue == raw } ?: OPEN
    }
}

enum class SkillLevel(val rawValue: String) {
    ALL_LEVELS("allLevels"),
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    val displayName: String
        get() = when (this) {
            ALL_LEVELS -> "All Levels"
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            ADVANCED -> "Advanced"
        }

    companion object {
        fun fromRawValue(raw: String?): SkillLevel =
            entries.firstOrNull { it.rawValue == raw } ?: ALL_LEVELS
    }
}

enum class CasualGameType(val rawValue: String) {
    ANY("any"),
    SINGLES("singles"),
    DOUBLES("doubles"),
    MIXED("mixed");

    val displayName: String
        get() = when (this) {
            ANY -> "Any"
            SINGLES -> "Singles"
            DOUBLES -> "Doubles"
            MIXED -> "Mixed"
        }

    companion object {
        fun fromRawValue(raw: String?): CasualGameType =
            entries.firstOrNull { it.rawValue == raw } ?: ANY
    }
}

enum class PlaySessionStatus(val rawValue: String) {
    ACTIVE("active"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    companion object {
        fun fromRawValue(raw: String?): PlaySessionStatus =
            entries.firstOrNull { it.rawValue == raw } ?: ACTIVE
    }
}

enum class CalorieSource(val rawValue: String) {
    HEALTH_CONNECT("healthkit"),
    ESTIMATED("estimated");

    companion object {
        fun fromRawValue(raw: String?): CalorieSource =
            entries.firstOrNull { it.rawValue == raw } ?: ESTIMATED
    }
}

enum class CalorieActivityType(val rawValue: String) {
    OPEN_PLAY("openPlay"),
    TOURNAMENT("tournament"),
    QUICK_PLAY("quickPlay");

    companion object {
        fun fromRawValue(raw: String?): CalorieActivityType =
            entries.firstOrNull { it.rawValue == raw } ?: OPEN_PLAY
    }
}
