package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.theme.*
import com.s2aglobal.tournmate.util.ShareUtil
import java.util.Calendar

private enum class DetailTab(val label: String) {
    INFO("Info"),
    TEAMS("Teams"),
    MATCHES("Matches"),
    MANAGE("Manage"),
}

/** Which score sheet is open, mirroring the iOS sheet items. */
private sealed interface ScoreSheet {
    val match: Match
    data class Simple(override val match: Match) : ScoreSheet
    data class EditSimple(override val match: Match) : ScoreSheet
    data class Sets(override val match: Match) : ScoreSheet
    data class Dispute(override val match: Match) : ScoreSheet
    data class EditSets(override val match: Match) : ScoreSheet
}

@Composable
fun TournamentDetailScreen(
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    isGuest: Boolean = false,
    viewModel: TournamentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(DetailTab.INFO) }
    var showEditSheet by remember { mutableStateOf(false) }

    var showDobPrompt by remember { mutableStateOf(false) }
    var showPartnerSheet by remember { mutableStateOf(false) }
    var selectedPartner by remember { mutableStateOf<Player?>(null) }
    var showWithdrawConfirm by remember { mutableStateOf(false) }
    var partnerToPick by remember { mutableStateOf<Player?>(null) }
    var scoreSheet by remember { mutableStateOf<ScoreSheet?>(null) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showResetAlert by remember { mutableStateOf(false) }

    LaunchedEffect(state.didDelete) { if (state.didDelete) onBack() }

    // Close the partner sheet once the registration request finishes (iOS awaits, then dismisses).
    var awaitingRegistration by remember { mutableStateOf(false) }
    LaunchedEffect(state.isRegistering) {
        if (!state.isRegistering && awaitingRegistration) {
            awaitingRegistration = false
            showPartnerSheet = false
        }
    }

    val tournament = state.tournament
    if (tournament == null) {
        Box(Modifier.fillMaxSize().background(Color.White).statusBarsPadding(), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppAccent)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading…", fontSize = 13.sp, color = Color.Gray)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, Modifier.size(48.dp), tint = WarningOrange)
                    Spacer(Modifier.height(16.dp))
                    Text(state.errorMessage ?: "Tournament not found", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) { Text("Retry") }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("Go Back", color = AppAccent) }
                }
            }
        }
        return
    }

    if (showEditSheet) {
        EditTournamentSheet(
            tournament = tournament,
            onSave = { title, date, location, locationAddress, locationLatitude, locationLongitude,
                       format, matchFormat, formatConfig, randomPairing, registrationDeadline,
                       entryFee, currency, paymentInfo, prizeInfo, durationMinutes, ageGroup, scoringConfig ->
                viewModel.updateTournament(
                    title = title, date = date,
                    location = location, locationAddress = locationAddress,
                    locationLatitude = locationLatitude, locationLongitude = locationLongitude,
                    format = format, matchFormat = matchFormat, formatConfig = formatConfig,
                    randomPairing = randomPairing, registrationDeadline = registrationDeadline,
                    entryFee = entryFee, currency = currency, paymentInfo = paymentInfo,
                    prizeInfo = prizeInfo, durationMinutes = durationMinutes, ageGroup = ageGroup,
                    scoringConfig = scoringConfig,
                )
                showEditSheet = false
            },
            onCancel = { showEditSheet = false },
            canEditScoring = state.matches.isEmpty(),
        )
        return
    }

    val uid = state.firebaseUid ?: ""
    val actions = MatchActions(
        isCreator = state.isCreator,
        uid = state.firebaseUid,
        onEnterSetScore = { scoreSheet = ScoreSheet.Sets(it) },
        onEnterSimpleScore = { scoreSheet = ScoreSheet.Simple(it) },
        onConfirm = { viewModel.confirmScore(it, uid) },
        onDispute = { viewModel.disputeScore(it, uid) },
        onResolveDispute = { scoreSheet = ScoreSheet.Dispute(it) },
        onEditSetScore = { scoreSheet = ScoreSheet.EditSets(it) },
        onEditSimpleScore = { scoreSheet = ScoreSheet.EditSimple(it) },
    )

    fun handleRegisterTap() {
        val needsDob = tournament.ageGroup != AgeGroup.OPEN && state.currentPlayer?.dateOfBirth == null
        when {
            needsDob -> showDobPrompt = true
            state.needsPartnerPick -> { selectedPartner = null; showPartnerSheet = true }
            else -> viewModel.register()
        }
    }

    val visibleTabs = DetailTab.entries.filter { it != DetailTab.MANAGE || state.isCreator }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        HeaderSection(tournament, state.isCreator, onBack, onAdminTap = { showEditSheet = true })

        TabPicker(activeTab, visibleTabs) { activeTab = it }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = if (activeTab == DetailTab.MANAGE) 40.dp else 20.dp),
        ) {
            when (activeTab) {
                DetailTab.INFO -> InfoTabContent(
                    state = state,
                    isGuest = isGuest,
                    onWithdraw = { showWithdrawConfirm = true },
                    onCalorieWeightChange = viewModel::setCalorieWeight,
                    onLogCalories = viewModel::logTournamentCalories,
                )
                DetailTab.TEAMS -> TeamsTabContent(
                    state = state,
                    onPlayerClick = onPlayerClick,
                    onPartnerUp = { partnerToPick = it },
                    onRegisterWithPartner = { selectedPartner = it; showPartnerSheet = true },
                )
                DetailTab.MATCHES -> MatchesTabContent(state, viewModel, actions)
                DetailTab.MANAGE -> ManageTabContent(
                    state = state,
                    viewModel = viewModel,
                    onResetMatches = { showResetAlert = true },
                    onCancelTournament = { showCancelAlert = true },
                    onDeleteTournament = { showDeleteAlert = true },
                )
            }
        }

        if (activeTab != DetailTab.MANAGE) {
            StickyFooter(
                tournament = tournament,
                isRegistered = state.isRegistered,
                isGuest = isGuest || state.currentPlayer == null,
                isBusy = state.isRegistering,
                onRegister = ::handleRegisterTap,
            )
        }
    }

    // ── Sheets ──────────────────────────────────────

    if (showDobPrompt) {
        DobPromptSheet(
            ageGroup = tournament.ageGroup,
            onConfirm = { dob ->
                showDobPrompt = false
                val pickPartner = state.needsPartnerPick
                viewModel.saveDateOfBirthAndRegister(dob, partner = null, continueToPartnerPick = pickPartner)
                if (pickPartner) { selectedPartner = null; showPartnerSheet = true }
            },
            onCancel = { showDobPrompt = false },
        )
    }

    if (showPartnerSheet) {
        SelectPartnerSheet(
            availablePartners = state.availablePartners,
            initialPartner = selectedPartner,
            isRegistering = state.isRegistering,
            onRegister = { partner -> awaitingRegistration = true; viewModel.register(partner) },
            onCancel = { showPartnerSheet = false },
        )
    }

    scoreSheet?.let { sheet ->
        val match = sheet.match
        val close = { scoreSheet = null }
        when (sheet) {
            is ScoreSheet.Sets -> SetScoreEntryScreen(
                match = match, isCreator = state.isCreator,
                onSubmit = { sets -> viewModel.submitSetScores(match, sets, uid, autoConfirm = state.isCreator) },
                onDismiss = close,
            )
            is ScoreSheet.Dispute -> SetScoreEntryScreen(
                match = match, isCreator = state.isCreator, existingScores = match.setScores,
                onSubmit = { sets -> viewModel.resolveDispute(match, sets) },
                onDismiss = close,
            )
            is ScoreSheet.EditSets -> SetScoreEntryScreen(
                match = match, isCreator = true, existingScores = match.setScores,
                onSubmit = { sets -> viewModel.resolveDispute(match, sets) },
                onDismiss = close,
            )
            is ScoreSheet.Simple -> SimpleScoreEntryScreen(
                match = match, isCreator = state.isCreator,
                onSubmit = { a, b ->
                    if (state.isCreator) viewModel.recordScore(match, a, b)
                    else viewModel.submitSetScores(match, listOf(SetScore(a, b)), uid, autoConfirm = false)
                },
                onDismiss = close,
            )
            is ScoreSheet.EditSimple -> SimpleScoreEntryScreen(
                match = match, isCreator = true,
                onSubmit = { a, b -> viewModel.recordScore(match, a, b) },
                onDismiss = close,
            )
        }
    }

    // ── Dialogs ─────────────────────────────────────

    if (showWithdrawConfirm) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirm = false },
            title = { Text("Withdraw from Tournament?") },
            text = {
                Text(
                    if (tournament.format.isDoubles) "You will be removed from this tournament. If you have a partner, your team will be dissolved."
                    else "You will be removed from this tournament. This cannot be undone."
                )
            },
            dismissButton = { TextButton(onClick = { showWithdrawConfirm = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(
                    onClick = { showWithdrawConfirm = false; viewModel.withdraw() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) { Text("Withdraw") }
            },
        )
    }

    partnerToPick?.let { player ->
        val teammate = state.currentTeammate
        val switching = state.hasExistingPartner
        AlertDialog(
            onDismissRequest = { partnerToPick = null },
            title = { Text(if (switching) "Switch Partner?" else "Partner Up?") },
            text = {
                Text(
                    if (switching && teammate != null)
                        "You'll leave ${teammate.name} and partner with ${player.name}. ${teammate.name} will be moved back to the solo list."
                    else "Team up with ${player.name} for this tournament?"
                )
            },
            dismissButton = { TextButton(onClick = { partnerToPick = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { viewModel.pickPartnerFromTeams(player); partnerToPick = null }) {
                    Text(if (switching) "Switch" else "Confirm", color = AppAccent)
                }
            },
        )
    }

    if (showCancelAlert) {
        val fee = tournament.formattedFee
        AlertDialog(
            onDismissRequest = { showCancelAlert = false },
            title = { Text("Cancel Tournament?") },
            text = {
                Text(
                    if (fee != null) "This will cancel \"${tournament.title}\".\n\nRegistered players paid $fee each. Please arrange refunds for all ${state.registrations.size} registered player(s) before confirming.\n\n⚠️ This action cannot be undone."
                    else "This will cancel \"${tournament.title}\". All registered players will see this tournament as cancelled.\n\nThis action cannot be undone."
                )
            },
            dismissButton = { TextButton(onClick = { showCancelAlert = false }) { Text("Keep Tournament") } },
            confirmButton = {
                TextButton(
                    onClick = { showCancelAlert = false; viewModel.cancelTournament() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) { Text("Cancel Tournament") }
            },
        )
    }

    if (showResetAlert) {
        AlertDialog(
            onDismissRequest = { showResetAlert = false },
            title = { Text("Reset All Matches?") },
            text = { Text("This will delete all match results, scores, and bracket progress. Teams will remain paired but you'll need to regenerate the bracket.\n\nThis cannot be undone.") },
            dismissButton = { TextButton(onClick = { showResetAlert = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(
                    onClick = { showResetAlert = false; viewModel.resetMatches() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) { Text("Reset") }
            },
        )
    }

    if (showDeleteAlert) {
        val fee = tournament.formattedFee
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Delete Tournament?") },
            text = {
                Text(
                    if (fee != null) "This will permanently delete \"${tournament.title}\" and all its data.\n\n⚠️ ${state.registrations.size} player(s) are registered and paid $fee each. Ensure all refunds have been processed.\n\nThis cannot be undone."
                    else "This will permanently delete \"${tournament.title}\" along with all registrations, matches, and data.\n\nThis cannot be undone."
                )
            },
            dismissButton = { TextButton(onClick = { showDeleteAlert = false }) { Text("Keep") } },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteAlert = false; viewModel.deleteTournament() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) { Text("Delete Permanently") }
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Header
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun badgeStyle(tournament: Tournament): Pair<String, Color> {
    if (tournament.status == TournamentStatus.CANCELLED) return "CANCELLED" to Color.Red
    val now = Calendar.getInstance()
    val day = Calendar.getInstance().apply { time = tournament.date }
    val sameDay = now.get(Calendar.YEAR) == day.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    return when {
        tournament.isPast -> "COMPLETED" to Color.Gray
        sameDay || tournament.isRegistrationClosed -> "LIVE" to Color(0xFF34C759)
        else -> "UPCOMING" to Color(0xFFFF9500)
    }
}

@Composable
private fun HeaderCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun HeaderPill(text: String, background: Color, borderColor: Color?) {
    Text(
        text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp)) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun HeaderSection(tournament: Tournament, isCreator: Boolean, onBack: () -> Unit, onAdminTap: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (statusText, statusColor) = badgeStyle(tournament)
    val green = Color(0xFF34C759)

    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(AppAccent, AppAccentDeep)))
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderCircleButton(onBack) { Icon(Icons.Default.ChevronLeft, "Back", Modifier.size(24.dp), tint = Color.White) }
            Spacer(Modifier.weight(1f))
            HeaderCircleButton({ ShareUtil.shareTournament(context, tournament) }) {
                Icon(Icons.Default.Share, "Share", Modifier.size(17.dp), tint = Color.White)
            }
            if (isCreator) {
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(green.copy(alpha = 0.2f))
                        .border(1.dp, green.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onAdminTap).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Settings, null, Modifier.size(12.dp), tint = green)
                    Text("ADMIN MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = green)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderPill(statusText, statusColor, null)
            HeaderPill(tournament.format.displayName.uppercase(), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.2f))
            if (tournament.ageGroup != AgeGroup.OPEN) {
                HeaderPill(tournament.ageGroup.ageShortLabel.uppercase(), Color(0xFFFF9500).copy(alpha = 0.2f), Color(0xFFFF9500).copy(alpha = 0.4f))
            }
        }

        Text(
            tournament.title,
            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3, lineHeight = 38.sp,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp),
        )

        if (tournament.location.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 24.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.PinDrop, null, Modifier.size(14.dp), tint = Color.Gray)
                Text(tournament.location, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Tab Picker
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TabPicker(active: DetailTab, tabs: List<DetailTab>, onSelect: (DetailTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = 20.dp, bottom = 4.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        tabs.forEach { tab ->
            val isActive = active == tab
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(tab) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    tab.label.uppercase(),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    color = if (isActive) Color.Black else Color.Gray.copy(alpha = 0.5f),
                )
                Box(
                    Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) AppAccent else Color.Transparent),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Sticky Footer
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun FooterPill(background: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(background),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun StickyFooter(
    tournament: Tournament,
    isRegistered: Boolean,
    isGuest: Boolean,
    isBusy: Boolean,
    onRegister: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(Color.White)
            .navigationBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("ENTRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(tournament.formattedFee ?: "Free", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.weight(1f)) {
            val dimmed = Color.White.copy(alpha = 0.6f)
            when {
                // Cancelled wins over every other state, including registered (matches iOS).
                tournament.status == TournamentStatus.CANCELLED -> FooterPill(Color.Gray) {
                    Text("CANCELLED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = dimmed)
                }
                isRegistered -> FooterPill(AppAccent) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("REGISTERED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                tournament.isRegistrationClosed || isGuest -> FooterPill(Color.Gray) {
                    Text(if (isGuest) "SIGN IN TO REGISTER" else "REGISTRATION CLOSED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = dimmed)
                    if (!isGuest) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = dimmed)
                    }
                }
                else -> Button(
                    onClick = onRegister,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent, disabledContainerColor = AppAccent),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("REGISTER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}
