package com.s2aglobal.tournmate.ui.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    onBack: () -> Unit,
    onNavigateToTournament: (String) -> Unit = {},
    onNavigateToSession: (String) -> Unit = {},
    viewModel: NotificationInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    if (state.notifications.any { !it.read }) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Read All", color = BrandPurple, fontSize = 13.sp)
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
                        color = BrandPurple,
                    )
                }

                state.notifications.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
    val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(BrandPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationIcon(item.type),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = BrandPurple,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = if (!item.read) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                )
                if (!item.read) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandPurple),
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = item.body,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = dateFmt.format(item.createdAt),
                fontSize = 11.sp,
                color = Color.LightGray,
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = Color.LightGray)
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
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BrandPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = BrandPurple,
            )
        }
        Text("All caught up!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Notifications for new tournaments and open play sessions in your area will appear here.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private fun notificationIcon(type: String): ImageVector = when {
    type.contains("session", ignoreCase = true) -> Icons.Default.SportsHandball
    type.contains("tournament", ignoreCase = true) -> Icons.Default.EmojiEvents
    type.contains("join", ignoreCase = true) ||
    type.contains("player", ignoreCase = true) -> Icons.Default.People
    type.contains("score", ignoreCase = true) ||
    type.contains("match", ignoreCase = true) -> Icons.Default.CheckCircle
    else -> Icons.Default.Notifications
}
