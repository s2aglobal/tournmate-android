package com.s2aglobal.tournmate.ui.screen.openplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.PlaySession
import com.s2aglobal.tournmate.domain.model.PlaySessionStatus
import com.s2aglobal.tournmate.domain.model.SkillLevel
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.component.SportBadge
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.theme
import androidx.compose.ui.draw.rotate
import com.s2aglobal.tournmate.ui.theme.ErrorRed
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OpenPlayListScreen(
    viewModel: OpenPlayListViewModel,
    isGuest: Boolean = false,
    onSessionClick: (PlaySession) -> Unit = {},
    onHostClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortNewestFirst by remember { mutableStateOf(true) }

    val sortedSessions = remember(uiState.displaySessions, sortNewestFirst) {
        if (sortNewestFirst) uiState.displaySessions
        else uiState.displaySessions.sortedByDescending { it.date }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CapsuleFilterRow(
            currentFilter = uiState.filter,
            onFilterSelected = { viewModel.setFilter(it) },
        )

        if (uiState.filter == OpenPlayFilter.COMPLETED && sortedSessions.isNotEmpty()) {
            SectionHeader(
                count = sortedSessions.size,
                sortNewestFirst = sortNewestFirst,
                onSortChange = { sortNewestFirst = it },
            )
        }

        if (uiState.isLoading && uiState.displaySessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppAccent)
            }
        } else if (uiState.isEmpty && !uiState.isLoading) {
            EmptyState(
                filter = uiState.filter,
                isGuest = isGuest,
                onHostClick = onHostClick,
                onBrowseClick = { viewModel.setFilter(OpenPlayFilter.ALL_SESSIONS) },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sortedSessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        isPast = uiState.filter == OpenPlayFilter.COMPLETED ||
                            session.isPast ||
                            session.status == PlaySessionStatus.COMPLETED,
                        onClick = { onSessionClick(session) },
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
private fun CapsuleFilterRow(
    currentFilter: OpenPlayFilter,
    onFilterSelected: (OpenPlayFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OpenPlayFilter.entries.forEach { filter ->
            val isSelected = currentFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) Color.Black else Color.White,
                border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null,
                shadowElevation = if (!isSelected) 0.dp else 2.dp,
            ) {
                Text(
                    text = filter.displayName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.Black,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    count: Int,
    sortNewestFirst: Boolean,
    onSortChange: (Boolean) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppAccent,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Completed", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = AppAccent.copy(alpha = 0.1f),
        ) {
            Text(
                "$count TOTAL",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AppAccent,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Sort",
                    tint = AppAccent,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Soonest First") },
                    onClick = { onSortChange(true); menuExpanded = false },
                    trailingIcon = {
                        if (sortNewestFirst) Icon(Icons.Filled.Check, null)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Latest First") },
                    onClick = { onSortChange(false); menuExpanded = false },
                    trailingIcon = {
                        if (!sortNewestFirst) Icon(Icons.Filled.Check, null)
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: PlaySession,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val (statusText, statusColor) = sessionStatusBadge(session, isPast)
    val accentColor = statusColor

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .padding(vertical = 30.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SessionBadge(statusText.uppercase(), statusColor)
                        SessionBadge(session.skillLevel.displayName, skillLevelColor(session.skillLevel))
                        SessionBadge(session.gameType.displayName, AppAccent)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SportBadge(session.sportType, 16.dp)
                        Text(
                            text = dateFormatter.format(session.date),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                        )
                    }
                }

                Text(
                    text = session.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) Color.Gray else Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (session.venue.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray.copy(alpha = 0.5f),
                        )
                        Column {
                            Text(
                                text = session.venue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                            if (session.venueAddress.isNotBlank()) {
                                Text(
                                    text = session.venueAddress,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFF2F2F7),
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray,
                        )
                        Text(
                            text = buildString {
                                append(timeFormatter.format(session.date))
                                session.formattedDuration?.let { append(" · $it") }
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.Gray,
                            )
                            Text(
                                text = "${session.attendeeCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                            )
                        }

                        SessionFeeBadge(
                            text = session.formattedCost ?: "Free",
                            isFree = !session.hasCost,
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionBadge(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SessionFeeBadge(text: String, isFree: Boolean) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isFree) AppAccent else Color.Black,
        modifier = Modifier
            .background(
                if (isFree) AppAccent.copy(alpha = 0.1f) else Color(0xFFF2F2F7),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

private fun sessionStatusBadge(session: PlaySession, isPast: Boolean): Pair<String, Color> {
    if (session.status == PlaySessionStatus.CANCELLED) return "Cancelled" to ErrorRed
    if (isPast || session.status == PlaySessionStatus.COMPLETED) return "Completed" to Color.Gray
    val endTime = session.date.time + (session.durationMinutes ?: 120) * 60_000L
    val now = System.currentTimeMillis()
    if (session.date.time <= now && endTime > now) return "Live" to SuccessGreen
    return "Upcoming" to WarningOrange
}

private fun skillLevelColor(level: SkillLevel): Color = when (level) {
    SkillLevel.ALL_LEVELS -> SuccessGreen
    SkillLevel.BEGINNER -> Color(0xFF2196F3)
    SkillLevel.INTERMEDIATE -> WarningOrange
    SkillLevel.ADVANCED -> ErrorRed
}

@Composable
private fun EmptyState(
    filter: OpenPlayFilter,
    isGuest: Boolean,
    onHostClick: () -> Unit,
    onBrowseClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val sport = CurrentSport.sport
        val heroSize = if (filter == OpenPlayFilter.MY_SESSIONS) 100.dp else 120.dp
        Box(
            modifier = Modifier
                .size(heroSize)
                .clip(CircleShape)
                .background(sport.theme.tint),
            contentAlignment = Alignment.Center,
        ) {
            SportBadge(sport, if (filter == OpenPlayFilter.MY_SESSIONS) 52.dp else 64.dp, Modifier.rotate(-12f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when (filter) {
                OpenPlayFilter.ALL_SESSIONS -> "No Open Play Sessions"
                OpenPlayFilter.MY_SESSIONS -> "No Sessions Yet"
                OpenPlayFilter.COMPLETED -> "No completed sessions"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (filter) {
                OpenPlayFilter.ALL_SESSIONS -> if (isGuest) "Sign in to post a session or join others."
                else "Be the first to post a ${sport.inlineName} session!\nInvite others to play."
                OpenPlayFilter.MY_SESSIONS -> "Post your own session or join one\nfrom the All Sessions tab."
                OpenPlayFilter.COMPLETED -> "Completed sessions will show here."
            },
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (!isGuest) {
            when (filter) {
                OpenPlayFilter.ALL_SESSIONS -> {
                    Button(
                        onClick = onHostClick,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("+ Post a Session", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                OpenPlayFilter.MY_SESSIONS -> {
                    Button(
                        onClick = onBrowseClick,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("\uD83D\uDD0D Browse Sessions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                OpenPlayFilter.COMPLETED -> {}
            }
        }
    }
}
