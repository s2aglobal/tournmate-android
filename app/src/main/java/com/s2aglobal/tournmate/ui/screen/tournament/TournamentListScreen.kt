package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.ui.component.TournamentCard
import com.s2aglobal.tournmate.ui.theme.BrandPurple

@Composable
fun TournamentListScreen(
    isGuest: Boolean,
    onTournamentClick: (Tournament) -> Unit,
    onHostClick: () -> Unit,
    viewModel: TournamentListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var showCancelDialog by remember { mutableStateOf<Tournament?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Tournament?>(null) }

    // Error dialog
    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Unable to Post") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
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
                    if (fee != null) "This will cancel \"${tournament.title}\". Registered players paid $fee each.\n\nPlease arrange refunds for all registered players before confirming."
                    else "This will cancel \"${tournament.title}\". All registered players will be notified. This action cannot be undone."
                )
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) { Text("Keep Tournament") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    if (fee != null) "This will permanently delete \"${tournament.title}\" and all its data.\n\nRegistered players paid $fee each. Please ensure all refunds have been processed before deleting."
                    else "This will permanently delete \"${tournament.title}\" and all its registrations, matches, and data. This cannot be undone."
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Keep") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
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

        // Content
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
                    onTournamentClick = onTournamentClick,
                    onCancel = { showCancelDialog = it },
                    onDelete = { showDeleteDialog = it },
                    onRefresh = { viewModel.load() },
                )
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
    onTournamentClick: (Tournament) -> Unit,
    onCancel: (Tournament) -> Unit,
    onDelete: (Tournament) -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        if (state.filter == TournamentFilter.MINE) {
            if (state.myPostedTournaments.isNotEmpty()) {
                item(key = "header_posted") {
                    SectionHeader(
                        title = "Tournaments I Posted",
                        icon = Icons.Default.Edit,
                        count = state.myPostedTournaments.size,
                    )
                }
                items(state.myPostedTournaments, key = { "posted_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = false,
                        isCreator = state.firebaseUid != null && tournament.createdBy == state.firebaseUid,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = { onCancel(tournament) },
                        onDelete = { onDelete(tournament) },
                    )
                }
            }
            if (state.myEnrolledTournaments.isNotEmpty()) {
                item(key = "header_enrolled") {
                    SectionHeader(
                        title = "Tournaments I Enrolled",
                        icon = Icons.Default.PersonAdd,
                        count = state.myEnrolledTournaments.size,
                    )
                }
                items(state.myEnrolledTournaments, key = { "enrolled_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = false,
                        isCreator = false,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = {},
                        onDelete = {},
                    )
                }
            }
            if (state.inProgressTournaments.isNotEmpty()) {
                item(key = "header_mine_live") {
                    SectionHeader(
                        title = "Live Now",
                        icon = Icons.Default.Sensors,
                        count = state.inProgressTournaments.size,
                    )
                }
                items(state.inProgressTournaments, key = { "mine_live_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = false,
                        isCreator = state.firebaseUid != null && tournament.createdBy == state.firebaseUid,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = { onCancel(tournament) },
                        onDelete = { onDelete(tournament) },
                    )
                }
            }
        } else {
            if (state.inProgressTournaments.isNotEmpty()) {
                item(key = "header_live") {
                    SectionHeader(
                        title = "Live Now",
                        icon = Icons.Default.Sensors,
                        count = state.inProgressTournaments.size,
                    )
                }
                items(state.inProgressTournaments, key = { "live_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = false,
                        isCreator = state.firebaseUid != null && tournament.createdBy == state.firebaseUid,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = { onCancel(tournament) },
                        onDelete = { onDelete(tournament) },
                    )
                }
            }
            if (state.trulyUpcomingTournaments.isNotEmpty()) {
                item(key = "header_upcoming") {
                    SectionHeader(
                        title = "Upcoming",
                        icon = Icons.Default.CalendarToday,
                        count = state.trulyUpcomingTournaments.size,
                    )
                }
                items(state.trulyUpcomingTournaments, key = { "upcoming_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = false,
                        isCreator = state.firebaseUid != null && tournament.createdBy == state.firebaseUid,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = { onCancel(tournament) },
                        onDelete = { onDelete(tournament) },
                    )
                }
            }
            if (state.pastTournaments.isNotEmpty()) {
                item(key = "header_completed") {
                    SectionHeader(
                        title = "Completed",
                        icon = Icons.Default.History,
                        count = state.pastTournaments.size,
                    )
                }
                items(state.pastTournaments, key = { "completed_${it.id}" }) { tournament ->
                    TournamentCardItem(
                        tournament = tournament,
                        isPast = true,
                        isCreator = state.firebaseUid != null && tournament.createdBy == state.firebaseUid,
                        onClick = { onTournamentClick(tournament) },
                        onCancel = { onCancel(tournament) },
                        onDelete = { onDelete(tournament) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPurple,
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
            color = BrandPurple,
            modifier = Modifier
                .background(BrandPurple.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TournamentCardItem(
    tournament: Tournament,
    isPast: Boolean,
    isCreator: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        TournamentCard(tournament = tournament, isPast = isPast)
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
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            shape = RoundedCornerShape(50),
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
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
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))

        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = BrandPurple.copy(alpha = 0.08f),
            ) {}
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = BrandPurple.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("No Tournaments Yet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Start your first badminton tournament or\njoin one happening nearby.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBrowse,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            shape = RoundedCornerShape(50),
        ) {
            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Browse Tournaments", fontWeight = FontWeight.SemiBold)
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
            iconColor = BrandPurple,
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
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = BrandPurple.copy(alpha = 0.08f),
            ) {}
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = BrandPurple.copy(alpha = 0.5f),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("No Tournaments Yet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isGuest) "Sign in to create or browse upcoming\nbadminton tournaments near you."
            else "Be the first to organize a tournament!\nTap HOST to get the shuttlecocks flying.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        if (!isGuest) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Default.AddCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Tournament", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(32.dp))
        TipRow(icon = Icons.Default.CalendarMonth, color = Color(0xFFFF9800), text = "Organizers publish tournaments with date, venue, and format.")
        Spacer(Modifier.height(12.dp))
        TipRow(icon = Icons.Default.People, color = Color(0xFF2196F3), text = "Players register and get paired for singles or doubles.")
        Spacer(Modifier.height(12.dp))
        TipRow(icon = Icons.Default.BarChart, color = BrandPurple, text = "Play matches, track scores, and climb the Elo rankings.")
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
                        color = BrandPurple,
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
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconColor.copy(alpha = 0.1f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
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
