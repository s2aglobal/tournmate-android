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

/** Live sports shown together on the intro screens to say "multi-sport" (iOS `introShowcaseSports`). */
val IntroShowcaseSports: List<SportType> = listOf(SportType.PICKLEBALL, SportType.BADMINTON, SportType.TENNIS)

/** Pickleball, badminton and tennis artwork side by side (iOS `MultiSportArtworkRow`). */
@Composable
fun MultiSportArtworkRow(size: Dp = 44.dp, spacing: Dp = 12.dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Pickleball, badminton, tennis and more"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        IntroShowcaseSports.forEach { sport ->
            SportArtworkImage(
                sport = sport,
                size = size,
                modifier = Modifier.shadow(4.dp, SportArtworkShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f)),
            )
        }
    }
}
