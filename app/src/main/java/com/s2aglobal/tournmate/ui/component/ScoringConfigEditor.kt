package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.ScoringConfig
import com.s2aglobal.tournmate.domain.model.ScoringSystem
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.scoringRules
import com.s2aglobal.tournmate.ui.theme.AppAccent

private val OptionFill = Color(0xFFF2F2F7)

/** "Game" / "Set" — the sport's word for one scoring unit. */
val SportType.scoringUnit: String
    get() = setNameSingular.replaceFirstChar { it.uppercase() }

/**
 * Organizer controls for a tournament's scoring: games per match, points to win,
 * and scoring system. Options come from the sport's `scoringRules`.
 * [singleGameOnly] hides the best-of choice (round robin plays one game per match).
 */
@Composable
fun ScoringConfigEditor(
    sport: SportType,
    config: ScoringConfig,
    onConfigChange: (ScoringConfig) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AppAccent,
    singleGameOnly: Boolean = false,
) {
    val rules = sport.scoringRules
    val unit = sport.scoringUnit
    val effectiveConfig = if (singleGameOnly) config.copy(gamesPerMatch = 1) else config

    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (singleGameOnly) {
            EditorSection("${unit.uppercase()}S PER MATCH") {
                Text("Round-robin matches are a single ${unit.lowercase()}.", fontSize = 13.sp, color = Color.Gray)
            }
        } else if (rules.gamesPerMatchOptions.size > 1) {
            EditorSection("${unit.uppercase()}S PER MATCH") {
                OptionRow(
                    values = rules.gamesPerMatchOptions,
                    selected = config.gamesPerMatch,
                    accent = accent,
                    label = { if (it == 1) "1 $unit" else "Best of $it" },
                ) { games ->
                    onConfigChange(rules.config(games, config.pointsToWin, config.scoringSystem))
                }
            }
        }

        if (rules.pointsToWinOptions.size > 1) {
            EditorSection("POINTS TO WIN A ${unit.uppercase()}") {
                OptionRow(
                    values = rules.pointsToWinOptions,
                    selected = config.pointsToWin,
                    accent = accent,
                    label = { "$it" },
                ) { points ->
                    onConfigChange(rules.config(config.gamesPerMatch, points, config.scoringSystem))
                }
            }
        }

        if (rules.scoringSystemOptions.size > 1) {
            EditorSection("SCORING SYSTEM") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rules.scoringSystemOptions.forEach { system ->
                        ScoringSystemRow(system, config.scoringSystem == system, accent) {
                            onConfigChange(config.copy(scoringSystem = system))
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = accent)
            Text(ScoringDescription.detail(effectiveConfig, sport), fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Color.Gray)
        content()
    }
}

@Composable
private fun OptionRow(
    values: List<Int>,
    selected: Int,
    accent: Color,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        values.forEach { value ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accent else OptionFill,
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label(value), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoringSystemRow(system: ScoringSystem, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (isSelected) accent else Color.Transparent, shape),
        shape = shape,
        color = if (isSelected) accent.copy(alpha = 0.08f) else OptionFill,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                null, Modifier.size(20.dp),
                tint = if (isSelected) accent else Color(0xFFC7C7CC),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(system.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Text(system.tagline, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

/** Plain-language descriptions of a scoring config. */
object ScoringDescription {
    /** One-line summary, e.g. "Best of 3 games · to 11, win by 2 · Side-out". */
    fun summary(config: ScoringConfig, sport: SportType): String {
        val unitPlural = sport.setName
        val unit = sport.setNameSingular
        val parts = mutableListOf<String>()
        parts += if (config.gamesPerMatch == 1) "1 $unit" else "Best of ${config.gamesPerMatch} $unitPlural"
        if (config.pointsToWin > 0) {
            val margin = if (config.winBy > 1) ", win by ${config.winBy}" else ""
            parts += "to ${config.pointsToWin}$margin"
        }
        if (sport.scoringRules.scoringSystemOptions.size > 1) {
            parts += if (config.scoringSystem == ScoringSystem.SIDE_OUT) "Side-out" else "Rally"
        }
        return parts.joinToString(" · ")
    }

    /** Full sentence explaining how a match is won. */
    fun detail(config: ScoringConfig, sport: SportType): String {
        val unit = sport.setNameSingular
        val match = if (config.gamesPerMatch == 1) {
            "Matches are a single $unit."
        } else {
            "First to win ${config.gamesToWin} of ${config.gamesPerMatch} ${sport.setName} takes the match."
        }
        if (config.pointsToWin <= 0) return "$match Enter the final score; the higher score wins."
        val pointWord = if (sport == SportType.TENNIS) "games" else "points"
        var game = "Each $unit is played to ${config.pointsToWin} $pointWord"
        if (config.winBy > 1) game += ", win by ${config.winBy}"
        config.pointCap?.let { cap ->
            game += if (sport == SportType.TENNIS) " (tiebreak at ${cap - 1}-${cap - 1})" else ", capped at $cap"
        }
        game += "."
        val system = if (sport.scoringRules.scoringSystemOptions.size > 1) {
            if (config.scoringSystem == ScoringSystem.SIDE_OUT) " Only the serving side scores." else " Every rally scores a point."
        } else ""
        return "$match $game$system"
    }
}
