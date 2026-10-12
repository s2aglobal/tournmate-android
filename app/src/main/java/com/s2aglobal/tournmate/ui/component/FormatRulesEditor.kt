package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.FormatConfig
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.SeedingMode
import com.s2aglobal.tournmate.domain.model.TieBreaker

private val RulesFill = Color(0xFFF2F2F7)

/**
 * The match-format rules (seeding, points, groups, rounds, max entries) for a
 * tournament. One editor for both Create Tournament and Edit Tournament, so
 * they always offer the same choices. Only options the draw actually uses are
 * shown: seeding is Elo Ranked or Random; bronze match, consolation bracket,
 * allow byes and manual seeding aren't built yet. Mirrors iOS `FormatRulesEditor`.
 */
@Composable
fun FormatRulesEditor(
    format: MatchFormat,
    config: FormatConfig,
    onConfigChange: (FormatConfig) -> Unit,
    accent: Color,
    isCricket: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (format) {
            MatchFormat.SINGLE_ELIMINATION -> SeedingSection(config, onConfigChange, accent)
            MatchFormat.DOUBLE_ELIMINATION -> {
                // Only for tournaments created before double elimination was hidden.
                SeedingSection(config, onConfigChange, accent)
                RulesNote(Icons.Outlined.Info, "Plays as single elimination: one loss and a team is out.", accent)
            }
            MatchFormat.ROUND_ROBIN -> {
                RulesToggleRow(
                    Icons.Default.Repeat, "Double Round Robin", "Each team plays every other team twice",
                    config.doubleRoundRobin, accent,
                ) { onConfigChange(config.copy(doubleRoundRobin = it)) }
                RulesSection(if (isCricket) "POINTS (WIN / TIE OR NO RESULT / LOSS)" else "POINTS SYSTEM") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RulesStepper("Win", config.pointsPerWin, 0..5, Modifier.weight(1f)) { onConfigChange(config.copy(pointsPerWin = it)) }
                        RulesStepper(if (isCricket) "Tie / NR" else "Draw", config.pointsPerDraw, 0..5, Modifier.weight(1f)) { onConfigChange(config.copy(pointsPerDraw = it)) }
                        RulesStepper("Loss", config.pointsPerLoss, 0..5, Modifier.weight(1f)) { onConfigChange(config.copy(pointsPerLoss = it)) }
                    }
                }
                RulesSection("TIE-BREAKER") {
                    TieBreakerChoice(config.tieBreaker, isCricket, accent) { onConfigChange(config.copy(tieBreaker = it)) }
                }
            }
            MatchFormat.GROUP_KNOCKOUT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RulesStepper("Groups", config.groupCount, 2..8, Modifier.weight(1f)) { onConfigChange(config.copy(groupCount = it)) }
                    RulesStepper("Per Group", config.teamsPerGroup, 3..8, Modifier.weight(1f)) { onConfigChange(config.copy(teamsPerGroup = it)) }
                }
                RulesStepper("Advance Per Group", config.advancingPerGroup, 1..4, Modifier.fillMaxWidth()) { onConfigChange(config.copy(advancingPerGroup = it)) }
                RulesSection("GROUP TIE-BREAKER") {
                    TieBreakerChoice(config.groupTieBreaker, isCricket, accent) { onConfigChange(config.copy(groupTieBreaker = it)) }
                }
                RulesNote(Icons.Outlined.Info, "Top finishers from each group advance to a single-elimination knockout stage.", accent)
            }
            MatchFormat.SWISS -> {
                RulesStepper("Number of Rounds", config.swissRounds, 3..10, Modifier.fillMaxWidth()) { onConfigChange(config.copy(swissRounds = it)) }
                RulesNote(Icons.Outlined.Info, "Each round, teams with similar records play each other. No one is eliminated.", accent)
            }
            MatchFormat.MANUAL_DRAW ->
                RulesNote(Icons.Default.Draw, "You assign all matchups after registration closes. Full control over the draw.", accent)
        }
        RulesSection("MAX PARTICIPANTS (OPTIONAL)") {
            MaxParticipantsField(config.maxParticipants, accent) { onConfigChange(config.copy(maxParticipants = it)) }
        }
    }
}

// ── Sections ─────────────────────────────────────────

@Composable
private fun SeedingSection(config: FormatConfig, onConfigChange: (FormatConfig) -> Unit, accent: Color) {
    val current = SeedingMode.fromRawValue(config.seedingMode).effective
    RulesSection("SEEDING") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeedingMode.offered.forEach { mode ->
                RulesChoice(mode.displayName, current == mode, accent, Modifier.weight(1f)) {
                    onConfigChange(config.copy(seedingMode = mode.rawValue))
                }
            }
        }
        Text(
            if (current == SeedingMode.RANDOM) "The bracket, including any byes, is drawn at random."
            else "Highest-rated teams are kept apart until the late rounds and get any byes.",
            fontSize = 11.sp, color = Color.Gray,
        )
    }
}

@Composable
private fun TieBreakerChoice(selectedRaw: String, isCricket: Boolean, accent: Color, onSelect: (String) -> Unit) {
    if (isCricket) {
        // Cricket always ranks level teams by net run rate (then head-to-head).
        Row(
            Modifier.fillMaxWidth().background(RulesFill, RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = accent)
            Text("Net run rate, then head-to-head", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        val current = TieBreaker.fromRawValue(selectedRaw)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TieBreaker.entries.forEach { tb ->
                RulesChoice(tieBreakerName(tb), current == tb, accent, Modifier.weight(1f), fontSize = 11) { onSelect(tb.rawValue) }
            }
        }
    }
}

private fun tieBreakerName(tb: TieBreaker) = when (tb) {
    TieBreaker.HEAD_TO_HEAD -> "Head-to-Head"
    TieBreaker.POINT_DIFF -> "Point Differential"
    TieBreaker.GAMES_WON -> "Games Won"
}

// ── Building blocks ──────────────────────────────────

@Composable
private fun RulesSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
        content()
    }
}

@Composable
private fun RulesChoice(
    title: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier,
    fontSize: Int = 13, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.15f) else RulesFill,
    ) {
        Text(
            title, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1,
            color = if (selected) accent else Color.Black,
            modifier = Modifier
                .then(if (selected) Modifier.border(1.dp, accent, shape) else Modifier)
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun RulesNote(icon: ImageVector, text: String, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 1.dp), tint = accent)
        Text(text, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
    }
}

@Composable
private fun RulesToggleRow(
    icon: ImageVector, title: String, subtitle: String, isOn: Boolean, accent: Color,
    onToggle: (Boolean) -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = RulesFill, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Switch(checked = isOn, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = accent))
        }
    }
}

@Composable
private fun RulesStepper(label: String, value: Int, range: IntRange, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            modifier = Modifier.background(RulesFill, RoundedCornerShape(10.dp)),
        ) {
            IconButton(
                onClick = { if (value > range.first) onChange(value - 1) },
                modifier = Modifier.size(36.dp).semantics { contentDescription = "Decrease $label" },
            ) { Icon(Icons.Default.Remove, null, Modifier.size(14.dp)) }
            Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 30.dp), textAlign = TextAlign.Center)
            IconButton(
                onClick = { if (value < range.last) onChange(value + 1) },
                modifier = Modifier.size(36.dp).semantics { contentDescription = "Increase $label" },
            ) { Icon(Icons.Default.Add, null, Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun MaxParticipantsField(value: Int?, accent: Color, onChange: (Int?) -> Unit) {
    val text = value?.toString() ?: ""
    val style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    BasicTextField(
        value = text,
        onValueChange = { raw -> onChange(raw.filter { it.isDigit() }.take(6).toIntOrNull()) },
        singleLine = true,
        textStyle = style,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(accent),
        modifier = Modifier.fillMaxWidth().background(RulesFill, RoundedCornerShape(14.dp)),
        decorationBox = { inner ->
            Box(Modifier.padding(16.dp)) {
                if (text.isEmpty()) Text("Unlimited", style = style.copy(color = Color.Gray.copy(alpha = 0.6f)))
                inner()
            }
        },
    )
}
