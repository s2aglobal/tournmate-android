package com.s2aglobal.tournmate.domain.model

/**
 * The fixtures of the server's `check:cricket` "Player stats" section (and the
 * iOS DrawTests block "Cricket player stats"), shared by
 * [CricketPlayerStatsTest] and the parity dump in `DrawParityTest`.
 */
object CricketPlayerStatsFixtures {
    const val A = "regA"
    const val B = "regB"
    val names: (String) -> String = { if (it == A) "Strikers" else "Royals" }

    fun inn(side: String, runs: Int, wickets: Int, overs: String): CricketInnings =
        CricketInnings(side, runs, wickets, CricketOvers.parse(overs, 6) ?: error("bad overs $overs"))

    val card = CricketResult(listOf(inn(A, 158, 7, "20"), inn(B, 159, 4, "18.2")))

    fun bat(t: String, k: String, r: Int, b: Int, out: Boolean = true) = BattingFigures(t, k, k, r, b, out)
    fun bowl(t: String, k: String, b: Int, r: Int, w: Int) = BowlingFigures(t, k, k, b, r, w)

    val good = CricketPlayerStats(
        batting = listOf(bat(A, "P1", 60, 40), bat(A, "P2", 45, 30, false), bat(B, "Q1", 80, 50), bat(B, "guest:Ravi", 70, 55, false)),
        bowling = listOf(bowl(B, "Q2", 24, 30, 3), bowl(A, "P3", 24, 41, 2), bowl(A, "guest:Sam", 20, 35, 1)),
    )

    val m2 = CricketPlayerStats(
        batting = listOf(bat(A, "P1", 20, 10), bat(B, "guest:Ravi", 75, 40), bat(A, "P2", 105, 60, true)),
        bowling = listOf(bowl(A, "P3", 24, 18, 3), bowl(B, "Q2", 24, 25, 3), bowl(B, "Q9", 12, 9, 0)),
    )

    /** The three "PSTATS ..." parity lines, built exactly as iOS `Tools/DrawTests/main.swift` does. */
    fun parityLines(): List<String> {
        val r = CricketRates
        val grid = mutableListOf<String>()
        for (runs in listOf(0, 1, 7, 33, 99, 150)) {
            for (balls in listOf(0, 1, 6, 16, 23, 61)) {
                grid.add(
                    "$runs/$balls sr=${r.formatTenths(r.strikeRateTenths(runs, balls))} " +
                        "econ=${r.formatHundredths(r.economyHundredths(runs, balls))} " +
                        "avg=${r.formatHundredths(r.averageHundredths(runs, balls))}",
                )
            }
        }
        val bl = CricketLeaderboards.batting(listOf(good, m2))
        val wl = CricketLeaderboards.bowling(listOf(good, m2))
        return listOf(
            "PSTATS rates " + grid.joinToString("; "),
            "PSTATS batting " + bl.joinToString(" | ") { "${it.key} ${it.runs} ${it.highestText} ${it.strikeRate} ${it.average}" },
            "PSTATS bowling " + wl.joinToString(" | ") { "${it.key} ${it.wickets} ${it.bestText} ${it.economy} ${it.oversText(6)}" },
        )
    }
}
