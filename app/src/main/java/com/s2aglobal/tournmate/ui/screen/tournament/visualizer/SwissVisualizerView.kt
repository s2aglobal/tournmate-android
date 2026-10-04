package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent

@Composable
fun SwissVisualizerView(
    state: TournamentVisualState,
    onMatchTap: ((MatchNode) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = AppAccent.copy(alpha = 0.05f),
        ) {
            Text(
                "Paired by similar record — no elimination",
                fontSize = 12.sp, color = Color.Gray,
                modifier = Modifier.padding(14.dp),
            )
        }

        state.rounds.forEach { round ->
            RoundHeader(round.roundName)
            round.matches.forEach { matchNode ->
                MatchCard(matchNode, onTap = { onMatchTap?.invoke(matchNode) })
            }
        }
    }
}
