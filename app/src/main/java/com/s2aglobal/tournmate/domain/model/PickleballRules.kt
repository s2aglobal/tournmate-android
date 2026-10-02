package com.s2aglobal.tournmate.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SwapVert

/**
 * A plain-language summary of the USA Pickleball rules for recreational and club players.
 * Always defer to the current official rulebook for sanctioned play.
 *
 * Source: https://usapickleball.org/what-is-pickleball/official-rules/
 */
object PickleballRules {

    val sections: List<RuleSection> = listOf(
        RuleSection(
            title = "The Court",
            icon = Icons.Filled.GridOn,
            subtitle = "Dimensions, lines, and the kitchen",
            rules = listOf(
                Rule(
                    "Court Dimensions",
                    """
                    • The court is 20 ft wide × 44 ft long (6.10 m × 13.41 m) for both singles and doubles.
                    • A centerline splits each side into a left and right service court.
                    • The same court size as doubles badminton, so many venues share lines.
                    """.trimIndent(),
                ),
                Rule(
                    "The Net",
                    """
                    • The net is 36 in (91.4 cm) high at the sidelines and 34 in (86.4 cm) at the center.
                    • Net posts sit outside the sidelines.
                    """.trimIndent(),
                ),
                Rule(
                    "The Non-Volley Zone (Kitchen)",
                    """
                    • The non-volley zone extends 7 ft (2.13 m) from the net on each side.
                    • Its line is part of the zone — touching the line counts as being in the kitchen.
                    """.trimIndent(),
                ),
                Rule(
                    "In or Out",
                    """
                    • A ball touching any part of the baseline, sideline, or centerline is in.
                    • On the serve, a ball landing on the non-volley zone line is a fault (short).
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Serving",
            icon = Icons.Filled.NorthEast,
            subtitle = "Volley serve, drop serve, and placement",
            rules = listOf(
                Rule(
                    "Where to Serve From",
                    """
                    • At contact, at least one foot must be on the playing surface behind the baseline.
                    • Neither foot may touch the court or baseline, and feet must stay within the imaginary extensions of the centerline and sideline.
                    • Serve diagonally (cross-court) into the opponent's service court.
                    • The serve must clear the non-volley zone, including its line.
                    """.trimIndent(),
                ),
                Rule(
                    "Volley Serve",
                    """
                    • Hit the ball out of the air without letting it bounce.
                    • The arm must move in an upward arc.
                    • Contact the ball below the waist (navel level).
                    • The highest part of the paddle head must be below the wrist at contact.
                    """.trimIndent(),
                ),
                Rule(
                    "Drop Serve",
                    """
                    • Drop the ball from any height (no throwing or tossing it down) and hit it after it bounces.
                    • The volley-serve motion restrictions don't apply to a drop serve.
                    """.trimIndent(),
                ),
                Rule(
                    "Serve Timing and Faults",
                    """
                    • The server gets one attempt per serve.
                    • A serve that clips the net and lands in the correct service court is in play — there are no service lets.
                    • The server should serve within 10 seconds after the score is called.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Two-Bounce Rule",
            icon = Icons.Filled.SwapVert,
            subtitle = "Let it bounce twice before volleying",
            rules = listOf(
                Rule(
                    "How It Works",
                    """
                    • The serve must bounce once before the receiver returns it.
                    • The return must bounce once before the serving team hits it.
                    • After these two bounces, players may volley or play off the bounce.
                    """.trimIndent(),
                ),
                Rule(
                    "Why It Matters",
                    """
                    • It stops the serving team from rushing the net and volleying the return.
                    • It's why the serving team usually starts back and the third shot is often a soft "drop."
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Kitchen Rules",
            icon = Icons.Filled.CropFree,
            subtitle = "No volleys inside the non-volley zone",
            rules = listOf(
                Rule(
                    "No Volleying in the Kitchen",
                    """
                    • You may not volley (hit the ball before it bounces) while touching the non-volley zone or its line.
                    • Anything you're wearing or carrying, including your paddle, counts.
                    """.trimIndent(),
                ),
                Rule(
                    "Momentum Counts",
                    """
                    • If your momentum after a volley carries you into the kitchen, it's a fault — even if the ball is already dead.
                    • Both feet must be re-established outside the zone before you can volley again.
                    """.trimIndent(),
                ),
                Rule(
                    "When You Can Enter",
                    """
                    • You can step into the kitchen at any time to play a ball that has bounced.
                    • You can stand in the kitchen when you aren't hitting a volley.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Scoring",
            icon = Icons.Filled.Numbers,
            subtitle = "Side-out scoring, rally scoring, calling the score",
            rules = listOf(
                Rule(
                    "Winning a Game",
                    """
                    • Games are usually played to 11 points, win by 2.
                    • Tournaments may also play games to 15 or 21, and matches as one game or best of three.
                    """.trimIndent(),
                ),
                Rule(
                    "Side-Out Scoring",
                    """
                    • Only the serving side can score a point.
                    • If the serving side loses the rally, the serve passes to the next server or to the other team (a "side out").
                    """.trimIndent(),
                ),
                Rule(
                    "Rally Scoring",
                    """
                    • Some events use rally scoring: every rally wins a point, regardless of who served.
                    • Check your tournament's settings — TournMate shows which system a tournament uses.
                    """.trimIndent(),
                ),
                Rule(
                    "Calling the Score (Doubles)",
                    """
                    • Call three numbers before each serve: serving team score, receiving team score, server number (1 or 2).
                    • A game starts at "0-0-2" because only one player on the first serving team serves before the first side out.
                    • The server stands on the right when their team's score is even and on the left when it's odd.
                    """.trimIndent(),
                ),
                Rule(
                    "Calling the Score (Singles)",
                    """
                    • Call two numbers: server's score, then receiver's score.
                    • Serve from the right when your score is even and from the left when it's odd.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Faults",
            icon = Icons.Filled.Block,
            subtitle = "What ends a rally",
            rules = listOf(
                Rule(
                    "Common Faults",
                    """
                    • Hitting the ball out of bounds or into the net.
                    • Volleying before the two-bounce rule allows it.
                    • Volleying from inside the kitchen, or momentum carrying you in after a volley.
                    • The ball bouncing twice on your side before you return it.
                    • The ball touching you or your clothing (other than your paddle hand below the wrist).
                    • Touching the net, net posts, or the opponent's court with your body or paddle while the ball is in play.
                    """.trimIndent(),
                ),
                Rule(
                    "Serve Faults",
                    """
                    • Foot on or over the baseline at contact.
                    • Serve lands in the kitchen, on the kitchen line, or outside the correct service court.
                    • An illegal volley-serve motion.
                    """.trimIndent(),
                ),
            ),
        ),
        RuleSection(
            title = "Line Calls & Etiquette",
            icon = Icons.Filled.PanTool,
            subtitle = "Fair play in recreational games",
            rules = listOf(
                Rule(
                    "Making Line Calls",
                    """
                    • Without referees, players call the lines on their own side of the net.
                    • If you're not sure, the ball is in — give your opponent the benefit of the doubt.
                    • Call faults promptly and audibly.
                    """.trimIndent(),
                ),
                Rule(
                    "Good Sportsmanship",
                    """
                    • Call the score clearly before every serve.
                    • Meet at the net after the game for a paddle tap.
                    • Wait for a rally to end before crossing behind a neighbouring court.
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
                    • 20 × 44 ft — Court size (singles and doubles).
                    • 7 ft — Non-volley zone depth from the net.
                    • 36 in / 34 in — Net height at sidelines / center.
                    • 11 — Points to win a standard game (win by 2).
                    • 10 seconds — Time to serve after the score is called.
                    • 2 — Bounces before anyone may volley.
                    """.trimIndent(),
                ),
            ),
        ),
    )
}
