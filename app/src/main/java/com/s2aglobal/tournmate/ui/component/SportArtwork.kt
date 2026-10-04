package com.s2aglobal.tournmate.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.theme

// ── Sport artwork (full-colour icon pack) ───────────

/** Full-colour ball artwork from the icon pack (iOS `SportType.artworkName`). Null for legacy ids. */
@get:DrawableRes
val SportType.artworkRes: Int?
    get() = when (this) {
        SportType.PICKLEBALL -> R.drawable.ic_sport_pickleball
        SportType.BADMINTON -> R.drawable.ic_sport_badminton
        SportType.TENNIS -> R.drawable.ic_sport_tennis
        SportType.PADEL -> R.drawable.ic_sport_padel
        SportType.TABLE_TENNIS -> R.drawable.ic_sport_table_tennis
        SportType.SQUASH -> R.drawable.ic_sport_squash
        SportType.VOLLEYBALL -> R.drawable.ic_sport_volleyball
        SportType.BEACH_VOLLEYBALL -> R.drawable.ic_sport_beach_volleyball
        SportType.BASKETBALL -> R.drawable.ic_sport_basketball
        SportType.SOCCER -> R.drawable.ic_sport_soccer
        SportType.CRICKET -> R.drawable.ic_sport_cricket
        SportType.ROUNDNET -> R.drawable.ic_sport_roundnet
        SportType.GOLF -> R.drawable.ic_sport_golf
        SportType.DISC_GOLF -> R.drawable.ic_sport_disc_golf
        SportType.BOWLING -> R.drawable.ic_sport_bowling
        SportType.DARTS -> R.drawable.ic_sport_darts
        SportType.FOOTBALL, SportType.GENERIC -> null
    }

/**
 * The sport's full-colour ball artwork, untinted (iOS `SportArtworkImage`). Sports without an icon
 * (football, generic) get a plain ball in the pack style: theme-coloured disc with a white highlight.
 */
@Composable
fun SportArtworkImage(sport: SportType, size: Dp, modifier: Modifier = Modifier) {
    val res = sport.artworkRes
    if (res != null) {
        Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
    } else {
        val fill = sport.theme.primary
        // Same geometry as the 48×48 pack: ball r=22 at the centre, highlight r=7 at (17, 15).
        Canvas(modifier.size(size)) {
            val u = this.size.minDimension / 48f
            val ball = Path().apply { addOval(Rect(center, 22f * u)) }
            drawPath(ball, fill)
            clipPath(ball) {
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 7f * u, center = Offset(17f * u, 15f * u))
            }
        }
    }
}

/** The ball's outline inside [SportArtworkImage] (r = 22 of 48), for shadows that hug the ball. */
val SportArtworkShape = GenericShape { size, _ ->
    addOval(Rect(Offset(size.width / 2, size.height / 2), size.minDimension * 22f / 48f))
}

// ── Monochrome sport glyph ──────────────────────────

/** Monochrome (tintable) icon for a sport, iOS `SportType.icon`. Tab bar and inline tinted icons only. */
@Composable
fun sportIconPainter(sport: SportType, @DrawableRes badmintonIcon: Int = R.drawable.ic_figure_badminton): Painter =
    when (sport) {
        SportType.BADMINTON -> painterResource(badmintonIcon)
        SportType.PICKLEBALL -> painterResource(R.drawable.ic_pickleball)
        SportType.TENNIS -> painterResource(R.drawable.ic_tennisball)
        SportType.TABLE_TENNIS -> painterResource(R.drawable.ic_figure_table_tennis)
        SportType.VOLLEYBALL, SportType.ROUNDNET -> painterResource(R.drawable.ic_volleyball_fill)
        SportType.BASKETBALL -> painterResource(R.drawable.ic_basketball_fill)
        SportType.FOOTBALL -> rememberVectorPainter(Icons.Filled.SportsFootball)
        SportType.SOCCER -> painterResource(R.drawable.ic_soccerball)
        SportType.CRICKET -> painterResource(R.drawable.ic_cricketball_fill)
        SportType.PADEL -> painterResource(R.drawable.ic_figure_padel)
        SportType.SQUASH -> painterResource(R.drawable.ic_figure_squash)
        SportType.BEACH_VOLLEYBALL -> painterResource(R.drawable.ic_figure_volleyball)
        SportType.GOLF -> painterResource(R.drawable.ic_figure_golf)
        SportType.BOWLING -> painterResource(R.drawable.ic_figure_bowling)
        SportType.DARTS, SportType.DISC_GOLF -> painterResource(R.drawable.ic_target)
        SportType.GENERIC -> rememberVectorPainter(Icons.Filled.Sports)
    }

// ── Court lines ─────────────────────────────────────

/**
 * Top-down court diagram drawn to each sport's real proportions.
 * Length runs horizontally with the net in the middle.
 */
@Composable
fun CourtLines(
    sport: SportType,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
    lineWidth: Dp = 2.dp,
    /** Fill for no-volley "kitchen" style zones (pickleball only). */
    zoneColor: Color? = null,
) {
    val spec = CourtSpec.forSport(sport)
    Canvas(modifier) {
        val lw = lineWidth.toPx()
        // Fit the court's aspect ratio inside the available rect.
        var w = size.width
        var h = w / spec.aspect
        if (h > size.height) { h = size.height; w = h * spec.aspect }
        val ox = (size.width - w) / 2
        val oy = (size.height - h) / 2
        fun pt(p: Offset) = Offset(ox + p.x * w, oy + p.y * h)

        if (zoneColor != null) {
            for (zone in spec.zones) {
                drawRect(zoneColor, topLeft = Offset(ox + zone.left * w, oy + zone.top * h), size = Size(zone.width * w, zone.height * h))
            }
        }
        drawRect(lineColor, topLeft = Offset(ox, oy), size = Size(w, h), style = Stroke(lw))
        for ((a, b) in spec.lines) drawLine(lineColor, pt(a), pt(b), strokeWidth = lw)
        drawLine(
            lineColor, pt(Offset(0.5f, -0.04f)), pt(Offset(0.5f, 1.04f)),
            strokeWidth = lw * 1.6f, cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(lw * 2, lw * 3f)),
        )
    }
}

/** Court markings in unit coordinates: x along the length (0…1), y across the width (0…1). */
class CourtSpec(val aspect: Float, val lines: List<Pair<Offset, Offset>>, val zones: List<Rect>) {
    companion object {
        private fun line(x1: Float, y1: Float, x2: Float, y2: Float) = Offset(x1, y1) to Offset(x2, y2)

        // 44 × 20 ft; non-volley zone (kitchen) 7 ft each side of the net.
        private val Pickleball: CourtSpec = run {
            val k1 = 15f / 44f
            val k2 = 29f / 44f
            CourtSpec(
                aspect = 44f / 20f,
                lines = listOf(line(k1, 0f, k1, 1f), line(k2, 0f, k2, 1f), line(0f, 0.5f, k1, 0.5f), line(k2, 0.5f, 1f, 0.5f)),
                zones = listOf(Rect(k1, 0f, k2, 1f)),
            )
        }

        // 13.4 × 6.1 m; short service 1.98 m from net, doubles long service 0.76 m from back.
        private val Badminton: CourtSpec = run {
            val s1 = 4.72f / 13.4f
            val s2 = 1 - s1
            val l1 = 0.76f / 13.4f
            val l2 = 1 - l1
            val side1 = 0.46f / 6.1f
            val side2 = 1 - side1
            CourtSpec(
                aspect = 13.4f / 6.1f,
                lines = listOf(
                    line(s1, 0f, s1, 1f), line(s2, 0f, s2, 1f),
                    line(l1, 0f, l1, 1f), line(l2, 0f, l2, 1f),
                    line(0f, side1, 1f, side1), line(0f, side2, 1f, side2),
                    line(0f, 0.5f, s1, 0.5f), line(s2, 0.5f, 1f, 0.5f),
                ),
                zones = emptyList(),
            )
        }

        // 23.77 × 10.97 m; singles sidelines 1.37 m in, service line 6.40 m from net.
        private val Tennis: CourtSpec = run {
            val side1 = 1.37f / 10.97f
            val side2 = 1 - side1
            val sv1 = 5.485f / 23.77f
            val sv2 = 1 - sv1
            CourtSpec(
                aspect = 23.77f / 10.97f,
                lines = listOf(
                    line(0f, side1, 1f, side1), line(0f, side2, 1f, side2),
                    line(sv1, side1, sv1, side2), line(sv2, side1, sv2, side2),
                    line(sv1, 0.5f, sv2, 0.5f),
                ),
                zones = emptyList(),
            )
        }

        private val Empty = CourtSpec(2f, emptyList(), emptyList())

        fun forSport(sport: SportType): CourtSpec = when (sport) {
            SportType.PICKLEBALL -> Pickleball
            SportType.BADMINTON -> Badminton
            SportType.TENNIS -> Tennis
            else -> Empty
        }
    }
}
