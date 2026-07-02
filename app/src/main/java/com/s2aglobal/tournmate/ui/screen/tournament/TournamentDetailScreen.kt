package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.s2aglobal.tournmate.ui.screen.tournament.visualizer.*
import com.s2aglobal.tournmate.ui.theme.*
import com.s2aglobal.tournmate.util.ShareUtil
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
    var showEditSheet by remember { mutableStateOf(false) }

    if (showEditSheet && state.tournament != null) {
        EditTournamentSheet(
            tournament = state.tournament!!,
            onSave = { title, date, location, locationAddress, locationLatitude, locationLongitude,
                       format, matchFormat, formatConfig, randomPairing, registrationDeadline,
                       entryFee, currency, paymentInfo, prizeInfo, durationMinutes, ageGroup ->
                viewModel.updateTournament(
                    title = title, date = date,
                    location = location, locationAddress = locationAddress,
                    locationLatitude = locationLatitude, locationLongitude = locationLongitude,
                    format = format, matchFormat = matchFormat, formatConfig = formatConfig,
                    randomPairing = randomPairing, registrationDeadline = registrationDeadline,
                    entryFee = entryFee, currency = currency, paymentInfo = paymentInfo,
                    prizeInfo = prizeInfo, durationMinutes = durationMinutes, ageGroup = ageGroup,
                )
                showEditSheet = false
            },
            onCancel = { showEditSheet = false },
        )
        return
    }

    when {
        state.isLoading -> {
            Box(
                Modifier.fillMaxSize().background(Color.White).statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandPurple)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading...", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
        state.tournament == null -> {
            Box(
                Modifier.fillMaxSize().background(Color.White).statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, Modifier.size(48.dp), tint = WarningOrange)
                    Spacer(Modifier.height(16.dp))
                    Text(state.errorMessage ?: "Tournament not found", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
                        Text("Retry")
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) {
                        Text("Go Back", color = BrandPurple)
                    }
                }
            }
        }
        else -> {
            val tournament = state.tournament!!
            val visibleTabs = if (state.isCreator) DetailTab.entries.toList() else DetailTab.entries.filter { it != DetailTab.MANAGE }

            Column(Modifier.fillMaxSize().background(Color.White)) {
                // Purple gradient header
                HeaderSection(tournament, state.isCreator, onBack, onAdminTap = { showEditSheet = true })

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
                                val isDoubles = tournament.format.isDoubles
                                val formedTeams = if (isDoubles) state.registrations.filter { it.isTeamFormed } else state.registrations
                                val awaitingPartner = if (isDoubles) state.registrations.filter { !it.isTeamFormed } else emptyList()

                                item {
                                    Text(
                                        "REGISTERED TEAMS (${formedTeams.size})",
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = Color.Gray, letterSpacing = 1.sp,
                                    )
                                }
                                itemsIndexed(formedTeams, key = { _, reg -> "team_${reg.id}" }) { index, reg ->
                                    TeamRow(reg, index + 1, onClick = { onPlayerClick(reg.playerId) })
                                }

                                if (awaitingPartner.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "AWAITING PARTNER (${awaitingPartner.size})",
                                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            color = Color.Gray, letterSpacing = 1.sp,
                                        )
                                    }
                                    itemsIndexed(awaitingPartner, key = { _, reg -> "solo_${reg.id}" }) { index, reg ->
                                        TeamRow(reg, index + 1, onClick = { onPlayerClick(reg.playerId) })
                                    }
                                }
                            }
                        }
                        DetailTab.MATCHES -> {
                            item {
                                MatchesTabContent(
                                    state = state,
                                    viewModel = viewModel,
                                )
                            }
                        }
                        DetailTab.MANAGE -> {
                            item {
                                ManageTabContent(
                                    state = state,
                                    viewModel = viewModel,
                                )
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
private fun HeaderSection(tournament: Tournament, isCreator: Boolean, onBack: () -> Unit, onAdminTap: () -> Unit = {}) {
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
            .padding(top = 4.dp, bottom = 20.dp),
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
                onClick = { ShareUtil.shareTournament(context, tournament) },
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp), tint = Color.White)
            }

            Spacer(Modifier.width(8.dp))

            if (isCreator) {
                Surface(
                    onClick = onAdminTap,
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
private fun TeamRow(registration: Registration, seedNumber: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Numbered seed circle
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = BrandPurple) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$seedNumber", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            // Team name with partner
            Column(Modifier.weight(1f)) {
                val teamName = buildString {
                    append(registration.player.name)
                    registration.partner?.let { append(" & ${it.name}") }
                }
                Text(teamName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            // PAID badge or Needs Partner
            if (!registration.isTeamFormed && registration.tournament.format.isDoubles) {
                Surface(shape = RoundedCornerShape(6.dp), color = WarningOrange.copy(alpha = 0.1f)) {
                    Text("Needs Partner", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                    Text("PAID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
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

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Matches Tab with Sub-tabs
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private enum class MatchSubTab(val label: String) { DRAW("Draw"), MATCHES("Matches"), STANDINGS("Standings") }

@Composable
private fun MatchesTabContent(state: TournamentDetailUiState, viewModel: TournamentDetailViewModel) {
    var subTab by remember { mutableStateOf(MatchSubTab.DRAW) }
    var matchToScore by remember { mutableStateOf<Match?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MatchSubTab.entries.forEach { tab ->
                val sel = subTab == tab
                Surface(onClick = { subTab = tab }, shape = RoundedCornerShape(50), color = if (sel) BrandPurple else Color(0xFFF2F2F7)) {
                    Text(tab.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            // PDF/Share button
            Surface(
                onClick = {
                    val tournament = state.tournament ?: return@Surface
                    val regMap = state.registrations.associateBy { it.id.toString().uppercase() }
                    val text = buildString {
                        appendLine("${tournament.title} — ${tournament.matchFormat.displayName}")
                        appendLine("${tournament.format.displayName} | ${tournament.location}")
                        appendLine()

                        when (subTab) {
                            MatchSubTab.DRAW -> {
                                appendLine("=== DRAW ===")
                                val hasGroups = state.matches.any { it.groupLabel != null }
                                if (hasGroups) {
                                    state.matches.filter { it.groupLabel != null }.groupBy { it.groupLabel ?: "" }.toSortedMap().forEach { (group, matches) ->
                                        appendLine("Group $group:")
                                        val teamIds = matches.flatMap { listOf(it.teamAId, it.teamBId) }.distinct()
                                        teamIds.forEachIndexed { i, id ->
                                            val name = regMap[id]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "?"
                                            appendLine("  ${group}${i+1}. $name")
                                        }
                                        appendLine()
                                    }
                                }
                            }
                            MatchSubTab.MATCHES -> {
                                appendLine("=== MATCHES ===")
                                state.matches.groupBy { it.groupLabel ?: "Match" }.toSortedMap().forEach { (group, matches) ->
                                    appendLine(if (group == "Match") "" else "Group $group:")
                                    matches.sortedBy { it.round }.forEach { m ->
                                        val a = regMap[m.teamAId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                        val b = regMap[m.teamBId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                        appendLine("  $a ${m.displayScoreLine} $b [${m.status.rawValue}]")
                                    }
                                    appendLine()
                                }
                            }
                            MatchSubTab.STANDINGS -> {
                                appendLine("=== STANDINGS ===")
                                val standings = viewModel.computeRRStandings(state)
                                standings.forEachIndexed { i, e ->
                                    appendLine("${i+1}. ${e.teamName} — ${e.points}pts (W:${e.wins} L:${e.losses})")
                                }
                            }
                        }
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share ${subTab.label}"))
                },
                shape = RoundedCornerShape(50),
                color = Color(0xFFF2F2F7),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.matches.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Text("Matches will appear here once generated.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            when (subTab) {
                MatchSubTab.DRAW -> {
                    val vs = TournamentVisualState.fromTournament(state.matches, state.registrations, state.tournament?.matchFormat ?: MatchFormat.SINGLE_ELIMINATION, state.totalBracketRounds)
                    TournamentFormatVisualizerView(vs) { node -> if (node.match.status == MatchStatus.SCHEDULED || node.match.status == MatchStatus.SCORE_SUBMITTED) matchToScore = node.match }
                }
                MatchSubTab.MATCHES -> {
                    val regMap = state.registrations.associateBy { it.id.toString().uppercase() }
                    val isGroupKnockout = state.tournament?.matchFormat == MatchFormat.GROUP_KNOCKOUT
                    val hasGroups = state.matches.any { it.groupLabel != null }

                    if (isGroupKnockout && hasGroups) {
                        val groupMatches = state.matches.filter { it.groupLabel != null }
                        val knockoutMatches = state.matches.filter { it.groupLabel == null }

                        groupMatches.groupBy { it.groupLabel ?: "" }.toSortedMap().forEach { (groupLabel, gMatches) ->
                            Text("Group $groupLabel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            gMatches.sortedBy { it.round ?: 0 }.forEach { match ->
                                val aName = regMap[match.teamAId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                val bName = regMap[match.teamBId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                MatchCard(MatchNode(match, aName, bName, match.displayScoreLine, match.status == MatchStatus.FINISHED, null),
                                    onTap = { if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.SCORE_SUBMITTED) matchToScore = match },
                                    modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        if (knockoutMatches.isNotEmpty()) {
                            Text("Knockout Stage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                            knockoutMatches.sortedWith(compareBy<Match> { it.round ?: 0 }.thenBy { it.bracketPosition ?: 0 }).forEach { match ->
                                val aName = regMap[match.teamAId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                val bName = regMap[match.teamBId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                MatchCard(MatchNode(match, aName, bName, match.displayScoreLine, match.status == MatchStatus.FINISHED, null),
                                    onTap = { if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.SCORE_SUBMITTED) matchToScore = match },
                                    modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    } else {
                        state.matches.groupBy { it.round ?: 0 }.toSortedMap().forEach { (round, roundMatches) ->
                            Text("Round $round", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(vertical = 8.dp))
                            roundMatches.forEach { match ->
                                val aName = regMap[match.teamAId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                val bName = regMap[match.teamBId]?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "TBD"
                                MatchCard(MatchNode(match, aName, bName, match.displayScoreLine, match.status == MatchStatus.FINISHED, null),
                                    onTap = { if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.SCORE_SUBMITTED) matchToScore = match },
                                    modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
                MatchSubTab.STANDINGS -> {
                    val tournament = state.tournament ?: return
                    when (tournament.matchFormat) {
                        MatchFormat.ROUND_ROBIN, MatchFormat.MANUAL_DRAW, MatchFormat.SWISS -> StandingsTable(viewModel.computeRRStandings(state))
                        MatchFormat.GROUP_KNOCKOUT -> {
                            val gs = viewModel.computeGroupStandings(state)
                            gs.keys.sorted().forEach { g ->
                                Text("Group $g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(vertical = 8.dp))
                                StandingsTable(gs[g] ?: emptyList())
                            }
                        }
                        else -> Text("Bracket progress shown in the Draw tab.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    matchToScore?.let { match ->
        val isCreator = state.isCreator
        val useSets = state.tournament?.sportType?.usesSetScoring == true && state.tournament.matchFormat != MatchFormat.ROUND_ROBIN
        if (useSets) {
            SetScoreEntryScreen(match = match, isCreator = isCreator, onSubmit = { sets ->
                val uid = state.firebaseUid ?: ""
                if (isCreator) { viewModel.submitSetScores(match, sets, uid); viewModel.confirmScore(match, uid) } else viewModel.submitSetScores(match, sets, uid)
                matchToScore = null
            }, onDismiss = { matchToScore = null })
        } else {
            SimpleScoreEntryScreen(match = match, isCreator = isCreator, onSubmit = { a, b ->
                if (isCreator) viewModel.recordScore(match, a, b) else { val s = SetScore(a, b); viewModel.submitSetScores(match, listOf(s), state.firebaseUid ?: "") }
                matchToScore = null
            }, onDismiss = { matchToScore = null })
        }
    }
}

@Composable
private fun StandingsTable(standings: List<StandingsEntry>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(20.dp))
                Text("Team", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                Text("P", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp))
                Text("W", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp))
                Text("L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp))
                Text("Pts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.width(30.dp))
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color(0xFFF2F2F7))
            standings.forEachIndexed { i, e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Text(e.teamName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${e.played}", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                    Text("${e.wins}", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                    Text("${e.losses}", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                    Text("${e.points}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.width(30.dp))
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Manage Tab
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ManageTabContent(state: TournamentDetailUiState, viewModel: TournamentDetailViewModel) {
    val tournament = state.tournament ?: return
    var showResetAlert by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    var manualTeamA by remember { mutableStateOf<Registration?>(null) }
    var manualTeamB by remember { mutableStateOf<Registration?>(null) }

    if (showResetAlert) {
        AlertDialog(onDismissRequest = { showResetAlert = false }, title = { Text("Reset All Matches?") },
            text = { Text("This will delete all match results, scores, and bracket progress. This cannot be undone.") },
            dismissButton = { TextButton(onClick = { showResetAlert = false }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = { viewModel.resetMatches(); showResetAlert = false }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Reset") } })
    }
    if (showCancelAlert) {
        AlertDialog(onDismissRequest = { showCancelAlert = false }, title = { Text("Cancel Tournament?") }, text = { Text("This action cannot be undone.") },
            dismissButton = { TextButton(onClick = { showCancelAlert = false }) { Text("Keep") } },
            confirmButton = { TextButton(onClick = { viewModel.cancelTournament(); showCancelAlert = false }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Cancel Tournament") } })
    }
    if (showDeleteAlert) {
        AlertDialog(onDismissRequest = { showDeleteAlert = false }, title = { Text("Delete Tournament?") }, text = { Text("This will permanently delete the tournament and all data.") },
            dismissButton = { TextButton(onClick = { showDeleteAlert = false }) { Text("Keep") } },
            confirmButton = { TextButton(onClick = { viewModel.deleteTournament(); showDeleteAlert = false }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Delete") } })
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (state.isTournamentComplete) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SuccessGreen.copy(alpha = 0.1f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(24.dp), tint = SuccessGreen)
                    Text("Tournament Complete!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }
        }

        state.statusMessage?.let { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = BrandPurple.copy(alpha = 0.08f)) { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(14.dp)) } }

        if (tournament.format.isDoubles && state.registrations.count { it.partnerId == null } >= 2) {
            AdminActionButton("RANDOM PAIRINGS", Icons.Default.Shuffle, state.isLoading) { viewModel.generateRandomPairs() }
        }

        if (state.matches.isEmpty() && !state.isTournamentComplete) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.1f)))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp), tint = BrandPurple)
                        Text("BRACKET GENERATION", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Ready to start? This will generate matches based on seeded teams.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    if (tournament.matchFormat != MatchFormat.MANUAL_DRAW) {
                        val (t, ic) = when (tournament.matchFormat) {
                            MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> "GENERATE BRACKET" to Icons.Default.AccountTree
                            MatchFormat.ROUND_ROBIN -> "GENERATE ROUND ROBIN" to Icons.Default.Loop
                            MatchFormat.GROUP_KNOCKOUT -> "GENERATE GROUPS" to Icons.Default.ViewModule
                            MatchFormat.SWISS -> "GENERATE SWISS ROUND 1" to Icons.Default.FormatListNumbered
                            else -> "" to Icons.Default.Add
                        }
                        AdminActionButton(t, ic, state.isLoading) {
                            when (tournament.matchFormat) {
                                MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> viewModel.generateBracket()
                                MatchFormat.ROUND_ROBIN -> viewModel.generateRoundRobinMatches()
                                MatchFormat.GROUP_KNOCKOUT -> viewModel.generateGroupKnockoutMatches()
                                MatchFormat.SWISS -> viewModel.generateNextSwissRound()
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        if (tournament.matchFormat == MatchFormat.MANUAL_DRAW) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.padding(20.dp)) {
                    Text("MANUAL DRAW", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Team A", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    ManualTeamPicker(state.formedTeams, manualTeamA) { manualTeamA = it }
                    Spacer(Modifier.height(8.dp))
                    Text("Team B", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    ManualTeamPicker(state.formedTeams, manualTeamB) { manualTeamB = it }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { val a = manualTeamA!!; val b = manualTeamB!!; viewModel.createManualMatch(a, b, 1); manualTeamA = null; manualTeamB = null },
                        enabled = manualTeamA != null && manualTeamB != null && manualTeamA != manualTeamB,
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) { Text("CREATE MATCH", fontWeight = FontWeight.Bold) }
                }
            }
        }

        if (state.matches.isNotEmpty() && state.currentRoundFullyFinished && !state.isTournamentComplete) {
            when (tournament.matchFormat) {
                MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> AdminActionButton("ADVANCE TO NEXT ROUND", Icons.Default.ArrowForward, state.isLoading) { viewModel.advanceToNextRound() }
                MatchFormat.GROUP_KNOCKOUT -> {
                    if (state.isGroupStageComplete && !state.hasKnockoutMatches) AdminActionButton("ADVANCE TO KNOCKOUT", Icons.Default.ArrowForward, state.isLoading) { viewModel.advanceGroupToKnockout() }
                    else if (state.hasKnockoutMatches) AdminActionButton("ADVANCE TO NEXT ROUND", Icons.Default.ArrowForward, state.isLoading) { viewModel.advanceToNextRound() }
                }
                MatchFormat.SWISS -> { if (state.isCurrentSwissRoundComplete && !state.isSwissComplete) AdminActionButton("GENERATE SWISS ROUND ${state.currentSwissRound + 1}", Icons.Default.Add, state.isLoading) { viewModel.generateNextSwissRound() } }
                else -> {}
            }
        }

        if (state.matches.isNotEmpty()) {
            Surface(onClick = { showResetAlert = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = ErrorRed.copy(alpha = 0.05f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = ErrorRed); Spacer(Modifier.width(8.dp))
                    Text("RESET ALL MATCHES", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                }
            }
        }

        Text("DANGER ZONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        if (tournament.canCancelOrDelete) {
            Surface(onClick = { showCancelAlert = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Cancel, null, Modifier.size(18.dp), tint = ErrorRed)
                    Column { Text("Cancel Tournament", fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text("Mark as cancelled", fontSize = 11.sp, color = Color.Gray) }
                }
            }
            Surface(onClick = { showDeleteAlert = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = ErrorRed)
                    Column { Text("Delete Tournament", fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text("Permanently remove", fontSize = 11.sp, color = Color.Gray) }
                }
            }
        } else {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.Gray.copy(alpha = 0.05f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lock, null, Modifier.size(18.dp), tint = Color.Gray)
                    Column { Text("Cannot Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray); Text("Must be 3+ hours before start.", fontSize = 11.sp, color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
private fun AdminActionButton(title: String, icon: ImageVector, isLoading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
        if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTeamPicker(teams: List<Registration>, selected: Registration?, onSelect: (Registration) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(onClick = { expanded = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFFF2F2F7)) {
        Text(selected?.let { listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ") } ?: "Select team",
            fontSize = 14.sp, modifier = Modifier.padding(14.dp), color = if (selected != null) Color.Black else Color.Gray)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        teams.forEach { team -> DropdownMenuItem(text = { Text(listOfNotNull(team.player.name, team.partner?.name).joinToString(" & ")) }, onClick = { onSelect(team); expanded = false }) }
    }
}
