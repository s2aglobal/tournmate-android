package com.s2aglobal.tournmate.ui.screen.notification

import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color(0xFFF2F2F7),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppAccent,
                    )
                }

                state.notifications.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { item ->
                            NotificationRow(
                                item = item,
                                onClick = {
                                    viewModel.markAsRead(item)
                                    item.tournamentId?.let { onNavigateToTournament(it) }
                                        ?: item.sessionId?.let { onNavigateToSession(it) }
                                },
                                onDelete = { viewModel.delete(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val iconStyle = notificationIconStyle(item.type)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (item.read) Color.White else AppAccent.copy(alpha = 0.04f),
        shadowElevation = if (item.read) 0.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                        painter = sportIconPainter(CurrentSport.sport, R.drawable.ic_badminton),
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
                    color = Color.Gray,
                    maxLines = 3,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = timeAgo(item.createdAt),
                    fontSize = 11.sp,
                    color = Color.LightGray,
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

private data class NotificationIconStyle(
    val icon: ImageVector,
    val color: Color,
    val useSportIcon: Boolean = false,
)

private fun notificationIconStyle(type: String): NotificationIconStyle = when (type) {
    "new_registration" -> NotificationIconStyle(Icons.Default.PersonAdd, Color(0xFF4CAF50))
    "tournament_reminder" -> NotificationIconStyle(Icons.Default.Schedule, Color(0xFFFF9800))
    "tournament_created" -> NotificationIconStyle(Icons.Default.EmojiEvents, AppAccent)
    "match_finished" -> NotificationIconStyle(Icons.Default.EmojiEvents, Color(0xFFFFC107))
    "tournament_cancelled" -> NotificationIconStyle(Icons.Default.Cancel, Color(0xFFE53935))
    "session_created" -> NotificationIconStyle(Icons.Default.SportsHandball, Color(0xFF2196F3), useSportIcon = true)
    "session_joined" -> NotificationIconStyle(Icons.Default.People, Color(0xFF4CAF50))
    "session_left" -> NotificationIconStyle(Icons.Default.PersonRemove, Color(0xFFFF9800))
    "session_finished" -> NotificationIconStyle(Icons.Default.Flag, Color(0xFF4CAF50))
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
