package com.s2aglobal.tournmate.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.PickleBall
import com.s2aglobal.tournmate.ui.theme.PickleBallShade
import com.s2aglobal.tournmate.ui.theme.TennisBall
import com.s2aglobal.tournmate.ui.theme.theme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Drawn sport artwork (iOS SportArtwork.swift): vector only, scales from badges to hero banners.

// ── Pickleball ──────────────────────────────────────

/** One ring of holes. Outer rings are squashed radially so the ball reads as a sphere. */
private class HoleRing(val count: Int, val distance: Float, val radius: Float, val squash: Float, val offsetDegrees: Float)

private val HoleRings = listOf(
    HoleRing(1, 0f, 0.12f, 1.0f, 0f),
    HoleRing(6, 0.40f, 0.105f, 0.85f, 0f),
    HoleRing(12, 0.76f, 0.08f, 0.55f, 15f),
)

private val PickleHighlight = Color(0.96f, 1.0f, 0.62f)

/** An optic-yellow pickleball with its signature holes. */
@Composable
fun PickleballBall(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawPickleball() }
}

private fun DrawScope.drawPickleball() {
    val r = min(size.width, size.height) / 2
    val c = center
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PickleHighlight, PickleBall, PickleBallShade),
            center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
            radius = r * 1.6f,
        ),
        radius = r,
        center = c,
    )

    val holeFill = PickleBallShade.copy(alpha = 0.9f)
    val holeEdge = Color.Black.copy(alpha = 0.12f)
    val edgeWidth = maxOf(0.5f, r * 0.015f)
    for (ring in HoleRings) {
        for (i in 0 until ring.count) {
            val degrees = ring.offsetDegrees + i * 360f / ring.count
            val angle = Math.toRadians(degrees.toDouble())
            val dx = (cos(angle) * ring.distance * r).toFloat()
            val dy = (sin(angle) * ring.distance * r).toFloat()
            val h = ring.radius * r
            val w = h * ring.squash
            withTransform({
                translate(c.x + dx, c.y + dy)
                rotate(degrees, pivot = Offset.Zero)
            }) {
                drawOval(holeFill, topLeft = Offset(-w, -h), size = Size(w * 2, h * 2))
                drawOval(holeEdge, topLeft = Offset(-w, -h), size = Size(w * 2, h * 2), style = Stroke(edgeWidth))
            }
        }
    }

    drawCircle(Color.Black.copy(alpha = 0.10f), radius = r, center = c, style = Stroke(maxOf(0.5f, r * 0.02f)))
}

// ── Tennis ball ─────────────────────────────────────

/** A yellow tennis ball with its curved seam. */
@Composable
fun TennisBallArt(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = min(this.size.width, this.size.height) / 2
        val c = center
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0.97f, 1.0f, 0.6f), TennisBall, Color(0.62f, 0.70f, 0.08f)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
                radius = r * 1.6f,
            ),
            radius = r,
            center = c,
        )
        val seam = Path().apply {
            moveTo(c.x - r * 0.95f, c.y - r * 0.3f)
            quadraticBezierTo(c.x - r * 0.05f, c.y - r * 0.05f, c.x - r * 0.3f, c.y + r * 0.95f)
            moveTo(c.x + r * 0.95f, c.y + r * 0.3f)
            quadraticBezierTo(c.x + r * 0.05f, c.y + r * 0.05f, c.x + r * 0.3f, c.y - r * 0.95f)
        }
        val ball = Path().apply { addOval(Rect(c, r)) }
        clipPath(ball) {
            drawPath(seam, Color.White.copy(alpha = 0.95f), style = Stroke(width = r * 0.09f, cap = StrokeCap.Round))
        }
    }
}

// ── Sport icon / badge ──────────────────────────────

/** Monochrome (tintable) icon for a sport, iOS `SportType.icon`. */
@Composable
fun sportIconPainter(sport: SportType, @DrawableRes badmintonIcon: Int = R.drawable.ic_figure_badminton): Painter =
    when (sport) {
        SportType.BADMINTON -> painterResource(badmintonIcon)
        SportType.PICKLEBALL -> painterResource(R.drawable.ic_pickleball)
        SportType.TENNIS -> painterResource(R.drawable.ic_tennisball)
        SportType.TABLE_TENNIS -> rememberVectorPainter(Icons.Filled.SportsTennis)
        SportType.VOLLEYBALL -> rememberVectorPainter(Icons.Filled.SportsVolleyball)
        SportType.BASKETBALL -> rememberVectorPainter(Icons.Filled.SportsBasketball)
        SportType.FOOTBALL -> rememberVectorPainter(Icons.Filled.SportsFootball)
        SportType.SOCCER -> rememberVectorPainter(Icons.Filled.SportsSoccer)
        SportType.CRICKET -> rememberVectorPainter(Icons.Filled.SportsCricket)
        SportType.PADEL -> rememberVectorPainter(Icons.Filled.SportsTennis)
        SportType.SQUASH -> rememberVectorPainter(Icons.Filled.SportsTennis)
        SportType.BEACH_VOLLEYBALL -> rememberVectorPainter(Icons.Filled.SportsVolleyball)
        SportType.GOLF -> rememberVectorPainter(Icons.Filled.SportsGolf)
        SportType.BOWLING -> rememberVectorPainter(Icons.Filled.Sports)
        SportType.DARTS -> rememberVectorPainter(Icons.Filled.Adjust)
        SportType.GENERIC -> rememberVectorPainter(Icons.Filled.Sports)
    }

/** The sport's "ball": drawn ball where we have one, otherwise the sport icon on a themed disc. */
@Composable
fun SportBadge(sport: SportType, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    when (sport) {
        SportType.PICKLEBALL -> PickleballBall(size, modifier)
        SportType.TENNIS -> TennisBallArt(size, modifier)
        SportType.BOWLING -> BowlingBallArt(size, modifier)
        SportType.SQUASH -> SquashBallArt(size, modifier)
        else -> Box(
            modifier.size(size).background(sport.theme.gradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sportIconPainter(sport), null, Modifier.size(size * 0.5f), tint = Color.White)
        }
    }
}

// ── Bowling / squash balls ──────────────────────────

/** A glossy bowling ball with three finger holes. */
@Composable
fun BowlingBallArt(size: Dp, modifier: Modifier = Modifier) {
    val base = Color(0xFF3949AB)
    Canvas(modifier.size(size)) {
        val r = min(this.size.width, this.size.height) / 2
        val c = center
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF8C9EFF), base, Color(0xFF1A237E)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
                radius = r * 1.6f,
            ),
            radius = r,
            center = c,
        )
        val hole = Color(0xFF0D1440)
        drawCircle(hole, radius = r * 0.13f, center = Offset(c.x + r * 0.05f, c.y - r * 0.38f))
        drawCircle(hole, radius = r * 0.13f, center = Offset(c.x + r * 0.38f, c.y - r * 0.22f))
        drawCircle(hole, radius = r * 0.15f, center = Offset(c.x + r * 0.22f, c.y + r * 0.18f))
    }
}

/** A black squash ball with its two yellow dots. */
@Composable
fun SquashBallArt(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = min(this.size.width, this.size.height) / 2
        val c = center
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF6B6B6B), Color(0xFF2B2B2B), Color(0xFF0F0F0F)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
                radius = r * 1.6f,
            ),
            radius = r,
            center = c,
        )
        val dot = Color(0xFFFFD60A)
        drawCircle(dot, radius = r * 0.14f, center = Offset(c.x - r * 0.22f, c.y - r * 0.05f))
        drawCircle(dot, radius = r * 0.14f, center = Offset(c.x + r * 0.22f, c.y - r * 0.05f))
    }
}

/**
 * Artwork for a picker tile: the drawn ball where one exists (pickleball, tennis, bowling, squash),
 * otherwise the sport icon inside a soft circle. [onColor] when drawn on the
 * sport's own gradient (selected tile), so the soft circle doesn't vanish into the fill.
 */
@Composable
fun SportTileArt(sport: SportType, size: Dp, modifier: Modifier = Modifier, onColor: Boolean = false) {
    when (sport) {
        // Same as iOS: drawn balls for pickleball and tennis, icon-in-circle for the rest.
        SportType.PICKLEBALL, SportType.TENNIS ->
            SportBadge(sport, size, modifier)
        else -> {
            val tint = if (onColor) Color.White else sport.theme.primary
            Box(
                modifier.size(size).background(if (onColor) Color.White.copy(alpha = 0.22f) else tint.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sportIconPainter(sport), null, Modifier.size(size * 0.58f), tint = tint)
            }
        }
    }
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
