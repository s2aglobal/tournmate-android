package com.s2aglobal.tournmate.ui.screen.openplay

import com.s2aglobal.tournmate.ui.component.LocalTabBarClearance
import com.s2aglobal.tournmate.ui.component.TabBarContentGap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.s2aglobal.tournmate.ui.component.StickyEmptyState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.s2aglobal.tournmate.ui.component.MapPinCircle
import com.s2aglobal.tournmate.ui.component.PlayPullToRefresh
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
import com.s2aglobal.tournmate.ui.theme.AccentGradient
import com.s2aglobal.tournmate.ui.component.AppPrimaryButton
import com.s2aglobal.tournmate.ui.component.ListSortOption
import com.s2aglobal.tournmate.ui.component.ListSortStore
import com.s2aglobal.tournmate.ui.component.LocationSortNote
import com.s2aglobal.tournmate.ui.component.SortFilterIcon
import com.s2aglobal.tournmate.ui.component.SortOptionsSheet
import com.s2aglobal.tournmate.ui.component.rememberListSortOption
import com.s2aglobal.tournmate.ui.component.rememberSortLocationState
import com.s2aglobal.tournmate.ui.component.sortedForList
import androidx.compose.ui.graphics.Brush
import com.s2aglobal.tournmate.ui.component.SportArtworkImage
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
    val context = LocalContext.current
    val (sortOption, setSortOption) = rememberListSortOption(ListSortStore.KEY_OPEN_PLAY)
    val sortLocation = rememberSortLocationState(sortOption)
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        SortOptionsSheet(
            selected = sortOption,
            onSelect = { setSortOption(it); sortLocation.onOptionChosen(it) },
            onDismiss = { showSortSheet = false },
        )
    }
    var sessionToCancel by remember { mutableStateOf<PlaySession?>(null) }
    var sessionToDelete by remember { mutableStateOf<PlaySession?>(null) }

    sessionToCancel?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToCancel = null },
            title = { Text("Cancel Session?") },
            text = { Text("This will cancel the session. All joined players will be notified. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    Toast.makeText(context, "Session cancelled", Toast.LENGTH_SHORT).show()
                    viewModel.cancelSession(session.id.toString().uppercase())
                    sessionToCancel = null
                }) { Text("Cancel Session", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { sessionToCancel = null }) { Text("Keep") } },
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("This will permanently delete this session and all its data. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                    viewModel.deleteSession(session.id.toString().uppercase())
                    sessionToDelete = null
                }) { Text("Delete Permanently", color = ErrorRed) }
            },
            dismissButton = { TextButton(onClick = { sessionToDelete = null }) { Text("Keep") } },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isGuest) {
            CapsuleFilterRow(
                currentFilter = uiState.filter,
                onFilterSelected = { viewModel.setFilter(it) },
            )
        }

        PlayPullToRefresh(
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            val sessions = uiState.displaySessions
            val isCompleted = uiState.filter == OpenPlayFilter.COMPLETED
            val live = if (isCompleted) emptyList() else sessions.filter { isSessionLive(it) }
            val upcoming = if (isCompleted) emptyList() else sessions.filter { !isSessionLive(it) && !isSessionEnded(it) }
            val past = if (isCompleted) sessions else emptyList()
            val errorMessage = uiState.errorMessage

            when {
                errorMessage != null && uiState.isEmpty && !uiState.isLoading ->
                    ErrorState(message = errorMessage, onRetry = { viewModel.load() })
                uiState.isMyFilterEmpty ->
                    MySessionsEmptyState(onBrowseClick = { viewModel.setFilter(OpenPlayFilter.ALL_SESSIONS) })
                uiState.isLoading && uiState.isEmpty -> SkeletonLoading()
                live.isEmpty() && upcoming.isEmpty() && past.isEmpty() ->
                    EmptyState(isGuest = isGuest, onHostClick = onHostClick)
                else -> {
                    val sections = listOf(
                        Triple("Live Now", Icons.Filled.Sensors, live),
                        Triple("Upcoming", Icons.Filled.CalendarToday, upcoming),
                        Triple("Completed", Icons.Filled.History, past),
                    ).filter { it.third.isNotEmpty() }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = maxOf(100.dp, LocalTabBarClearance.current + TabBarContentGap),
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Only when a section the sort applies to is visible.
                        if (sortLocation.unavailable && sections.any { it.first != "Completed" }) {
                            item(key = "sort_location_note") { LocationSortNote() }
                        }
                        sections.forEachIndexed { index, (title, icon, list) ->
                            val sorted = list.sortedForList(sortOption, sortLocation.userLocation, isPast = title == "Completed")
                            item(key = "header_$title") {
                                SectionHeader(
                                    title = title,
                                    icon = icon,
                                    count = sorted.size,
                                    sortOption = sortOption,
                                    showSort = title != "Completed",
                                    onSortClick = { showSortSheet = true },
                                    modifier = if (index > 0) Modifier.padding(top = 8.dp) else Modifier,
                                )
                            }
                            items(sorted, key = { "${title}_${it.id}" }) { session ->
                                SessionCard(
                                    session = session,
                                    isPast = title == "Completed",
                                    isHost = uiState.isHost(session),
                                    onClick = { onSessionClick(session) },
                                    onCancel = { sessionToCancel = session },
                                    onDelete = { sessionToDelete = session },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sessionEndMillis(s: PlaySession): Long = s.date.time + (s.durationMinutes ?: 120) * 60_000L

private fun isSessionLive(s: PlaySession): Boolean {
    if (s.status == PlaySessionStatus.CANCELLED) return false
    val now = System.currentTimeMillis()
    return s.date.time <= now && sessionEndMillis(s) > now
}

private fun isSessionEnded(s: PlaySession): Boolean = sessionEndMillis(s) <= System.currentTimeMillis()

@Composable
private fun CapsuleFilterRow(
    currentFilter: OpenPlayFilter,
    onFilterSelected: (OpenPlayFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(bottom = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(OpenPlayFilter.entries) { filter ->
            val isSelected = currentFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) Color.Black else Color.White,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
            ) {
                Text(
                    text = filter.displayName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Black,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    count: Int,
    sortOption: ListSortOption,
    showSort: Boolean,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppAccent,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

        // Past sections are always newest first, so they get no sort control.
        if (showSort) SortFilterIcon(option = sortOption, onClick = onSortClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    session: PlaySession,
    isPast: Boolean,
    isHost: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val (statusText, statusColor) = sessionStatusBadge(session, isPast)
    val accentColor = statusColor

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isPast) 0.8f else 1f)
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (isHost) menuExpanded = true },
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 2.dp,
        ) {
            Box {
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
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SessionBadge(statusText.uppercase(), statusColor)
                            SessionBadge(session.skillLevel.displayName, skillLevelColor(session.skillLevel), fontSize = 10, weight = FontWeight.Bold)
                            SessionBadge(session.gameType.displayName, AppAccent, fontSize = 10, weight = FontWeight.Bold, bgAlpha = 0.1f)
                        }

                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SportArtworkImage(session.sportType, 18.dp)
                            Text(
                                text = dateFormatter.format(session.date),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                maxLines = 1,
                                softWrap = false,
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
                            MapPinCircle(Modifier.padding(top = 1.dp))
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
                            modifier = Modifier.weight(1f),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                                    imageVector = Icons.Filled.People,
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

                // iOS overlays the 4pt accent bar on the leading edge (it takes no content width).
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(vertical = 30.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor),
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (session.status != PlaySessionStatus.CANCELLED) {
                DropdownMenuItem(
                    text = { Text("Cancel Session") },
                    leadingIcon = { Icon(Icons.Filled.HighlightOff, null) },
                    onClick = { menuExpanded = false; onCancel() },
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", color = ErrorRed) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = ErrorRed) },
                onClick = { menuExpanded = false; onDelete() },
            )
        }
    }
}

@Composable
private fun SessionBadge(
    text: String,
    color: Color,
    fontSize: Int = 9,
    weight: FontWeight = FontWeight.ExtraBold,
    bgAlpha: Float = 0.15f,
) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontWeight = weight,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(color.copy(alpha = bgAlpha), RoundedCornerShape(6.dp))
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
    isGuest: Boolean,
    onHostClick: () -> Unit,
) {
    val sport = CurrentSport.sport
    StickyEmptyState(
        title = "No Open Play Sessions",
        subtitle = if (isGuest) "Sign in to post a session or join others."
        else "Be the first to post a ${sport.inlineName} session! Invite others to play.",
        hero = { SessionsHero(heroSize = 120.dp, badgeSize = 64.dp) },
        footer = if (isGuest) null else {
            {
                PrimaryActionButton(
                    text = "Post a Session",
                    icon = Icons.Filled.AddCircle,
                    onClick = onHostClick,
                    fill = sport.theme.gradient,
                    glow = sport.theme.primary,
                )
            }
        },
    )
}

@Composable
private fun MySessionsEmptyState(onBrowseClick: () -> Unit) {
    StickyEmptyState(
        title = "No Sessions Yet",
        subtitle = "Post your own session or join one from the All Sessions tab.",
        hero = { SessionsHero(heroSize = 100.dp, badgeSize = 52.dp) },
        footer = {
            PrimaryActionButton(text = "Browse Sessions", icon = Icons.Filled.Search, onClick = onBrowseClick)
        },
    )
}

@Composable
private fun SessionsHero(
    heroSize: androidx.compose.ui.unit.Dp,
    badgeSize: androidx.compose.ui.unit.Dp,
) {
    val sport = CurrentSport.sport
    Box(
        modifier = Modifier
            .size(heroSize)
            .clip(CircleShape)
            .background(sport.theme.tint),
        contentAlignment = Alignment.Center,
    ) {
        SportArtworkImage(sport, badgeSize, Modifier.rotate(-12f))
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    fill: Brush = AccentGradient,
    glow: Color = AppAccent,
) {
    // iOS `.buttonStyle(.appPrimary)` / `.appPrimary(sport)`.
    AppPrimaryButton(onClick = onClick, fill = fill, glow = glow) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Filled.Warning, null, Modifier.size(40.dp), tint = WarningOrange)
        Text("Couldn't Load Sessions", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(message, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        // iOS: `.appPrimary` with 60pt horizontal padding (column already pads 40).
        AppPrimaryButton(
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) { Text("Try Again") }
    }
}

@Composable
private fun SkeletonLoading() {
    val bar = Color.Gray.copy(alpha = 0.15f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(70.dp, 18.dp).background(bar, RoundedCornerShape(50)))
                        Box(Modifier.size(50.dp, 18.dp).background(bar, RoundedCornerShape(50)))
                    }
                    Box(Modifier.fillMaxWidth().height(20.dp).background(bar))
                    Box(Modifier.fillMaxWidth().height(14.dp).background(bar))
                }
            }
        }
    }
}
