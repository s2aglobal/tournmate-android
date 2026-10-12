package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.CricketEventType
import com.s2aglobal.tournmate.domain.model.CricketRules

private val CricketFill = Color(0xFFF2F2F7)

/**
 * Grid of cricket event types (T20, One Day, T10, box cricket). Picking one
 * resets the rules to its preset. Mirrors iOS `CricketEventTypePicker`.
 */
@Composable
fun CricketEventTypePicker(rules: CricketRules, onRulesChange: (CricketRules) -> Unit, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CricketEventType.presets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    val preset = type.defaultRules
                    val selected = rules.eventType == type
                    Surface(
                        onClick = { onRulesChange(preset) },
                        modifier = Modifier.weight(1f).semantics { this.selected = selected },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accent.copy(alpha = 0.12f) else CricketFill,
                    ) {
                        Column(
                            Modifier
                                .then(if (selected) Modifier.border(1.5.dp, accent, RoundedCornerShape(12.dp)) else Modifier)
                                .heightIn(min = 70.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                type.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (selected) accent else Color.Black, maxLines = 2,
                            )
                            Text(
                                "${preset.overs} overs · ${preset.playersPerSide}-a-side",
                                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (rules.eventType == CricketEventType.CUSTOM) {
            Text("Custom rules · ${rules.summary}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    }
}

/**
 * Overs, players per side and the two match options. Any change away from the
 * selected preset's values marks the rules as Custom. Mirrors iOS `CricketRulesEditor`.
 */
@Composable
fun CricketRulesEditor(rules: CricketRules, onRulesChange: (CricketRules) -> Unit, accent: Color) {
    /** Applies a change, then names the event type after the preset the values match (super over is free), or Custom. */
    fun apply(change: (CricketRules) -> CricketRules) {
        val next = change(rules)
        val type = CricketEventType.presets.firstOrNull { t ->
            val p = t.defaultRules
            p.overs == next.overs && p.playersPerSide == next.playersPerSide &&
                p.ballsPerOver == next.ballsPerOver && p.lastManStands == next.lastManStands
        } ?: CricketEventType.CUSTOM
        onRulesChange(next.copy(eventType = type))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().background(CricketFill, RoundedCornerShape(14.dp))) {
            StepperRow("Overs per innings", rules.overs, CricketRules.oversRange, accent) { v -> apply { it.copy(overs = v) } }
            HorizontalDivider(Modifier.padding(start = 16.dp), color = Color(0xFFE5E5EA))
            StepperRow("Players per side", rules.playersPerSide, CricketRules.playersRange, accent) { v -> apply { it.copy(playersPerSide = v) } }
            HorizontalDivider(Modifier.padding(start = 16.dp), color = Color(0xFFE5E5EA))
            SwitchRow(
                "Last man stands",
                "The last batter can bat alone, so all ${rules.playersPerSide} can be out.",
                rules.lastManStands, accent,
            ) { v -> apply { it.copy(lastManStands = v) } }
            HorizontalDivider(Modifier.padding(start = 16.dp), color = Color(0xFFE5E5EA))
            SwitchRow("Super over for ties", "A tied match is decided by one over each.", rules.superOver, accent) { v ->
                apply { it.copy(superOver = v) }
            }
        }
        Text(
            "Results are entered as a scorecard: runs, wickets and overs for each innings. " +
                "Standings rank by points (win 2, tie or no result 1), then net run rate.",
            fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun StepperRow(title: String, value: Int, range: IntRange, accent: Color, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(range.first)) }, enabled = value > range.first) {
            Icon(Icons.Default.RemoveCircle, "Fewer ${title.lowercase()}", Modifier.size(26.dp), tint = if (value > range.first) accent else Color.LightGray)
        }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 34.dp))
        IconButton(onClick = { onChange((value + 1).coerceAtMost(range.last)) }, enabled = value < range.last) {
            Icon(Icons.Default.AddCircle, "More ${title.lowercase()}", Modifier.size(26.dp), tint = if (value < range.last) accent else Color.LightGray)
        }
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detail, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = accent))
    }
}
