package com.s2aglobal.tournmate.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A top-level category of badminton rules (e.g. "Scoring", "Service Rules").
 * Each section contains one or more individual [Rule] entries.
 */
data class RuleSection(
    val title: String,
    val icon: ImageVector,
    val subtitle: String,
    val rules: List<Rule>,
)

/** A single badminton rule or guideline with a title and explanatory content. */
data class Rule(
    val title: String,
    val content: String,
)

/**
 * Static reference data containing the official BWF (Badminton World Federation)
 * Laws of Badminton, simplified for quick reference by recreational and club players.
 *
 * Source: BWF Laws of Badminton — https://corporate.bwfbadminton.com/statutes/
 */
object BadmintonRules {

    val sections: List<RuleSection> = listOf(

        // ─── 1. THE COURT ────────────────────────────────
        RuleSection(
            title = "The Court",
            icon = Icons.Filled.GridOn,
            subtitle = "Dimensions, lines, and zones",
            rules = listOf(
                Rule(
                    "Court Dimensions",
                    """
                    • Singles court: 13.40 m long × 5.18 m wide (44 ft × 17 ft).
                    • Doubles court: 13.40 m long × 6.10 m wide (44 ft × 20 ft).
                    • The court is divided in half by a net.
                    • All boundary lines are part of the area they define — a shuttle landing on the line is IN.
                    """.trimIndent(),
                ),
                Rule(
                    "Net Height",
                    """
                    • The net is 1.55 m (5 ft 1 in) high at the edges (above the doubles sidelines).
                    • The net is 1.524 m (5 ft) high at the centre.
                    • The net must be 760 mm (30 in) in depth and at least 6.1 m (20 ft) wide.
                    """.trimIndent(),
                ),
                Rule(
                    "Service Courts",
                    """
                    • Each half of the court is divided into a left and right service court by the centre line.
                    • The short service line is 1.98 m (6 ft 6 in) from the net on each side.
                    • In doubles, the long service line is 0.76 m (2 ft 6 in) inside the back boundary.
                    • In singles, the back boundary line IS the long service line.
                    """.trimIndent(),
                ),
                Rule(
                    "Court Zones Overview",
                    """
                    • Front court: the area between the net and the short service line — ideal for net shots and kills.
                    • Mid court: the central area — where most drives and flat exchanges occur.
                    • Rear court: the area behind the mid court to the back boundary — for clears and smashes.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 2. EQUIPMENT ────────────────────────────────
        RuleSection(
            title = "Equipment",
            icon = Icons.Filled.SportsTennis,
            subtitle = "Racket, shuttlecock, and net specifications",
            rules = listOf(
                Rule(
                    "The Shuttlecock",
                    """
                    • A feather shuttlecock has 16 goose feathers fixed into a cork base.
                    • The feathers should be 62–70 mm in length.
                    • Synthetic (nylon) shuttlecocks are allowed for recreational play.
                    • A shuttle weighs between 4.74 g and 5.50 g.
                    • The shuttle should land between 530 mm and 990 mm short of the opposite back boundary when tested with a full underhand stroke from the back boundary line.
                    """.trimIndent(),
                ),
                Rule(
                    "The Racket",
                    """
                    • Overall length must not exceed 680 mm (26.8 in).
                    • Overall width must not exceed 230 mm (9 in).
                    • The stringed area must not exceed 280 mm × 220 mm.
                    • There are no weight restrictions, but most rackets weigh 80–100 g.
                    """.trimIndent(),
                ),
                Rule(
                    "The Net",
                    """
                    • Made of fine dark cord with a mesh of 15–20 mm.
                    • The net is 6.1 m (20 ft) wide.
                    • The top edge is covered by a 75 mm white tape folded over a cord.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 3. SCORING ─────────────────────────────────
        RuleSection(
            title = "Scoring System",
            icon = Icons.Filled.Numbers,
            subtitle = "Points, games, and match format",
            rules = listOf(
                Rule(
                    "Rally Point Scoring",
                    """
                    • A match consists of the best of 3 games.
                    • Each game is played to 21 points.
                    • Every rally results in a point — regardless of who served (rally point system, adopted in 2006).
                    • The side winning a rally adds a point to its score.
                    """.trimIndent(),
                ),
                Rule(
                    "Winning a Game",
                    """
                    • The first side to reach 21 points wins the game.
                    • If the score is tied at 20-all, a side must lead by 2 points to win (e.g. 22-20, 23-21).
                    • If the score reaches 29-all, the side that scores the 30th point wins (no 2-point lead required).
                    """.trimIndent(),
                ),
                Rule(
                    "Interval & Change of Ends",
                    """
                    • When the leading score reaches 11, there is a 60-second interval.
                    • Between games, there is a 120-second interval (2 minutes).
                    • Players change ends after each game.
                    • In the third game, players change ends when the leading score reaches 11.
                    """.trimIndent(),
                ),
                Rule(
                    "Winning the Match",
                    """
                    • The side that wins 2 out of 3 games wins the match.
                    • If a player is unable to continue (injury, disqualification), the opposing side wins the match.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 4. SERVICE RULES ────────────────────────────
        RuleSection(
            title = "Service Rules",
            icon = Icons.Filled.SportsTennis,
            subtitle = "How to serve legally",
            rules = listOf(
                Rule(
                    "Basic Service Rules",
                    """
                    • The serve must be hit diagonally to the opponent's service court.
                    • The server and receiver must stand within their respective service courts.
                    • Neither the server nor receiver may touch the boundary lines until the shuttle is struck.
                    • The shuttle must be hit below 1.15 m (3 ft 9 in) from the floor at the point of contact.
                    • The server's racket shaft must point downward at the moment of contact.
                    """.trimIndent(),
                ),
                Rule(
                    "Service Court — Who Serves Where?",
                    """
                    • At the start of a game and when the server's score is even (0, 2, 4…), the server serves from the RIGHT service court.
                    • When the server's score is odd (1, 3, 5…), the server serves from the LEFT service court.
                    • If the serving side wins the rally, the same server serves again but switches sides.
                    • If the receiving side wins the rally, they become the serving side.
                    """.trimIndent(),
                ),
                Rule(
                    "Doubles Service Sequence",
                    """
                    • At the start, the server on the right serves to the receiver diagonally opposite.
                    • The receiving side does not change positions between rallies.
                    • Only the player standing in the correct service court for the score may receive the serve.
                    • If the serving side wins, the same server serves again from the alternate court.
                    • If the receiving side wins, they gain the serve.
                    • The player who did NOT serve last from that side becomes the new server.
                    """.trimIndent(),
                ),
                Rule(
                    "Common Service Faults",
                    """
                    • Hitting the shuttle above 1.15 m from the floor.
                    • The shuttle is not hit on the server's racket initially.
                    • The server's feet are not in the correct service court.
                    • The serve does not land in the correct diagonal court.
                    • Feinting or balking during the serve.
                    • Undue delay after both sides are ready.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 5. DURING PLAY ─────────────────────────────
        RuleSection(
            title = "During Play",
            icon = Icons.Filled.DirectionsRun,
            subtitle = "Rally rules and in-play regulations",
            rules = listOf(
                Rule(
                    "Winning a Rally",
                    """
                    A rally is won when:
                    • The shuttle lands inside the opponent's court boundaries (on the line = IN).
                    • The opponent hits the shuttle into the net or outside the court.
                    • The opponent commits a fault.
                    """.trimIndent(),
                ),
                Rule(
                    "Net Play",
                    """
                    • The shuttle may touch the net and go over during a rally — this is legal.
                    • A player may NOT touch the net or net posts with their body, racket, or clothing during play.
                    • A player may NOT reach over the net to hit the shuttle (the shuttle must be on your side when you strike it). Follow-through over the net is allowed if the initial contact was on your side.
                    """.trimIndent(),
                ),
                Rule(
                    "Faults During Play",
                    """
                    It is a fault if:
                    • The shuttle lands outside the court boundaries.
                    • The shuttle passes through or under the net.
                    • A player hits the shuttle twice in succession.
                    • The shuttle is caught and held on the racket and then slung.
                    • A player's racket or body touches the net.
                    • A player obstructs or distracts an opponent.
                    • The shuttle hits a player's body or clothing.
                    """.trimIndent(),
                ),
                Rule(
                    "Lets (Replay the Rally)",
                    """
                    A let is called and the rally replayed when:
                    • The shuttle gets caught on top of the net after crossing over.
                    • The server serves before the receiver is ready.
                    • Both server and receiver commit a fault simultaneously.
                    • An unforeseen or accidental situation occurs (e.g. a shuttle from another court enters).
                    • A line judge is unsighted and the umpire cannot make a decision.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 6. DOUBLES SPECIFICS ───────────────────────
        RuleSection(
            title = "Doubles Rules",
            icon = Icons.Filled.Group,
            subtitle = "Positioning, rotation, and strategy",
            rules = listOf(
                Rule(
                    "Court Boundaries in Doubles",
                    """
                    • During the serve: The full width (sidelines) is used, but the BACK service line is shorter (0.76 m inside the back boundary).
                    • During the rally: The full court (all sidelines and the back boundary) is used.
                    • Remember: wide sidelines IN for all of doubles, short back line for service only.
                    """.trimIndent(),
                ),
                Rule(
                    "Receiving Position",
                    """
                    • Only the player in the correct receiving court may return the serve.
                    • If the wrong player returns the serve, it is a fault on the receiving side.
                    • After the serve is returned, either player on a side may hit the shuttle — no rotation is required during the rally.
                    """.trimIndent(),
                ),
                Rule(
                    "Mixed Doubles Strategy",
                    """
                    • In mixed doubles, the female player typically takes the front court and the male player covers the rear court (attack formation).
                    • When defending, both players often stand side-by-side.
                    • Communication is critical — call "mine" or "yours" to avoid confusion.
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 7. COMMON FAULTS SUMMARY ───────────────────
        RuleSection(
            title = "Common Faults",
            icon = Icons.Filled.Warning,
            subtitle = "Quick reference for all fault types",
            rules = listOf(
                Rule(
                    "Service Faults",
                    """
                    • Shuttle struck above 1.15 m.
                    • Racket not pointing downward at contact.
                    • Server's feet on or outside the service court lines.
                    • Missing the shuttle on the serve attempt.
                    • Serve not landing in the correct diagonal service court.
                    """.trimIndent(),
                ),
                Rule(
                    "Rally Faults",
                    """
                    • Shuttle landing out of bounds.
                    • Shuttle going through or under the net.
                    • Shuttle hitting the ceiling or walls (in indoor play — depends on local rules for height).
                    • Player touching the net with body or racket.
                    • Shuttle hitting a player's body.
                    • Double hit by the same player.
                    • Invading opponent's court (reaching over the net).
                    """.trimIndent(),
                ),
                Rule(
                    "Conduct Faults",
                    """
                    • Deliberately distracting an opponent (shouting, gesturing).
                    • Delaying play deliberately.
                    • Modifying or damaging the shuttle to alter flight.
                    • Unsporting behaviour (verbal abuse, racket throwing).
                    • Coaching from off-court during play (in sanctioned tournaments).
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 8. MATCH OFFICIALS ─────────────────────────
        RuleSection(
            title = "Match Officials",
            icon = Icons.Filled.Shield,
            subtitle = "Umpires, service judges, and line judges",
            rules = listOf(
                Rule(
                    "The Umpire",
                    """
                    • The umpire is in charge of the match and the court.
                    • They call the score, manage faults, and enforce rules.
                    • The umpire overrules line judges and service judges when clearly wrong.
                    """.trimIndent(),
                ),
                Rule(
                    "Service Judge",
                    """
                    • Positioned at the side of the court, near the net.
                    • Responsible for calling service faults (height, racket angle, foot position).
                    """.trimIndent(),
                ),
                Rule(
                    "Line Judges",
                    """
                    • Positioned along the court boundaries.
                    • Signal whether the shuttle landed IN or OUT.
                    • Typically 2–4 line judges in professional matches, up to 10 in major events.
                    """.trimIndent(),
                ),
                Rule(
                    "Recreational / Club Play",
                    """
                    • In casual and club matches, players self-officiate.
                    • Call your own lines honestly — if you're unsure, give the benefit to your opponent.
                    • Agree on house rules before the match (e.g. ceiling height, let calls).
                    """.trimIndent(),
                ),
            ),
        ),

        // ─── 9. QUICK REFERENCE ─────────────────────────
        RuleSection(
            title = "Quick Reference",
            icon = Icons.Filled.Bookmark,
            subtitle = "Key numbers every player should know",
            rules = listOf(
                Rule(
                    "Key Numbers",
                    """
                    • 21 — Points per game.
                    • 3 — Maximum games per match (best of 3).
                    • 30 — Absolute maximum score in a game (at 29-all).
                    • 2 — Point lead required to win between 20-all and 29-all.
                    • 11 — Score at which the mid-game interval occurs and ends change in game 3.
                    • 60 — Seconds allowed for the mid-game interval (at 11 points).
                    • 120 — Seconds allowed between games.
                    • 1.15 m — Maximum shuttle height at service contact.
                    • 1.55 m — Net height at edges.
                    • 1.524 m — Net height at centre.
                    • 13.4 m × 6.1 m — Doubles court dimensions.
                    """.trimIndent(),
                ),
                Rule(
                    "Shuttle Speed Test",
                    """
                    • Before a match, players may test the shuttle speed.
                    • Hit the shuttle with a full underhand stroke from the back boundary line.
                    • A correct-speed shuttle should land between 530 mm and 990 mm short of the opposite back boundary.
                    • If the shuttle is too fast or too slow, choose a different speed grade.
                    """.trimIndent(),
                ),
            ),
        ),
    )
}
