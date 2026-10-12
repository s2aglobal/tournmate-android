package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.PlayerOfMatch
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.RosterMember
import com.s2aglobal.tournmate.domain.model.TeamRoles
import com.s2aglobal.tournmate.ui.component.OptionSheetCard
import com.s2aglobal.tournmate.ui.component.OptionSheetDivider
import com.s2aglobal.tournmate.ui.component.OptionSheetRow
import com.s2aglobal.tournmate.ui.component.OptionSheetScaffold
import com.s2aglobal.tournmate.ui.component.OptionSheetSectionLabel
import com.s2aglobal.tournmate.ui.component.OptionSheetStyle
import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos

/**
 * Player of the Match picker, styled like the Sort & filter sheet: one section
 * per team (players and guests). "Clear" removes the award. Closes 200ms after
 * a pick. Mirrors iOS `PlayerOfMatchSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerOfMatchSheet(
    teams: List<Registration>,
    selection: PlayerOfMatch?,
    onSelect: (PlayerOfMatch?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = rememberSheetCloser(sheetState, scope, onDismiss)

    fun choose(pick: PlayerOfMatch?) {
        onSelect(pick)
        close(200)
    }

    OptionSheetScaffold(sheetState = sheetState, onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sheetScrollLikeIos().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            OptionSheetHeader(
                title = "Player of the match",
                subtitle = null,
                action = "Clear",
                actionEnabled = selection != null,
                onAction = { choose(null) },
            )
            teams.forEach { team ->
                val members = team.rosterMembers
                val teamId = team.id.toString().uppercase()
                OptionSheetSectionLabel(team.displayName().uppercase(), Modifier.padding(top = 20.dp))
                OptionSheetCard(Modifier.padding(top = 8.dp).selectableGroup()) {
                    if (members.isEmpty()) {
                        Box(Modifier.fillMaxWidth().heightIn(min = OptionSheetStyle.RowHeight), contentAlignment = Alignment.Center) {
                            Text("No players named.", fontSize = 14.sp, color = OptionSheetStyle.Secondary)
                        }
                    }
                    members.forEachIndexed { idx, member ->
                        if (idx > 0) OptionSheetDivider()
                        val isSelected = selection?.teamRegistrationId.equals(teamId, ignoreCase = true) &&
                            selection?.let { TeamRoles.key(it.key) } == member.key
                        OptionSheetRow(member.name, isSelected, subtitle = playerOfMatchSubtitle(member, team)) {
                            choose(PlayerOfMatch(member.key, member.name, teamId))
                        }
                    }
                }
            }
        }
    }
}

/** "Captain · All-rounder", "Guest", …; null when there's nothing to say. */
internal fun playerOfMatchSubtitle(member: RosterMember, team: Registration): String? {
    val roles = team.roles.normalized(team.rosterMembers.map { it.key }, preferredCaptain = team.playerId)
    val parts = buildList {
        if (roles.isCaptain(member.key)) add("Captain")
        roles.playingRole(member.key)?.let { add(it.title) }
        if (member.isGuest) add("Guest")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
