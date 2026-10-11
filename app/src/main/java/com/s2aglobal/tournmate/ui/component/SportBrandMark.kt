package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import com.s2aglobal.tournmate.ui.theme.theme

/**
 * Brand tile on the splash and Welcome screens (iOS `SportBrandMark`): the saved sport's glyph on
 * that sport's theme colour. With no saved sport it shows the sport-neutral TournMate trophy (as in
 * the launcher icon) on brand purple.
 */
@Composable
fun SportBrandMark(
    size: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    shadowElevation: Dp,
    modifier: Modifier = Modifier,
    sport: SportType? = CurrentSport.storedSport,
) {
    val fill = sport?.theme?.primary ?: TournmatePurple
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .size(size)
            .shadow(shadowElevation, shape, ambientColor = fill.copy(alpha = 0.4f), spotColor = fill.copy(alpha = 0.4f)),
        shape = shape,
        color = fill,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                // No saved sport → trophy (iOS "trophy.fill").
                painter = if (sport != null) sportIconPainter(sport) else rememberVectorPainter(Icons.Filled.EmojiEvents),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = Color.White,
            )
        }
    }
}

/** Shown on the intro screens before the sport catalog has any live sport. */
private val IntroFallbackSports: List<SportType> = listOf(SportType.PICKLEBALL, SportType.BADMINTON, SportType.TENNIS)

/**
 * Sports shown on the intro screens (iOS `introShowcaseSports`): every sport users can pick right
 * now (from `config/sports`, so cricket appears once it's live for this app version), or the
 * original three when the catalog has none.
 */
@Composable
fun introShowcaseSports(): List<SportType> =
    LocalSportCatalog.current.liveSports.ifEmpty { IntroFallbackSports }

/** Row of live-sport artwork for the intro screens (iOS `MultiSportArtworkRow`). */
@Composable
fun MultiSportArtworkRow(size: Dp = 44.dp, spacing: Dp = 12.dp, modifier: Modifier = Modifier) {
    val sports = introShowcaseSports()
    val label = remember(sports) { joinedNames(sports.map { it.displayName }) }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
        },
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        sports.forEach { sport ->
            SportArtworkImage(
                sport = sport,
                size = size,
                modifier = Modifier.shadow(4.dp, SportArtworkShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f)),
            )
        }
    }
}

/** "Pickleball, Badminton and Tennis" (iOS `ListFormatter`). */
private fun joinedNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> names.dropLast(1).joinToString(", ") + ", and " + names.last()
}
