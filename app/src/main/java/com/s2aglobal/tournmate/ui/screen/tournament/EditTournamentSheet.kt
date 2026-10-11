package com.s2aglobal.tournmate.ui.screen.tournament

import com.s2aglobal.tournmate.ui.component.FullScreenCover
import com.s2aglobal.tournmate.ui.component.PrimaryCapsuleButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.component.CricketEventTypePicker
import com.s2aglobal.tournmate.ui.component.CricketRulesEditor
import com.s2aglobal.tournmate.ui.component.FormatRulesEditor
import com.s2aglobal.tournmate.ui.component.ScoringConfigEditor
import com.s2aglobal.tournmate.ui.component.SkillDivisionPicker
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.util.DatePickerUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTournamentSheet(
    tournament: Tournament,
    onSave: (
        title: String, date: Date,
        location: String, locationAddress: String,
        locationLatitude: Double?, locationLongitude: Double?,
        format: TournamentFormat, matchFormat: MatchFormat,
        formatConfig: FormatConfig?,
        randomPairing: Boolean,
        registrationDeadline: Date,
        entryFee: Double?, currency: String, paymentInfo: String?,
        prizeInfo: String?, durationMinutes: Int?, ageGroup: AgeGroup,
        scoringConfig: ScoringConfig?, skillDivision: String?,
        cricketRules: CricketRules?,
    ) -> Unit,
    onCancel: () -> Unit,
    /** Match rules (cricket) and scoring are locked once matches exist so results stay consistent. */
    canEditMatchRules: Boolean = true,
) {
    val focusManager = LocalFocusManager.current

    var editTitle by remember { mutableStateOf(tournament.title) }
    var editFormat by remember { mutableStateOf(tournament.format) }
    var editMatchFormat by remember { mutableStateOf(tournament.matchFormat) }
    var editRandomPairing by remember { mutableStateOf(tournament.randomPairing) }
    var editDate by remember { mutableStateOf(tournament.date) }
    var editDeadline by remember {
        mutableStateOf(tournament.registrationDeadline ?: tournament.date)
    }
    var editVenueName by remember { mutableStateOf(tournament.location) }
    var editVenueAddress by remember { mutableStateOf(tournament.locationAddress) }
    var editVenueLatitude by remember { mutableStateOf(tournament.locationLatitude) }
    var editVenueLongitude by remember { mutableStateOf(tournament.locationLongitude) }
    var editEntryFee by remember {
        mutableStateOf(tournament.entryFee?.let { if (it > 0) it.toBigDecimal().stripTrailingZeros().toPlainString() else "" } ?: "")
    }
    var editCurrency by remember { mutableStateOf(tournament.currency) }
    var editPaymentInfo by remember { mutableStateOf(tournament.paymentInfo ?: "") }
    var editPrizeInfo by remember { mutableStateOf(tournament.prizeInfo ?: "") }
    var editDurationMinutes by remember {
        mutableStateOf(tournament.durationMinutes?.toString() ?: "")
    }
    var editAgeGroup by remember { mutableStateOf(tournament.ageGroup) }
    var editSkillDivision by remember { mutableStateOf(tournament.skillDivision) }

    val existingConfig: FormatConfig? = remember(tournament) {
        FormatConfig.decodeOrNull(tournament.formatConfigData)
    }
    var editFormatConfig by remember {
        mutableStateOf(existingConfig ?: FormatConfig.fallback(tournament.matchFormat))
    }
    // Only write the config when it was loaded or the organizer touched it, so an
    // unreadable stored config is never replaced with blank defaults.
    var formatConfigEdited by remember { mutableStateOf(false) }
    var editScoringConfig by remember { mutableStateOf(tournament.scoringConfig) }
    var editCricketRules by remember { mutableStateOf(tournament.cricketRules) }

    var showVenuePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }
    var showEventTypePicker by remember { mutableStateOf(false) }
    var showMatchFormatPicker by remember { mutableStateOf(false) }

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val deadlineDateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val canSave = editTitle.trim().isNotEmpty() && editVenueName.isNotEmpty()

    // iOS presents the venue picker with .fullScreenCover over the edit sheet.
    if (showVenuePicker) FullScreenCover(onDismissRequest = { showVenuePicker = false }) {
        VenuePickerScreen(
            sportType = tournament.sportType,
            onVenueSelected = { name, address, lat, lng ->
                editVenueName = name
                editVenueAddress = address
                editVenueLatitude = lat
                editVenueLongitude = lng
                showVenuePicker = false
            },
            onCancel = { showVenuePicker = false },
        )
    }

    BackHandler { onCancel() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header icon
                Column(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppAccent.copy(alpha = 0.12f),
                        modifier = Modifier.size(60.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, null, tint = AppAccent, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Edit Tournament", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                // Tournament Name
                SectionLabel("TOURNAMENT NAME")
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Tournament Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                        unfocusedContainerColor = Color(0xFFF2F2F7),
                        focusedContainerColor = Color(0xFFF2F2F7),
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                // Event Type & Match Format (same choices as Create Tournament)
                if (tournament.isTeamEvent) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("EVENT TYPE")
                            if (canEditMatchRules) {
                                CricketEventTypePicker(editCricketRules, { editCricketRules = it }, AppAccent)
                            } else {
                                LockedValue(editCricketRules.eventType.displayName)
                            }
                        }
                        Column {
                            SectionLabel("MATCH FORMAT")
                            PickerButton(editMatchFormat.displayName) { showMatchFormatPicker = true }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("EVENT TYPE")
                            PickerButton(editFormat.displayName) { showEventTypePicker = true }
                        }
                        Column(Modifier.weight(1f)) {
                            SectionLabel("MATCH FORMAT")
                            PickerButton(editMatchFormat.displayName) { showMatchFormatPicker = true }
                        }
                    }
                }

                // Division
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("DIVISION")
                    EditAgeGroupDropdown(editAgeGroup, tournament.sportType.ageGroupsIncluding(tournament.ageGroup)) { editAgeGroup = it }
                    if (tournament.sportType.skillDivisions.isNotEmpty()) {
                        SkillDivisionPicker(
                            sport = tournament.sportType,
                            selection = editSkillDivision,
                            onSelectionChange = { editSkillDivision = it },
                        )
                    }
                }

                if (tournament.isTeamEvent) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("MATCH RULES")
                        if (canEditMatchRules) {
                            CricketRulesEditor(editCricketRules, { editCricketRules = it }, AppAccent)
                        } else {
                            LockedValue(editCricketRules.summary)
                        }
                    }
                } else if (canEditMatchRules) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("SCORING")
                        ScoringConfigEditor(
                            sport = tournament.sportType,
                            config = editScoringConfig,
                            onConfigChange = { editScoringConfig = it },
                            singleGameOnly = editMatchFormat == MatchFormat.ROUND_ROBIN,
                        )
                    }
                }

                // Format rules: same editor as Create Tournament, so both offer the same choices.
                FormatRulesEditor(
                    format = editMatchFormat,
                    config = editFormatConfig,
                    onConfigChange = {
                        editFormatConfig = it
                        formatConfigEdited = true
                    },
                    accent = AppAccent,
                    isCricket = tournament.isTeamEvent,
                )

                // Random Pairing (only for doubles; never for team sports)
                if (editFormat.isDoubles && !tournament.isTeamEvent) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF2F2F7),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Shuffle, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Random Pairing", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("System assigns partners after deadline", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = editRandomPairing,
                                onCheckedChange = { editRandomPairing = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = AppAccent),
                            )
                        }
                    }
                }

                // Location & Time
                SectionLabel("LOCATION & TIME")
                Surface(
                    onClick = { showVenuePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF2F2F7),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = CircleShape, color = AppAccent.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocationOn, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        if (editVenueName.isEmpty()) {
                            Text("Select Venue", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                        } else {
                            Column(Modifier.weight(1f)) {
                                Text(editVenueName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (editVenueAddress.isNotEmpty()) {
                                    Text(editVenueAddress, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                // Date & Time row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF5856D6).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(dateFmt.format(editDate), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF5856D6).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(timeFmt.format(editDate), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Registration Deadline
                SectionLabel("REGISTRATION DEADLINE")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { showDeadlineDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = Color(0xFFFF9500).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(deadlineDateFmt.format(editDeadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        onClick = { showDeadlineTimePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(timeFmt.format(editDeadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Entry Fee
                SectionLabel("ENTRY FEE")
                OutlinedTextField(
                    value = editEntryFee,
                    onValueChange = { editEntryFee = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("0 = Free") },
                    prefix = {
                        Text(editCurrency, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                        unfocusedContainerColor = Color(0xFFF2F2F7),
                        focusedContainerColor = Color(0xFFF2F2F7),
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    singleLine = true,
                )

                val feeValue = editEntryFee.toDoubleOrNull()
                if (feeValue != null && feeValue > 0) {
                    // Payment instruction
                    SectionLabel("PAYMENT INSTRUCTION")
                    OutlinedTextField(
                        value = editPaymentInfo,
                        onValueChange = { editPaymentInfo = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("e.g. Venmo: @handle, or Pay cash at venue", fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedContainerColor = Color(0xFFF2F2F7),
                            focusedContainerColor = Color(0xFFF2F2F7),
                        ),
                        minLines = 2,
                        maxLines = 4,
                    )

                    // Payment warning
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFCC00).copy(alpha = 0.08f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.GppMaybe, null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "TournMate does not process payments. You are responsible for collecting and refunding fees directly.",
                                fontSize = 12.sp, color = Color.Gray,
                            )
                        }
                    }

                    // Prize info
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("WINNING REWARDS")
                        OutlinedTextField(
                            value = editPrizeInfo,
                            onValueChange = { editPrizeInfo = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { Text("e.g. 1st: \$150, 2nd: \$50, 3rd: Free entry next event", fontSize = 14.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppAccent,
                                unfocusedBorderColor = Color(0xFFE5E5EA),
                                unfocusedContainerColor = Color(0xFFF2F2F7),
                                focusedContainerColor = Color(0xFFF2F2F7),
                            ),
                            minLines = 3,
                            maxLines = 4,
                        )
                        Text("Describe what winners receive.", fontSize = 11.sp, color = Color.Gray)
                    }

                    // Duration
                    SectionLabel("ESTIMATED DURATION")
                    OutlinedTextField(
                        value = editDurationMinutes,
                        onValueChange = { editDurationMinutes = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("e.g. 120") },
                        suffix = { Text("minutes", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.HourglassBottom, null, tint = Color(0xFF5856D6).copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedContainerColor = Color(0xFFF2F2F7),
                            focusedContainerColor = Color(0xFFF2F2F7),
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        singleLine = true,
                    )
                }
            }

            // Save button — iOS: plain capsule, no footer background or shadow.
            Box(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)) {
                PrimaryCapsuleButton(
                    onClick = {
                        focusManager.clearFocus()
                        onSave(
                            editTitle.trim(), editDate,
                            editVenueName, editVenueAddress,
                            editVenueLatitude, editVenueLongitude,
                            if (tournament.isTeamEvent) TournamentFormat.TEAM else editFormat, editMatchFormat,
                            editFormatConfig.takeIf { existingConfig != null || formatConfigEdited },
                            if (editFormat.isDoubles && !tournament.isTeamEvent) editRandomPairing else false,
                            editDeadline,
                            editEntryFee.toDoubleOrNull(), editCurrency,
                            editPaymentInfo.ifBlank { null },
                            editPrizeInfo.ifBlank { null },
                            editDurationMinutes.toIntOrNull(),
                            editAgeGroup,
                            // Round robin is always a single game.
                            // Legacy tournaments (no stored scoring) stay lenient unless the
                            // organizer actually changed the scoring here.
                            editScoringConfig
                                .let { if (editMatchFormat == MatchFormat.ROUND_ROBIN) it.copy(gamesPerMatch = 1) else it }
                                .takeIf { canEditMatchRules && !tournament.isTeamEvent }
                                ?.takeIf { tournament.enforcesScoringRules || editScoringConfig != tournament.scoringConfig },
                            editSkillDivision,
                            editCricketRules.takeIf { tournament.isTeamEvent && canEditMatchRules },
                        )
                    },
                    enabled = canSave,
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE CHANGES", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(editDate),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            editDate = DatePickerUtils.applyPickerDate(editDate, millis)
                            editDeadline = Tournament.defaultDeadline(editDate)
                        }
                        showDatePicker = false
                    }) { Text("OK", color = AppAccent) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = AppAccent) }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            val cal = Calendar.getInstance().apply { time = editDate }
            val timePickerState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            time = editDate
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        editDate = newCal.time
                        showTimePicker = false
                    }) { Text("OK", color = AppAccent) }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = AppAccent) }
                },
                text = { TimePicker(state = timePickerState) },
                title = { Text("Select Time") },
            )
        }

        if (showDeadlineDatePicker) {
            val deadlineState = rememberDatePickerState(
                initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(editDeadline),
            )
            DatePickerDialog(
                onDismissRequest = { showDeadlineDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        deadlineState.selectedDateMillis?.let { editDeadline = DatePickerUtils.applyPickerDate(editDeadline, it) }
                        showDeadlineDatePicker = false
                    }) { Text("OK", color = AppAccent) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeadlineDatePicker = false }) { Text("Cancel", color = AppAccent) }
                },
            ) {
                DatePicker(state = deadlineState)
            }
        }

        if (showDeadlineTimePicker) {
            val cal = Calendar.getInstance().apply { time = editDeadline }
            val deadlineTimeState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
            )
            AlertDialog(
                onDismissRequest = { showDeadlineTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        editDeadline = Calendar.getInstance().apply {
                            time = editDeadline
                            set(Calendar.HOUR_OF_DAY, deadlineTimeState.hour)
                            set(Calendar.MINUTE, deadlineTimeState.minute)
                        }.time
                        showDeadlineTimePicker = false
                    }) { Text("OK", color = AppAccent) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeadlineTimePicker = false }) { Text("Cancel", color = AppAccent) }
                },
                text = { TimePicker(state = deadlineTimeState) },
                title = { Text("DEADLINE TIME") },
            )
        }

        // Event type picker
        if (showEventTypePicker) {
            PickerDialog(
                title = "EVENT TYPE",
                items = TournamentFormat.entries.filter { it != TournamentFormat.TEAM },
                selectedItem = editFormat,
                displayName = { it.displayName },
                onSelect = {
                    editFormat = it
                    if (it.isSingles) editRandomPairing = false
                    showEventTypePicker = false
                },
                onDismiss = { showEventTypePicker = false },
            )
        }

        // Match format picker
        if (showMatchFormatPicker) {
            PickerDialog(
                title = "MATCH FORMAT",
                items = MatchFormat.offered(current = tournament.matchFormat),
                selectedItem = editMatchFormat,
                displayName = { it.displayName },
                onSelect = {
                    if (it != editMatchFormat) {
                        editMatchFormat = it
                        // Fresh defaults, but Edit never adds the tie-break rules version (iOS parity).
                        editFormatConfig = FormatConfig.defaults(it).copy(tieBreakRulesVersion = editFormatConfig.tieBreakRulesVersion)
                        formatConfigEdited = true
                    }
                    showMatchFormatPicker = false
                },
                onDismiss = { showMatchFormatPicker = false },
            )
        }
    }
}

// ── Reusable helpers ──────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun PickerButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF2F2F7),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EditAgeGroupDropdown(selected: AgeGroup, options: List<AgeGroup>, onSelect: (AgeGroup) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF2F2F7),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Badge, null, tint = AppAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(selected.displayName, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Default.UnfoldMore, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { group ->
                DropdownMenuItem(
                    text = {
                        Text(
                            group.displayName,
                            fontWeight = if (group == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (group == selected) AppAccent else Color.Black,
                        )
                    },
                    trailingIcon = if (group == selected) {
                        { Icon(Icons.Default.Check, null, tint = AppAccent, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onSelect(group); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun <T> PickerDialog(
    title: String,
    items: List<T>,
    selectedItem: T,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
        text = {
            Column {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    Surface(
                        onClick = { onSelect(item) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AppAccent.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                displayName(item),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppAccent else Color.Black,
                            )
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = AppAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppAccent) }
        },
    )
}

/** A value that can't be changed any more (matches exist). Mirrors iOS `lockedValue`. */
@Composable
private fun LockedValue(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, "Locked", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}
