package com.s2aglobal.tournmate.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsTennis

/**
 * A plain-language summary of the ITF Rules of Tennis for recreational and
 * club players. Always defer to the current official rules for sanctioned play.
 *
 * Source: https://www.itftennis.com/en/about-us/governance/rules-and-regulations/
 */
object TennisRules {

    val sections: List<RuleSection> = listOf(
        RuleSection(
            title = "The Court",
            icon = Icons.Filled.GridOn,
            subtitle = "Dimensions, lines, and the net",
            rules = listOf(
                Rule(
                    "Court Dimensions",
                    """
                    • The court is 78 ft (23.77 m) long.
                    • Singles width is 27 ft (8.23 m); doubles adds the alleys for 36 ft (10.97 m).
                    • Service boxes extend 21 ft (6.40 m) from the net on each side.
                    """.trimIndent(),
                ),
                Rule(
                    "The Net",
                    """
                    • The net is 3 ft (0.914 m) high at the centre and 3.5 ft (1.07 m) at the posts.
                    • A centre strap holds it at the correct height.
                    """.trimIndent(),
                ),
                Rule(
                    "In or Out",
                    """
                    • A ball touching any part of a line is in.
                    • In singles the alleys are out; in doubles they are in after the serve.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Serving",
            icon = Icons.Filled.NorthEast,
            subtitle = "Position, faults, and lets",
            rules = listOf(
                Rule(
                    "Where to Serve From",
                    """
                    • Stand behind the baseline, between the centre mark and the sideline.
                    • Start each game serving from the right (deuce) side, then alternate each point.
                    • Serve diagonally into the opposite service box.
                    """.trimIndent(),
                ),
                Rule(
                    "Faults",
                    """
                    • You get two attempts. Missing both is a double fault and loses the point.
                    • Stepping on or over the baseline before contact is a foot fault.
                    """.trimIndent(),
                ),
                Rule(
                    "Lets",
                    """
                    • A serve that clips the net and lands in the correct box is a let — replay that serve.
                    • A let is also called if the receiver wasn't ready.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Scoring",
            icon = Icons.Filled.Numbers,
            subtitle = "Points, games, sets, and tiebreaks",
            rules = listOf(
                Rule(
                    "Points in a Game",
                    """
                    • Points go love (0), 15, 30, 40, then game.
                    • At 40-40 (deuce), you need two points in a row: advantage, then game.
                    • Some formats use no-ad scoring: the next point after deuce wins.
                    """.trimIndent(),
                ),
                Rule(
                    "Games and Sets",
                    """
                    • A set is won by the first player to win 6 games with a 2-game lead (6-4, 7-5).
                    • At 6-6, a tiebreak decides the set (7-6).
                    • Matches are usually best of 3 sets.
                    """.trimIndent(),
                ),
                Rule(
                    "Tiebreaks",
                    """
                    • First to 7 points, win by 2.
                    • The player due to serve serves one point, then players alternate every two points.
                    • Change ends every 6 points.
                    """.trimIndent(),
                ),
                Rule(
                    "Entering Scores in TournMate",
                    """
                    • Enter each set as games won (for example 6-4 or 7-6).
                    • A tiebreak set is entered as 7-6.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "During a Point",
            icon = Icons.Filled.SportsTennis,
            subtitle = "What wins and loses a rally",
            rules = listOf(
                Rule(
                    "You Lose the Point If",
                    """
                    • The ball bounces twice on your side before you return it.
                    • Your return lands outside the court or hits the net and stays on your side.
                    • You or your racket touch the net or the opponent's court while the ball is in play.
                    • You volley a serve before it bounces.
                    • The ball touches you or your clothing.
                    """.trimIndent(),
                ),
                Rule(
                    "Volleys",
                    """
                    • You may hit the ball before it bounces (a volley), except on the serve return.
                    • Reaching over the net is only allowed to follow through after hitting on your side.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Doubles",
            icon = Icons.Filled.People,
            subtitle = "Serving order and positions",
            rules = listOf(
                Rule(
                    "Serving and Receiving",
                    """
                    • Partners take turns serving whole games; the order stays the same for the set.
                    • Each partner receives from the same side for the whole set.
                    • After the serve, either partner may hit the ball.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Line Calls & Etiquette",
            icon = Icons.Filled.PanTool,
            subtitle = "Fair play without umpires",
            rules = listOf(
                Rule(
                    "Making Line Calls",
                    """
                    • Call the lines on your own side, promptly and clearly.
                    • If you're not sure, the ball is in.
                    • The server announces the score before each first serve.
                    """.trimIndent(),
                ),
                Rule(
                    "Good Sportsmanship",
                    """
                    • Wait for a point to finish before walking behind a neighbouring court.
                    • Return stray balls between points.
                    • Shake hands at the net after the match.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Key Numbers",
            icon = Icons.Filled.FormatListNumbered,
            subtitle = "Quick reference",
            rules = listOf(
                Rule(
                    "At a Glance",
                    """
                    • 78 × 27 ft (singles) / 78 × 36 ft (doubles) — Court size.
                    • 3 ft — Net height at the centre.
                    • 6 games (win by 2) — Wins a set; tiebreak at 6-6.
                    • 7 points (win by 2) — Wins a tiebreak.
                    • 2 — Serve attempts per point.
                    """.trimIndent(),
                ),
            ),
        ),
    )
}
