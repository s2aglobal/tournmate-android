package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

private enum class DetailTab(val label: String) {
    INFO("Info"),
    TEAMS("Teams"),
    MATCHES("Matches"),
    MANAGE("Manage"),
}

@Composable
fun TournamentDetailScreen(
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    viewModel: TournamentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(DetailTab.INFO) }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPurple)
            }
        }
        state.tournament == null -> {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, Modifier.size(48.dp), tint = WarningOrange)
                    Spacer(Modifier.height(16.dp))
                    Text(state.errorMessage ?: "Tournament not found", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            val tournament = state.tournament!!
            val visibleTabs = if (state.isCreator) DetailTab.entries else DetailTab.entries.filter { it != DetailTab.MANAGE }

            Column(Modifier.fillMaxSize().background(Color.White)) {
                // Purple gradient header
                HeaderSection(tournament, state.isCreator, onBack)

                // Tab picker
                TabPicker(activeTab, visibleTabs) { activeTab = it }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (activeTab) {
                        DetailTab.INFO -> {
                            item { CoachsCornerCard() }
                            item { InfoGrid(tournament) }
                            tournament.prizeInfo?.takeIf { it.isNotEmpty() }?.let { prize ->
                                item { PrizeCard(prize) }
                            }
                            item { FeeCard(tournament) }
                            if (tournament.location.isNotEmpty()) {
                                item { LocationRow(tournament) }
                            }
                            item { DeadlineRow(tournament) }
                            if (state.isRegistered) {
                                item { RegisteredStatusCard(state.currentPlayerRegistration) }
                            }
                            state.organizerName?.let { name ->
                                item { OrganizerRow(name) }
                            }
                        }
                        DetailTab.TEAMS -> {
                            if (state.registrations.isEmpty()) {
                                item { EmptyTeamsState() }
                            } else {
                                items(state.registrations, key = { it.id }) { reg ->
                                    TeamRow(reg, onClick = { onPlayerClick(reg.playerId) })
                                }
                            }
                        }
                        DetailTab.MATCHES -> {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("Matches will appear here once generated.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                        DetailTab.MANAGE -> {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("Admin controls coming in Phase 3.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Sticky registration footer
                if (activeTab != DetailTab.MANAGE) {
                    StickyFooter(
                        tournament = tournament,
                        isRegistered = state.isRegistered,
                        canRegister = state.canRegister,
                        isRegistering = state.isRegistering,
                        onRegister = { viewModel.register() },
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Purple Gradient Header
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HeaderSection(tournament: Tournament, isCreator: Boolean, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (statusText, statusColor) = when {
        tournament.status == TournamentStatus.CANCELLED -> "CANCELLED" to ErrorRed
        tournament.isPast -> "COMPLETED" to Color.Gray
        tournament.isRegistrationClosed -> "LIVE" to SuccessGreen
        else -> "UPCOMING" to WarningOrange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(BrandPurple, BrandPurpleDark))
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        // Navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(Icons.Default.ChevronLeft, null, Modifier.size(20.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))

            // Share button
            IconButton(
                onClick = {
                    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val shareText = buildString {
                        append("${tournament.title}\n")
                        append("${dateFmt.format(tournament.date)} • ${tournament.format.displayName}\n")
                        if (tournament.location.isNotEmpty()) append("📍 ${tournament.location}\n")
                        append("\nJoin on TournMate!")
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Tournament"))
                },
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp), tint = Color.White)
            }

            Spacer(Modifier.width(8.dp))

            if (isCreator) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SuccessGreen.copy(alpha = 0.2f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen.copy(alpha = 0.5f)), width = 1.dp,
                    ),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Settings, null, Modifier.size(12.dp), tint = SuccessGreen)
                        Text("ADMIN MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Status + format badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = statusColor) {
                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)), width = 1.dp)) {
                Text(tournament.format.displayName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }

        // Title
        Text(
            tournament.title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 3,
            modifier = Modifier.padding(top = 8.dp),
        )

        // Location
        if (tournament.location.isNotEmpty()) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = Color.Gray)
                Text(tournament.location, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Tab Picker (underline style)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TabPicker(active: DetailTab, tabs: List<DetailTab>, onSelect: (DetailTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp).padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        tabs.forEach { tab ->
            val isActive = active == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.label.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isActive) Color.Black else Color.Gray.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(40.dp)
                        .background(
                            if (isActive) BrandPurple else Color.Transparent,
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Coach's Corner
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun CoachsCornerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF5C6BC0),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Bolt, null, Modifier.size(16.dp), tint = Color.Yellow)
                Text("COACH'S CORNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "\"Stay focused on your footwork and shuttle placement. Consistency wins matches!\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                lineHeight = 20.sp,
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Info Grid
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun InfoGrid(tournament: Tournament) {
    val dateFmt = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoGridCard(Icons.Default.CalendarToday, "DATE", dateFmt.format(tournament.date), Modifier.weight(1f))
            InfoGridCard(Icons.Default.Schedule, "START TIME", timeFmt.format(tournament.date), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoGridCard(Icons.Default.EmojiEvents, "FORMAT", tournament.matchFormat.displayName, Modifier.weight(1f))
            InfoGridCard(Icons.Default.SportsTennis, "EVENT TYPE", tournament.format.displayName, Modifier.weight(1f))
        }
        if (tournament.ageGroup != AgeGroup.OPEN || tournament.formattedDuration != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (tournament.ageGroup != AgeGroup.OPEN) {
                    InfoGridCard(Icons.Default.Person, "AGE GROUP", tournament.ageGroup.displayName, Modifier.weight(1f))
                }
                tournament.formattedDuration?.let {
                    InfoGridCard(Icons.Default.Timer, "DURATION", it, Modifier.weight(1f))
                }
                if (tournament.ageGroup == AgeGroup.OPEN || tournament.formattedDuration == null) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoGridCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Gray.copy(alpha = 0.05f),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.1f)), width = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = BrandPurple)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Prize + Fee Cards
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun PrizeCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PrizeGold.copy(alpha = 0.08f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏆", fontSize = 24.sp)
            Column {
                Text("PRIZES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrizeGold)
            }
        }
    }
}

@Composable
private fun FeeCard(tournament: Tournament) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Gray.copy(alpha = 0.05f),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.1f)), width = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                tournament.formattedFee ?: "Free",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (tournament.isFree) BrandPurple else Color.Black,
            )
            tournament.paymentInfo?.takeIf { it.isNotEmpty() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Location + Deadline Rows
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun LocationRow(tournament: Tournament) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Color.Gray.copy(alpha = 0.05f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
        Column {
            Text(tournament.location, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (tournament.locationAddress.isNotEmpty()) {
                Text(tournament.locationAddress, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun DeadlineRow(tournament: Tournament) {
    val fmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val deadlineText = fmt.format(tournament.effectiveDeadline)
    val isExpired = tournament.isRegistrationClosed

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Color.Gray.copy(alpha = 0.05f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Schedule, null, Modifier.size(20.dp), tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
        Column {
            Text("Registration Deadline", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isExpired) "Closed — $deadlineText" else deadlineText,
                fontSize = 12.sp,
                color = if (isExpired) WarningOrange else Color.Gray,
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Registration Status Card
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun RegisteredStatusCard(registration: Registration?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SuccessGreen.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(24.dp), tint = SuccessGreen)
            Column {
                Text("You're registered!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                registration?.partner?.let { partner ->
                    Text("Partner: ${partner.name}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Organizer
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun OrganizerRow(name: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Gray.copy(alpha = 0.05f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VerifiedUser, null, Modifier.size(20.dp), tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
        Column {
            Text("Organized by", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Teams Tab
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun EmptyTeamsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.People, null, Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))
        Text("No registrations yet", color = Color.Gray, fontSize = 14.sp)
        Text("Be the first to register!", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun TeamRow(registration: Registration, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Gray.copy(alpha = 0.05f),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val avatarUrl = PlayerAvatar.fromId(registration.player.avatarId).avatarUrl()
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFFF2F2F7)) {
                AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(44.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(registration.player.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                registration.partner?.let {
                    Text("with ${it.name}", fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (!registration.isTeamFormed && registration.tournament.format.isDoubles) {
                Surface(shape = RoundedCornerShape(6.dp), color = WarningOrange.copy(alpha = 0.1f)) {
                    Text("Needs Partner", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Sticky Footer
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun StickyFooter(
    tournament: Tournament,
    isRegistered: Boolean,
    canRegister: Boolean,
    isRegistering: Boolean,
    onRegister: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Entry fee
            Column(Modifier.padding(start = 8.dp)) {
                Text("ENTRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(tournament.formattedFee ?: "Free", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Register button
            when {
                isRegistered -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("REGISTERED", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                canRegister -> {
                    Button(
                        onClick = onRegister,
                        enabled = !isRegistering,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("REGISTER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                        }
                    }
                }
                tournament.isRegistrationClosed -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, disabledContainerColor = Color.Gray),
                    ) {
                        Text("REGISTRATION CLOSED", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.6f))
                    }
                }
                else -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, disabledContainerColor = Color.Gray),
                    ) {
                        Text("CANCELLED", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

