package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.CricketCareer
import com.s2aglobal.tournmate.ui.theme.AppAccent

/**
 * A player's cricket batting and bowling totals on their profile, from the
 * figures entered with match results. Mirrors iOS `CricketCareerCard`.
 */
@Composable
fun CricketCareerCard(career: CricketCareer, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.SportsCricket, null, Modifier.size(16.dp), tint = AppAccent)
            Text("Cricket Stats", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppAccent)
        }
        val b = career.batting
        val w = career.bowling
        if (b == null && w == null) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color.White, shadowElevation = 2.dp) {
                Text(
                    "Batting and bowling figures appear here once a scorer enters them with a result.",
                    fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(14.dp),
                )
            }
        } else {
            if (b != null) {
                CareerLine("BATTING", listOf("Inns" to "${b.innings}", "Runs" to "${b.runs}", "HS" to b.highestText, "Avg" to b.average, "SR" to b.strikeRate))
            }
            if (w != null) {
                CareerLine("BOWLING", listOf("Inns" to "${w.innings}", "Wkts" to "${w.wickets}", "Best" to w.bestText, "Econ" to w.economy))
            }
        }
    }
}

@Composable
private fun CareerLine(title: String, items: List<Pair<String, String>>) {
    Surface(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "$title: " + items.joinToString(", ") { (label, value) -> "$label $value" }
        },
        RoundedCornerShape(14.dp), Color.White, shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
            Row(Modifier.fillMaxWidth()) {
                items.forEach { (label, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(label, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
