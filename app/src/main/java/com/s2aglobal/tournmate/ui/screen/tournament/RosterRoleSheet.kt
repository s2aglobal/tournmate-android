package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.PlayingRole
import com.s2aglobal.tournmate.domain.model.RosterMember
import com.s2aglobal.tournmate.domain.model.TeamRoles
import com.s2aglobal.tournmate.ui.component.OptionSheetCard
import com.s2aglobal.tournmate.ui.component.OptionSheetDivider
import com.s2aglobal.tournmate.ui.component.OptionSheetRow
import com.s2aglobal.tournmate.ui.component.OptionSheetScaffold
import com.s2aglobal.tournmate.ui.component.OptionSheetSectionLabel
import com.s2aglobal.tournmate.ui.component.OptionSheetStyle
import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Role picker for one player on a team entry, styled like the Sort & filter
 * sheet: captain / vice-captain / wicketkeeper, a playing role, and remove from
 * this event. Changes apply as they're tapped. Mirrors iOS `RosterRoleSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RosterRoleSheet(
    member: RosterMember,
    /** "You", "TournMate player" or "Guest · no rating". */
    subtitle: String,
    roles: TeamRoles,
    onRolesChange: (TeamRoles) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close = rememberSheetCloser(sheetState, scope, onDismiss)
    val key = member.key
    val isCaptain = roles.isCaptain(key)

    OptionSheetScaffold(sheetState = sheetState, onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().sheetScrollLikeIos().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            OptionSheetHeader(
                title = member.name,
                subtitle = subtitle,
                action = "Done",
                actionEnabled = true,
                onAction = { close(0) },
            )
            OptionSheetSectionLabel("LEADERSHIP", Modifier.padding(top = 20.dp))
            OptionSheetCard(Modifier.padding(top = 8.dp)) {
                OptionSheetRow("Captain", isCaptain, role = Role.Checkbox) { onRolesChange(roles.setCaptain(key)) }
                OptionSheetDivider()
                OptionSheetRow("Vice-captain", roles.isViceCaptain(key), isEnabled = !isCaptain, role = Role.Checkbox) {
                    onRolesChange(roles.toggleViceCaptain(key))
                }
                OptionSheetDivider()
                OptionSheetRow("Wicketkeeper", roles.isKeeper(key), role = Role.Checkbox) { onRolesChange(roles.toggleKeeper(key)) }
            }
            Text(
                if (isCaptain) "Every team needs a captain. To hand over, open another player and make them captain."
                else "One captain, one vice-captain and one wicketkeeper per team.",
                fontSize = 12.sp,
                color = OptionSheetStyle.Secondary,
                // Two lines either way, so the sheet doesn't resize when the caption changes.
                minLines = 2,
                maxLines = 2,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )

            OptionSheetSectionLabel("PLAYING ROLE", Modifier.padding(top = 20.dp))
            OptionSheetCard(Modifier.padding(top = 8.dp)) {
                OptionSheetRow("Not set", roles.playingRole(key) == null) { onRolesChange(roles.setPlayingRole(null, key)) }
                PlayingRole.entries.forEach { role ->
                    OptionSheetDivider()
                    OptionSheetRow(role.title, roles.playingRole(key) == role) { onRolesChange(roles.setPlayingRole(role, key)) }
                }
            }

            OptionSheetCard(Modifier.padding(top = 20.dp)) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = OptionSheetStyle.RowHeight)
                        .clickable(role = Role.Button) { close(0); onRemove() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Remove from this event", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF3B30))
                }
            }
        }
    }
}

/** Title (24 bold) with an optional subtitle, and a text action on the right (Done / Clear / Reset). */
@Composable
internal fun OptionSheetHeader(
    title: String,
    subtitle: String?,
    action: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, fontSize = 14.sp, color = OptionSheetStyle.Secondary) }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            action,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (actionEnabled) OptionSheetStyle.Selected else OptionSheetStyle.Secondary,
            modifier = Modifier.clickable(enabled = actionEnabled, role = Role.Button, onClick = onAction).padding(vertical = 4.dp),
        )
    }
}

/** Hides the sheet (after [afterMillis]) and then calls [onDismiss]; repeated calls are ignored. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetCloser(sheetState: SheetState, scope: CoroutineScope, onDismiss: () -> Unit): (Long) -> Unit {
    var closing by remember { mutableStateOf(false) }
    return { afterMillis ->
        if (!closing) {
            closing = true
            scope.launch {
                if (afterMillis > 0) delay(afterMillis)
                sheetState.hide()
            }.invokeOnCompletion { onDismiss() }
        }
    }
}
