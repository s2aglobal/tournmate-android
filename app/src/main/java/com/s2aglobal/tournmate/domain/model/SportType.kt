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
            GENERIC -> "sports"
        }

    val inlineName: String
        get() = displayName.lowercase()

    val scoreUnitName: String
        get() = when (this) {
            SOCCER -> "goals"
            CRICKET -> "runs"
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
            GENERIC -> "set"
        }

    val usesSetScoring: Boolean
        get() = this in listOf(BADMINTON, TENNIS, TABLE_TENNIS, PICKLEBALL, VOLLEYBALL)

    val defaultBestOf: Int
        get() = scoringRules.defaultConfig.gamesPerMatch

    val defaultPointsPerSet: Int
        get() = scoringRules.defaultConfig.pointsToWin

    companion object {
        /** Sports offered in pickers, in display order. */
        val SELECTABLE: List<SportType> = listOf(PICKLEBALL, BADMINTON, TENNIS)

        fun fromRawValue(raw: String?): SportType =
            entries.firstOrNull { it.rawValue == raw } ?: BADMINTON
    }
}
