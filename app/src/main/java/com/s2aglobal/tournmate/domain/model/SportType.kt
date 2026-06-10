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
            GENERIC -> "Sport"
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
            GENERIC -> "sports"
        }

    val scoreUnitName: String
        get() = when (this) {
            BADMINTON -> "points"
            TENNIS -> "games"
            TABLE_TENNIS -> "points"
            PICKLEBALL -> "points"
            else -> "points"
        }

    val setName: String
        get() = when (this) {
            BADMINTON -> "sets"
            TENNIS -> "sets"
            TABLE_TENNIS -> "games"
            PICKLEBALL -> "games"
            else -> "sets"
        }

    val usesSetScoring: Boolean
        get() = this in listOf(BADMINTON, TENNIS, TABLE_TENNIS, PICKLEBALL)

    val defaultBestOf: Int
        get() = when (this) {
            BADMINTON -> 3
            TENNIS -> 3
            TABLE_TENNIS -> 5
            PICKLEBALL -> 3
            else -> 1
        }

    val defaultPointsPerSet: Int
        get() = when (this) {
            BADMINTON -> 21
            TENNIS -> 6
            TABLE_TENNIS -> 11
            PICKLEBALL -> 11
            else -> 21
        }

    companion object {
        fun fromRawValue(raw: String?): SportType =
            entries.firstOrNull { it.rawValue == raw } ?: BADMINTON
    }
}
