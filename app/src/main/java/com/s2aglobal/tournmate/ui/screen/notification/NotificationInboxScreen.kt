package com.s2aglobal.tournmate.ui.screen.notification

import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.EmojiEvents as EmojiEventsOutlined
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.ui.theme.AppAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    onBack: () -> Unit,
    onNavigateToTournament: (String) -> Unit = {},
    onNavigateToSession: (String) -> Unit = {},
    viewModel: NotificationInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.load() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            // iOS: inline title, xmark close on the leading edge.
            CenterAlignedTopAppBar(
                title = { Text("Notifications", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Close", modifier = Modifier.size(20.dp), tint = SecondaryText)
                    }
                },
                actions = {
                    if (state.notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            enabled = state.notifications.any { !it.read },
                        ) {
                            Text(
                                "Read All",
                                color = if (state.notifications.any { !it.read }) AppAccent else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = AppAccent)
                        Text("Loading...", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                state.notifications.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    // iOS: plain list — full-width rows separated by inset dividers.
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(state.notifications, key = { _, item -> item.id }) { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0x4D3C3C43),
                                )
                            }
                            SwipeToDeleteRow(onDelete = { viewModel.delete(item) }) {
                                NotificationRow(
                                    item = item,
                                    onClick = {
                                        viewModel.markAsRead(item)
                                        item.tournamentId?.let { onNavigateToTournament(it) }
                                            ?: item.sessionId?.let { onNavigateToSession(it) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Only reveal the red delete action while the row is being swiped.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF3B30))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text("Delete", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { content() }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    val iconStyle = notificationIconStyle(item.type)

    // Opaque white row (so the swipe background never bleeds through), with the
    // iOS unread tint as a rounded card inside the 16/12 row insets.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (item.read) Color.White else AppAccent.copy(alpha = 0.04f))
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconStyle.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                when (iconStyle.useSportIcon) {
                    true -> Icon(
                        painter = sportIconPainter(CurrentSport.sport),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconStyle.color,
                    )
                    false -> Icon(
                        imageVector = iconStyle.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconStyle.color,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = if (!item.read) FontWeight.Bold else FontWeight.Medium,
                    color = Color.Black,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontSize = 13.sp,
                    color = SecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = timeAgo(item.createdAt),
                    fontSize = 11.sp,
                    color = TertiaryText,
                )
            }

            if (!item.read) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppAccent),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.Gray,
        )
        Text("No Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "You'll see tournament updates, match results, and session reminders here.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
    }
}

// iOS label colors: .secondary / .tertiary on a light background.
private val SecondaryText = Color(0x993C3C43)
private val TertiaryText = Color(0x4D3C3C43)

private data class NotificationIconStyle(
    val icon: ImageVector,
    val color: Color,
    val useSportIcon: Boolean = false,
)

private fun notificationIconStyle(type: String): NotificationIconStyle = when (type) {
    "new_registration" -> NotificationIconStyle(Icons.Default.PersonAdd, Color(0xFF4CAF50))
    "tournament_reminder" -> NotificationIconStyle(Icons.Default.Event, Color(0xFFFF9800))
    "tournament_created" -> NotificationIconStyle(Icons.Outlined.EmojiEventsOutlined, AppAccent)
    "match_finished" -> NotificationIconStyle(Icons.Default.EmojiEvents, Color(0xFFFFC107))
    "tournament_cancelled" -> NotificationIconStyle(Icons.Default.Cancel, Color(0xFFE53935))
    "session_created" -> NotificationIconStyle(Icons.Default.SportsHandball, Color(0xFF2196F3), useSportIcon = true)
    "session_joined" -> NotificationIconStyle(Icons.Default.People, Color(0xFF4CAF50))
    "session_left" -> NotificationIconStyle(Icons.Default.PersonRemove, Color(0xFFFF9800))
    "session_finished" -> NotificationIconStyle(Icons.Default.SportsScore, Color(0xFF4CAF50))
    "player_unregistered" -> NotificationIconStyle(Icons.Default.PersonRemove, Color(0xFFE53935))
    else -> NotificationIconStyle(Icons.Default.Notifications, AppAccent)
}

private fun timeAgo(date: Date): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - date.time)
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    return SimpleDateFormat("M/d/yy", Locale.getDefault()).format(date)
}
