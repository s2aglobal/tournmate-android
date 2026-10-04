package com.s2aglobal.tournmate.ui.screen.tournament

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.domain.model.TournamentStatus
import com.s2aglobal.tournmate.ui.component.AppPrimaryButton
import com.s2aglobal.tournmate.ui.component.PlayPullToRefresh
import com.s2aglobal.tournmate.util.ShareUtil
import com.s2aglobal.tournmate.ui.component.TournamentCard
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.component.SportBadge
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.theme
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset

@Composable
fun TournamentListScreen(
    isGuest: Boolean,
    onTournamentClick: (Tournament) -> Unit,
    onHostClick: () -> Unit,
    viewModel: TournamentListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCancelDialog by remember { mutableStateOf<Tournament?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Tournament?>(null) }
    var sortNewestFirst by remember { mutableStateOf(true) }

    // Create / cancel / delete error alert
    state.createError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCreateError() },
            title = { Text("Unable to Post") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCreateError() }) { Text("OK") }
            },
        )
    }

    // Cancel dialog
    showCancelDialog?.let { tournament ->
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancel Tournament?") },
            text = {
                val fee = tournament.formattedFee
                Text(
                    if (fee != null) "This will cancel \"${tournament.title}\". Registered players paid $fee each.\n\n⚠️ Please arrange refunds for all registered players before confirming."
                    else "This will cancel \"${tournament.title}\". All registered players will be notified. This action cannot be undone."
                )
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) { Text("Keep Tournament") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Tournament cancelled", Toast.LENGTH_SHORT).show()
                        viewModel.cancel(tournament)
                        showCancelDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                ) { Text("Cancel Tournament") }
            },
        )
    }

    // Delete dialog
    showDeleteDialog?.let { tournament ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Tournament?") },
            text = {
                val fee = tournament.formattedFee
                Text(
                    if (fee != null) "This will permanently delete \"${tournament.title}\" and all its data.\n\n⚠️ Registered players paid $fee each. Please ensure all refunds have been processed before deleting."
                    else "This will permanently delete \"${tournament.title}\" and all its registrations, matches, and data. This cannot be undone."
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Keep") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Tournament deleted", Toast.LENGTH_SHORT).show()
                        viewModel.delete(tournament)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                ) { Text("Delete Permanently") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        if (!isGuest) {
            FilterChips(
                selectedFilter = state.filter,
                onFilterSelected = { viewModel.setFilter(it) },
            )
        }

        PlayPullToRefresh(
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.errorMessage != null -> {
                    ErrorState(
                        message = state.errorMessage!!,
                        onRetry = { viewModel.load() },
                    )
                }
                state.isMyFilterEmpty -> {
                    MyTournamentsEmptyState(
                        onBrowse = { viewModel.setFilter(TournamentFilter.ALL) },
                        onCreate = onHostClick,
                    )
                }
                state.isEmpty && !state.isLoading -> {
                    GlobalEmptyState(
                        isGuest = isGuest,
                        onCreateClick = onHostClick,
                    )
                }
                state.isLoading && state.isEmpty -> {
                    SkeletonLoading()
                }
                else -> {
                    TournamentList(
                        state = state,
                        sortNewestFirst = sortNewestFirst,
                        onSortChange = { sortNewestFirst = it },
                        onTournamentClick = onTournamentClick,
                        onShare = { ShareUtil.shareTournament(context, it) },
                        onCancel = { showCancelDialog = it },
                        onDelete = { showDeleteDialog = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: TournamentFilter,
    onFilterSelected: (TournamentFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(TournamentFilter.entries) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) Color.Black else Color.White,
                border = if (!isSelected) ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.1f))
                ) else null,
            ) {
                Text(
                    text = filter.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TournamentList(
    state: TournamentListUiState,
    sortNewestFirst: Boolean,
    onSortChange: (Boolean) -> Unit,
    onTournamentClick: (Tournament) -> Unit,
    onShare: (Tournament) -> Unit,
    onCancel: (Tournament) -> Unit,
    onDelete: (Tournament) -> Unit,
) {
    val isCreator: (Tournament) -> Boolean = { t ->
        state.firebaseUid != null && t.createdBy == state.firebaseUid
    }
    val sections = buildList {
        if (state.filter == TournamentFilter.MINE) {
            add(ListSection("posted", "Tournaments I Posted", Icons.Default.EditNote, state.myPostedTournaments, false))
            add(ListSection("enrolled", "Tournaments I Enrolled", Icons.Default.PersonAdd, state.myEnrolledTournaments, false))
            add(ListSection("mine_live", "Live Now", Icons.Default.Sensors, state.inProgressTournaments, false))
        } else {
            add(ListSection("live", "Live Now", Icons.Default.Sensors, state.inProgressTournaments, false))
            add(ListSection("upcoming", "Upcoming", Icons.Default.CalendarToday, state.trulyUpcomingTournaments, false))
            add(ListSection("completed", "Completed", Icons.Default.History, state.pastTournaments, true))
        }
    }.filter { it.tournaments.isNotEmpty() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        sections.forEachIndexed { index, section ->
            val list = if (sortNewestFirst) section.tournaments else section.tournaments.sortedByDescending { it.date }
            item(key = "header_${section.key}") {
                SectionHeader(
                    title = section.title,
                    icon = section.icon,
                    count = list.size,
                    sortNewestFirst = sortNewestFirst,
                    onSortChange = onSortChange,
                    modifier = if (index > 0) Modifier.padding(top = 16.dp) else Modifier,
                )
            }
            items(list, key = { "${section.key}_${it.id}" }) { tournament ->
                TournamentCardItem(
                    tournament = tournament,
                    isPast = section.isPast,
                    isCreator = isCreator(tournament),
                    onClick = { onTournamentClick(tournament) },
                    onShare = { onShare(tournament) },
                    onCancel = { onCancel(tournament) },
                    onDelete = { onDelete(tournament) },
                )
            }
        }
    }
}

private data class ListSection(
    val key: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tournaments: List<Tournament>,
    val isPast: Boolean,
)

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    sortNewestFirst: Boolean,
    onSortChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppAccent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$count TOTAL",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AppAccent,
            modifier = Modifier
                .background(AppAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = AppAccent)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Soonest First") },
                    onClick = { onSortChange(true); menuExpanded = false },
                    trailingIcon = { if (sortNewestFirst) Icon(Icons.Default.Check, null) },
                )
                DropdownMenuItem(
                    text = { Text("Latest First") },
                    onClick = { onSortChange(false); menuExpanded = false },
                    trailingIcon = { if (!sortNewestFirst) Icon(Icons.Default.Check, null) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TournamentCardItem(
    tournament: Tournament,
    isPast: Boolean,
    isCreator: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true },
            ),
    ) {
        TournamentCard(tournament = tournament, isPast = isPast)
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, null) },
                onClick = { menuExpanded = false; onShare() },
            )
            if (isCreator) {
                if (tournament.status != TournamentStatus.CANCELLED) {
                    DropdownMenuItem(
                        text = { Text("Cancel Tournament") },
                        leadingIcon = { Icon(Icons.Default.HighlightOff, null) },
                        onClick = { menuExpanded = false; onCancel() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF9800),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Couldn't Load Tournaments",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = message, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        // iOS AppEmptyState CTA: tinted capsule in the state's accent (orange), badge icon + label.
        val retryTint = Color(0xFFFF9800)
        Row(
            Modifier
                .clip(CircleShape)
                .background(retryTint.copy(alpha = 0.12f))
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = retryTint)
            Text("Try Again", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = retryTint)
        }
    }
}

@Composable
private fun MyTournamentsEmptyState(
    onBrowse: () -> Unit,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))

        val sport = CurrentSport.sport
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = sport.theme.tint,
            ) {}
            SportBadge(sport, 64.dp, Modifier.rotate(-12f))
        }

        Spacer(Modifier.height(24.dp))
        Text("No Tournaments Yet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Start your first ${sport.inlineName} tournament or\njoin one happening nearby.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(24.dp))
        // iOS .appPrimary, 40pt from the screen edges (this column already insets 20).
        AppPrimaryButton(onClick = onBrowse, modifier = Modifier.padding(horizontal = 20.dp)) {
            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Browse Tournaments")
        }

        Spacer(Modifier.height(24.dp))

        ActionCard(
            icon = Icons.Default.EmojiEvents,
            iconColor = Color(0xFFFF9800).copy(alpha = 0.8f),
            title = "Create Tournament",
            subtitle = "Host and manage your own event",
            onClick = onCreate,
        )

        Spacer(Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Default.People,
            iconColor = AppAccent,
            title = "Join Tournament",
            subtitle = "Register and compete with others",
            onClick = onBrowse,
        )
    }
}

@Composable
private fun GlobalEmptyState(isGuest: Boolean, onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        val sport = CurrentSport.sport
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = sport.theme.tint,
            ) {}
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = sport.theme.primary.copy(alpha = 0.75f),
            )
            SportBadge(
                sport, 40.dp,
                Modifier
                    .offset(x = 38.dp, y = 34.dp)
                    .rotate(-15f)
                    .shadow(4.dp, CircleShape),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("No Tournaments Yet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isGuest) "Sign in to create or browse upcoming\n${sport.inlineName} tournaments near you."
            else "Be the first to organize a tournament!\nTap HOST to ${sport.theme.gearPhrase}.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        if (!isGuest) {
            Spacer(Modifier.height(24.dp))
            // iOS .appPrimary, 40pt from the screen edges (this column already insets 40).
            AppPrimaryButton(onClick = onCreateClick) {
                Icon(Icons.Default.AddCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Tournament")
            }
        }

        Spacer(Modifier.height(32.dp))
        TipRow(icon = Icons.Default.CalendarMonth, color = Color(0xFFFF9800), text = "Organizers publish tournaments with date, venue, and format.")
        Spacer(Modifier.height(12.dp))
        TipRow(icon = Icons.Default.People, color = Color(0xFF2196F3), text = "Players register and get paired for singles or doubles.")
        Spacer(Modifier.height(12.dp))
        TipRow(icon = Icons.Default.BarChart, color = AppAccent, text = "Play matches, track scores, and climb the Elo rankings.")
    }
}

@Composable
private fun SkeletonLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF5F5F5),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = AppAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = iconColor.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(18.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(16.dp), tint = AppAccent)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun TipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
            }
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray,
            lineHeight = 18.sp,
        )
    }
}
