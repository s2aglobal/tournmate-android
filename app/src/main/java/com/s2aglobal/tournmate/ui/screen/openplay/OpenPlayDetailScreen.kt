package com.s2aglobal.tournmate.ui.screen.openplay

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.theme.*
import com.s2aglobal.tournmate.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenPlayDetailScreen(
    session: PlaySession,
    attendees: List<Player>,
    currentPlayerId: String?,
    firebaseUid: String?,
    isLoading: Boolean = false,
    isGuest: Boolean = false,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
    onUpdate: (
        title: String,
        date: java.util.Date,
        durationMinutes: Int?,
        skillLevel: SkillLevel,
        gameType: CasualGameType,
        preferredAgeGroup: AgeGroup,
        costPerPerson: Double?,
        notes: String?,
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onPlayerClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val isHost = firebaseUid != null && session.hostId == firebaseUid
    val isAttending = currentPlayerId != null && session.attendeeIds.contains(currentPlayerId)
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { ShareUtil.shareSession(context, session) },
                    ) {
                        Icon(Icons.Default.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color(0xFFF2F2F7),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroSection(session = session)
            }

            item {
                SessionInfoCard(session = session)
            }

            if (session.venue.isNotEmpty()) {
                item {
                    VenueCard(session = session)
                }
            }

            if (!isGuest) {
                item {
                    YourStatusCard(
                        session = session,
                        isAttending = isAttending,
                        hasProfile = currentPlayerId != null,
                        isLoading = isLoading,
                        onJoin = onJoin,
                        onLeave = { showLeaveConfirm = true },
                    )
                }
            }

            item {
                AttendeesCard(
                    session = session,
                    attendees = attendees,
                    isLoading = isLoading,
                    onPlayerClick = onPlayerClick,
                )
            }

            if (isHost && session.status == PlaySessionStatus.ACTIVE && !session.isPast) {
                item {
                    HostActionsCard(
                        onEdit = { showEditSheet = true },
                        onFinish = { showFinishConfirm = true },
                        onCancel = { showCancelConfirm = true },
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        EditSessionSheet(
            session = session,
            onSave = { title, date, duration, skill, game, ageGroup, cost, notes ->
                onUpdate(title, date, duration, skill, game, ageGroup, cost, notes)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Session?") },
            text = { Text("Are you sure you want to leave this open play session?") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; onLeave() }) {
                    Text("Leave", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Session?") },
            text = { Text("This will mark the session as cancelled. Attendees will be notified.") },
            confirmButton = {
                TextButton(onClick = { showCancelConfirm = false; onCancel() }) {
                    Text("Cancel Session", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep") }
            },
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Finish Session?") },
            text = { Text("This will mark the session as complete. Players will be prompted to log their calories.") },
            confirmButton = {
                TextButton(onClick = { showFinishConfirm = false; onFinish() }) {
                    Text("Finish", color = SuccessGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HeroSection(session: PlaySession) {
    val heroColor = detailSkillLevelColor(session.skillLevel)
    val (statusText, statusColor) = detailSessionStatusBadge(session)
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(heroColor, heroColor.copy(alpha = 0.3f)),
                ),
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_figure_badminton),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 24.dp)
                .size(100.dp),
            tint = Color.White.copy(alpha = 0.1f),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor,
                ) {
                    Text(
                        text = statusText.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }
                HeroBadge(session.skillLevel.displayName)
                HeroBadge(session.gameType.displayName)
            }

            Text(
                text = session.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    text = buildString {
                        append(dateFormatter.format(session.date))
                        session.formattedDuration?.let { append(" · $it") }
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun HeroBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun SessionInfoCard(session: PlaySession) {
    DetailCard {
        DetailSectionHeader(title = "Details", icon = Icons.Default.Info)

        val tiles = buildList {
            add(Triple(Icons.Default.TrackChanges, "Skill Level", session.skillLevel.displayName) to detailSkillLevelColor(session.skillLevel))
            add(Triple(Icons.Default.Shuffle, "Game Type", session.gameType.displayName) to WarningOrange)
            if (session.preferredAgeGroup != AgeGroup.OPEN) {
                add(Triple(Icons.Default.Person, "Age Group", session.preferredAgeGroup.displayName) to Color(0xFF009688))
            }
            add(Triple(Icons.Default.Group, "Attendees", "${session.attendeeCount} joined") to BrandPurple)
            val costValue = session.formattedCost ?: "Free"
            val costColor = if (session.hasCost) WarningOrange else BrandPurple
            add(Triple(Icons.Default.LocalOffer, "Cost / Person", costValue) to costColor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    rowTiles.forEach { (tile, color) ->
                        InfoTile(
                            icon = tile.first,
                            title = tile.second,
                            value = tile.third,
                            color = color,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowTiles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        session.hostName?.let { hostName ->
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(session.hostAvatar.avatarUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Host",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Hosted by", fontSize = 11.sp, color = Color.Gray)
                    Text(hostName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        session.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notes, null, Modifier.size(14.dp), tint = BrandPurple)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notes,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun VenueCard(session: PlaySession) {
    val context = LocalContext.current

    DetailCard {
        DetailSectionHeader(title = "Location", icon = Icons.Default.LocationOn)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val lat = session.venueLatitude
            val lng = session.venueLongitude
            if (lat != null && lng != null) {
                val latLng = LatLng(lat, lng)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(latLng, 15f)
                }
                GoogleMap(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                    ),
                ) {
                    Marker(state = MarkerState(position = latLng), title = session.venue)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F7)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Map, null, tint = BrandPurple.copy(alpha = 0.5f))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.venue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (session.venueAddress.isNotBlank()) {
                    Text(
                        text = session.venueAddress,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (lat != null && lng != null) {
                    TextButton(
                        onClick = {
                            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${session.venue})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = BrandPurple,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Directions", color = BrandPurple, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun YourStatusCard(
    session: PlaySession,
    isAttending: Boolean,
    hasProfile: Boolean,
    isLoading: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
) {
    DetailCard {
        DetailSectionHeader(title = "Your Status", icon = Icons.Default.PersonAdd)

        when {
            session.status == PlaySessionStatus.CANCELLED -> {
                StatusMessageRow(
                    icon = Icons.Default.Cancel,
                    iconTint = ErrorRed,
                    message = "This session has been cancelled.",
                    background = ErrorRed.copy(alpha = 0.06f),
                )
            }
            session.isPast || session.status == PlaySessionStatus.COMPLETED -> {
                StatusMessageRow(
                    icon = Icons.Default.Schedule,
                    iconTint = Color.Gray,
                    message = "This session has ended.",
                    background = Color(0xFFF2F2F7),
                )
            }
            isAttending -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandPurple.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BrandPurple.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = BrandPurple)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("You're in!", fontWeight = FontWeight.Bold, color = BrandPurple, fontSize = 14.sp)
                        Text("See you on the court 🏸", fontSize = 12.sp, color = Color.Gray)
                        if (session.hasCost) {
                            Text(
                                "Cost: ${session.formattedCost}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }
                    }
                    TextButton(onClick = onLeave) {
                        Text("Leave", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            hasProfile -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (session.hasCost) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarningOrange.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.LocalOffer, null, Modifier.size(14.dp), tint = WarningOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cost per person: ${session.formattedCost}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }
                    }
                    Button(
                        onClick = onJoin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = session.isJoinable && !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_figure_badminton),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join This Session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            else -> {
                StatusMessageRow(
                    icon = Icons.Default.Warning,
                    iconTint = WarningOrange,
                    message = "Your player profile could not be loaded. Try signing out and back in.",
                    background = WarningOrange.copy(alpha = 0.06f),
                )
            }
        }
    }
}

@Composable
private fun AttendeesCard(
    session: PlaySession,
    attendees: List<Player>,
    isLoading: Boolean,
    onPlayerClick: (String) -> Unit,
) {
    DetailCard {
        DetailSectionHeader(
            title = "Players (${session.attendeeCount})",
            icon = Icons.Default.Group,
        )

        if (attendees.isEmpty() && !isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.GroupOff, null, tint = Color.Gray.copy(alpha = 0.5f))
                Text("No players yet", fontSize = 14.sp, color = Color.Gray)
                Text("Be the first to join!", fontSize = 12.sp, color = Color.LightGray)
            }
        } else if (attendees.isEmpty() && isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = BrandPurple,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attendees.forEach { player ->
                    AttendeeRow(
                        player = player,
                        isHost = player.firebaseUid == session.hostId,
                        onClick = { onPlayerClick(player.id.toString().uppercase()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostActionsCard(
    onEdit: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionHeader(title = "Host Actions", icon = Icons.Default.Settings)

        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
        ) {
            Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Session", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
        ) {
            Icon(Icons.Default.Flag, null, Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Finish Session", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ErrorRed.copy(alpha = 0.1f),
                contentColor = ErrorRed,
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
        ) {
            Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel Session", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun InfoTile(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = color)
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusMessageRow(
    icon: ImageVector,
    iconTint: Color,
    message: String,
    background: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = iconTint)
        Text(message, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun AttendeeRow(
    player: Player,
    isHost: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F2F7),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(player.avatar.avatarUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = player.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        player.name.ifEmpty { "Player" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isHost) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = BrandPurple.copy(alpha = 0.15f),
                        ) {
                            Text(
                                "Host",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                            )
                        }
                    }
                }
                Text("Elo: ${player.elo.toInt()}", fontSize = 11.sp, color = Color.Gray)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.LightGray,
            )
        }
    }
}

private fun detailSkillLevelColor(level: SkillLevel): Color = when (level) {
    SkillLevel.ALL_LEVELS -> BrandPurple
    SkillLevel.BEGINNER -> Color(0xFF2196F3)
    SkillLevel.INTERMEDIATE -> WarningOrange
    SkillLevel.ADVANCED -> ErrorRed
}

private fun detailSessionStatusBadge(session: PlaySession): Pair<String, Color> {
    if (session.status == PlaySessionStatus.CANCELLED) return "Cancelled" to ErrorRed
    if (session.status == PlaySessionStatus.COMPLETED || session.isPast) return "Completed" to Color.Gray
    val endTime = session.date.time + (session.durationMinutes ?: 120) * 60_000L
    val now = System.currentTimeMillis()
    if (session.date.time <= now && endTime > now) return "Live" to SuccessGreen
    return "Upcoming" to WarningOrange
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionSheet(
    session: PlaySession,
    onSave: (
        title: String,
        date: Date,
        durationMinutes: Int?,
        skillLevel: SkillLevel,
        gameType: CasualGameType,
        preferredAgeGroup: AgeGroup,
        costPerPerson: Double?,
        notes: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(session.title) }
    var selectedDate by remember { mutableStateOf(session.date) }
    var selectedDuration by remember { mutableIntStateOf(session.durationMinutes ?: 0) }
    var skillLevel by remember { mutableStateOf(session.skillLevel) }
    var gameType by remember { mutableStateOf(session.gameType) }
    var ageGroup by remember { mutableStateOf(session.preferredAgeGroup) }
    var costText by remember {
        mutableStateOf(session.costPerPerson?.let { String.format("%.2f", it) } ?: "")
    }
    var notes by remember { mutableStateOf(session.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F2F7),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text("Cancel", color = BrandPurple, fontSize = 16.sp)
                }
                Text(
                    "Edit Session",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                FormSection("Title") {
                    FormTextField(value = title, onValueChange = { title = it }, placeholder = "Open Play")
                }

                FormSection("Date & Time") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormatter.format(selectedDate), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Surface(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(timeFormatter.format(selectedDate), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                FormSection("Duration") {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(60 to "1 hour", 90 to "1.5 hours", 120 to "2 hours", 180 to "3 hours", 0 to "Open-ended").forEach { (mins, label) ->
                            val isSelected = selectedDuration == mins
                            Surface(
                                onClick = { selectedDuration = mins },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) BrandPurple else Color.White,
                                shadowElevation = if (isSelected) 0.dp else 1.dp,
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color.Black,
                                )
                            }
                        }
                    }
                }

                FormSection("Skill Level") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkillLevel.entries.forEach { level ->
                            val isSelected = skillLevel == level
                            Surface(
                                onClick = { skillLevel = level },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    level.displayName,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                FormSection("Game Type") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CasualGameType.entries.forEach { type ->
                            val isSelected = gameType == type
                            Surface(
                                onClick = { gameType = type },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    type.displayName,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                FormSection("Preferred Age Group") {
                    AgeGroupDropdown(selected = ageGroup, onSelected = { ageGroup = it })
                }

                FormSection("Cost per Person (Optional)") {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("$", color = BrandPurple, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = costText,
                                onValueChange = { costText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                                decorationBox = { inner ->
                                    if (costText.isEmpty()) Text("0.00", color = Color.LightGray, fontSize = 15.sp)
                                    inner()
                                },
                            )
                        }
                    }
                }

                FormSection("Notes (Optional)") {
                    FormTextField(
                        value = notes,
                        onValueChange = { notes = it.take(500) },
                        placeholder = "e.g. Court 3, bring shuttlecocks",
                        singleLine = false,
                        minHeight = 80.dp,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSave(
                            title.trim(),
                            selectedDate,
                            if (selectedDuration == 0) null else selectedDuration,
                            skillLevel,
                            gameType,
                            ageGroup,
                            costText.toDoubleOrNull(),
                            notes.ifBlank { null },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = title.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                        val oldCal = Calendar.getInstance().apply { time = selectedDate }
                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        selectedDate = newCal.time
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply { time = selectedDate }
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    selectedDate = newCal.time
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
