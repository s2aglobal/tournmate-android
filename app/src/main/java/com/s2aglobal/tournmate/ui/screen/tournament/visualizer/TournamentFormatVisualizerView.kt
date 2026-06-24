package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.runtime.Composable
import com.s2aglobal.tournmate.domain.model.MatchFormat

@Composable
fun TournamentFormatVisualizerView(
    state: TournamentVisualState,
    onMatchTap: ((MatchNode) -> Unit)? = null,
) {
    when (state.formatType) {
        MatchFormat.SINGLE_ELIMINATION -> SingleEliminationVisualizerView(state, onMatchTap)
        MatchFormat.DOUBLE_ELIMINATION -> SingleEliminationVisualizerView(state, onMatchTap)
        MatchFormat.ROUND_ROBIN -> RoundRobinVisualizerView(state, onMatchTap)
        MatchFormat.GROUP_KNOCKOUT -> GroupKnockoutVisualizerView(state, onMatchTap)
        MatchFormat.SWISS -> SwissVisualizerView(state, onMatchTap)
        MatchFormat.MANUAL_DRAW -> ManualDrawVisualizerView(state, onMatchTap)
    }
}
