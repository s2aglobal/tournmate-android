package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.validation.InputValidator
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val commonCurrencies = listOf(
    Triple("USD", "$", "US Dollar"),
    Triple("INR", "₹", "Indian Rupee"),
    Triple("GBP", "£", "British Pound"),
    Triple("EUR", "€", "Euro"),
    Triple("CAD", "C$", "Canadian Dollar"),
    Triple("AUD", "A$", "Australian Dollar"),
    Triple("SGD", "S$", "Singapore Dollar"),
    Triple("MYR", "RM", "Malaysian Ringgit"),
)

private val publishableSports = listOf(SportType.BADMINTON, SportType.PICKLEBALL, SportType.TENNIS)

private data class MatchFormatItem(
    val format: MatchFormat,
    val icon: ImageVector,
    val tagline: String,
)

private val matchFormatItems = listOf(
    MatchFormatItem(MatchFormat.SINGLE_ELIMINATION, Icons.Default.EmojiEvents, "Lose once, you're out"),
    MatchFormatItem(MatchFormat.DOUBLE_ELIMINATION, Icons.Default.Replay, "Lose twice to be eliminated"),
    MatchFormatItem(MatchFormat.ROUND_ROBIN, Icons.Default.Loop, "Everyone plays everyone"),
    MatchFormatItem(MatchFormat.GROUP_KNOCKOUT, Icons.Default.ViewModule, "Group stage then bracket"),
    MatchFormatItem(MatchFormat.SWISS, Icons.Default.FormatListNumbered, "Paired by similar record"),
    MatchFormatItem(MatchFormat.MANUAL_DRAW, Icons.Default.Draw, "Organizer sets matchups"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishTournamentScreen(
    firebaseUid: String?,
    preferredSport: SportType,
    onPublish: (
        title: String, date: Date, location: String, locationAddress: String,
        locationLatitude: Double?, locationLongitude: Double?,
        format: TournamentFormat, matchFormat: MatchFormat, formatConfig: FormatConfig?,
        randomPairing: Boolean, registrationDeadline: Date?, createdBy: String?,
        entryFee: Double?, currency: String, paymentInfo: String?, prizeInfo: String?,
        durationMinutes: Int?, ageGroup: AgeGroup, sportType: SportType,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var wizardStep by remember { mutableIntStateOf(1) }
    var showSuccess by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("TournMate Tournament") }
    var newDate by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }.time)
    }
    var newFormat by remember { mutableStateOf(TournamentFormat.MENS_DOUBLES) }
    var newMatchFormat by remember { mutableStateOf(MatchFormat.SINGLE_ELIMINATION) }
    var newRandomPairing by remember { mutableStateOf(false) }
    var newDeadline by remember { mutableStateOf(Tournament.defaultDeadline(newDate)) }
    var newEntryFee by remember { mutableStateOf("") }
    var newCurrency by remember { mutableStateOf("USD") }
    var newPaymentInfo by remember { mutableStateOf("") }
    var newPrizeInfo by remember { mutableStateOf("") }
    var newDurationMinutes by remember { mutableStateOf("") }
    var newAgeGroup by remember { mutableStateOf(AgeGroup.OPEN) }
    var newSportType by remember { mutableStateOf(preferredSport) }
    var newFormatConfig by remember { mutableStateOf(FormatConfig.defaults(MatchFormat.SINGLE_ELIMINATION)) }

    var venueName by remember { mutableStateOf("") }
    var venueAddress by remember { mutableStateOf("") }
    var venueLatitude by remember { mutableStateOf<Double?>(null) }
    var venueLongitude by remember { mutableStateOf<Double?>(null) }

    // Bottom sheet states
    var showVenueSheet by remember { mutableStateOf(false) }
    var showEventTypeSheet by remember { mutableStateOf(false) }
    var showAgeGroupSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }

    val titleError = remember(newTitle) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) null else InputValidator.validateEventTitle(newTitle).errorMessage
    }

    val dateIsTooSoon = newDate.time <= System.currentTimeMillis() + 3 * 3600 * 1000L
    val deadlineIsPast = newDeadline.before(Date())
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    val nextDisabled = when (wizardStep) {
        1 -> titleError != null || !InputValidator.validateEventTitle(newTitle).isValid || venueName.isEmpty() || dateIsTooSoon
        3 -> {
            val hasFee = (newEntryFee.toDoubleOrNull() ?: 0.0) > 0
            val missingPayment = newPaymentInfo.trim().isEmpty()
            val missingPrize = newPrizeInfo.trim().isEmpty()
            val feeValid = InputValidator.validateEntryFee(newEntryFee.toDoubleOrNull()).isValid
            val notesValid = InputValidator.validatePaymentInfo(newPaymentInfo).isValid && InputValidator.validatePrizeInfo(newPrizeInfo).isValid
            (hasFee && (missingPayment || missingPrize)) || !feeValid || !notesValid || deadlineIsPast
        }
        else -> false
    }

    BackHandler {
        if (wizardStep > 1) wizardStep -= 1 else onDismiss()
    }

    // Date picker dialog
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = newDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { time = newDate }
                        val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                        cal.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                        cal.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                        newDate = cal.time
                        newDeadline = Tournament.defaultDeadline(newDate)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { time = newDate }
        val tpState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("START TIME", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                Button(
                    onClick = {
                        cal.set(Calendar.HOUR_OF_DAY, tpState.hour)
                        cal.set(Calendar.MINUTE, tpState.minute)
                        newDate = cal.time
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                ) { Text("SET TIME") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }

    // Deadline date picker
    if (showDeadlineDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = newDeadline.time)
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { time = newDeadline }
                        val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                        cal.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                        cal.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                        newDeadline = cal.time
                    }
                    showDeadlineDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDeadlineDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    // Deadline time picker
    if (showDeadlineTimePicker) {
        val cal = Calendar.getInstance().apply { time = newDeadline }
        val tpState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showDeadlineTimePicker = false },
            title = { Text("DEADLINE TIME", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                Button(
                    onClick = {
                        cal.set(Calendar.HOUR_OF_DAY, tpState.hour)
                        cal.set(Calendar.MINUTE, tpState.minute)
                        newDeadline = cal.time
                        showDeadlineTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                ) { Text("SET TIME") }
            },
            dismissButton = { TextButton(onClick = { showDeadlineTimePicker = false }) { Text("Cancel") } },
        )
    }

    if (showEventTypeSheet) {
        ListBottomSheet(
            title = "EVENT TYPE",
            items = TournamentFormat.entries.filter { it != TournamentFormat.FIXED_DOUBLES },
            selectedItem = newFormat,
            itemLabel = { it.displayName },
            onItemSelected = { newFormat = it; if (it.isSingles) newRandomPairing = false; showEventTypeSheet = false },
            onDismiss = { showEventTypeSheet = false },
        )
    }

    if (showAgeGroupSheet) {
        ListBottomSheet(
            title = "AGE GROUP",
            items = AgeGroup.entries.toList(),
            selectedItem = newAgeGroup,
            itemLabel = { it.displayName },
            onItemSelected = { newAgeGroup = it; showAgeGroupSheet = false },
            onDismiss = { showAgeGroupSheet = false },
        )
    }

    if (showCurrencySheet) {
        ListBottomSheet(
            title = "CURRENCY",
            items = commonCurrencies,
            selectedItem = commonCurrencies.first { it.first == newCurrency },
            itemLabel = { "${it.second} ${it.first} — ${it.third}" },
            onItemSelected = { newCurrency = it.first; showCurrencySheet = false },
            onDismiss = { showCurrencySheet = false },
        )
    }

    // Main fullscreen content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusManager.clearFocus() },
    ) {
        if (showSuccess) {
            SuccessScreen(title = newTitle, onDone = onDismiss)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                WizardTopBar(
                    step = wizardStep,
                    onBack = { if (wizardStep > 1) wizardStep -= 1 else onDismiss() },
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                WizardProgressBar(step = wizardStep, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (wizardStep) {
                        1 -> Step1BasicInfo(
                            sportType = newSportType, onSportChange = { newSportType = it },
                            title = newTitle, onTitleChange = { newTitle = it }, titleError = titleError,
                            format = newFormat, onEventTypeClick = { showEventTypeSheet = true },
                            randomPairing = newRandomPairing, onRandomPairingChange = { newRandomPairing = it },
                            venueName = venueName, venueAddress = venueAddress, onVenueClick = { showVenueSheet = true },
                            date = newDate, dateFmt = dateFmt, timeFmt = timeFmt,
                            onDateClick = { showDatePicker = true }, onTimeClick = { showTimePicker = true },
                            dateIsTooSoon = dateIsTooSoon,
                            durationMinutes = newDurationMinutes, onDurationChange = { newDurationMinutes = it },
                        )
                        2 -> Step2FormatRules(
                            matchFormat = newMatchFormat,
                            onMatchFormatChange = { newMatchFormat = it; newFormatConfig = FormatConfig.defaults(it) },
                            ageGroup = newAgeGroup, onAgeGroupClick = { showAgeGroupSheet = true },
                            formatConfig = newFormatConfig, onFormatConfigChange = { newFormatConfig = it },
                        )
                        3 -> Step3RulesLogistics(
                            entryFee = newEntryFee, onEntryFeeChange = { newEntryFee = it },
                            currency = newCurrency, onCurrencyClick = { showCurrencySheet = true },
                            paymentInfo = newPaymentInfo, onPaymentInfoChange = { newPaymentInfo = it },
                            prizeInfo = newPrizeInfo, onPrizeInfoChange = { newPrizeInfo = it },
                            deadline = newDeadline, dateFmt = dateFmt, timeFmt = timeFmt,
                            onDeadlineDateClick = { showDeadlineDatePicker = true },
                            onDeadlineTimeClick = { showDeadlineTimePicker = true },
                            deadlineIsPast = deadlineIsPast,
                        )
                        else -> Step4Review(
                            sportType = newSportType, title = newTitle,
                            format = newFormat, matchFormat = newMatchFormat,
                            formatConfig = newFormatConfig, venueName = venueName,
                            date = newDate, deadline = newDeadline,
                            ageGroup = newAgeGroup, entryFee = newEntryFee,
                            currency = newCurrency, prizeInfo = newPrizeInfo,
                            randomPairing = newRandomPairing, dateFmt = dateFmt,
                        )
                    }
                }

                WizardBottomButton(
                    step = wizardStep, disabled = nextDisabled,
                    onNext = { wizardStep += 1 },
                    onPublish = {
                        onPublish(
                            newTitle, newDate, venueName, venueAddress, venueLatitude, venueLongitude,
                            newFormat, newMatchFormat, newFormatConfig,
                            if (newFormat.isDoubles) newRandomPairing else false,
                            newDeadline, firebaseUid,
                            newEntryFee.toDoubleOrNull(), newCurrency,
                            newPaymentInfo.ifBlank { null }, newPrizeInfo.ifBlank { null },
                            newDurationMinutes.toIntOrNull(), newAgeGroup, newSportType,
                        )
                        showSuccess = true
                    },
                )
            }
        }
    }

    // Venue picker renders as a fullscreen overlay on top of the wizard
    if (showVenueSheet) {
        VenuePickerScreen(
            onVenueSelected = { name, address, lat, lng ->
                venueName = name
                venueAddress = address
                venueLatitude = lat
                venueLongitude = lng
                showVenueSheet = false
            },
            onCancel = { showVenueSheet = false },
        )
    }
}

// ── Bottom Sheets ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ListBottomSheet(
    title: String, items: List<T>, selectedItem: T,
    itemLabel: (T) -> String, onItemSelected: (T) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                title, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            items.forEach { item ->
                val isSelected = item == selectedItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemSelected(item) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        itemLabel(item), fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) BrandPurple else Color.Black,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Surface(Modifier.size(24.dp), CircleShape, BrandPurple) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.White)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFFF2F2F7))
            }
        }
    }
}

// ── Wizard Chrome ────────────────────────────────────

@Composable
private fun WizardTopBar(step: Int, onBack: () -> Unit) {
    val titles = listOf("BASIC INFO", "FORMAT RULES", "RULES & LOGISTICS", "REVIEW & POST")
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(if (step == 1) Icons.Default.Close else Icons.Default.ChevronLeft, null, Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(titles.getOrElse(step - 1) { "" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun WizardProgressBar(step: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { i ->
            Surface(Modifier.weight(1f).height(4.dp), RoundedCornerShape(50), if (i < step) BrandPurple else Color.Gray.copy(alpha = 0.15f)) {}
        }
    }
}

@Composable
private fun WizardBottomButton(step: Int, disabled: Boolean, onNext: () -> Unit, onPublish: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding()) {
            Button(
                onClick = if (step < 4) onNext else onPublish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !disabled,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, disabledContainerColor = BrandPurple.copy(alpha = 0.4f)),
            ) {
                if (step < 4) {
                    Text("NEXT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PUBLISH", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// ── Step 1 ───────────────────────────────────────────

@Composable
private fun Step1BasicInfo(
    sportType: SportType, onSportChange: (SportType) -> Unit,
    title: String, onTitleChange: (String) -> Unit, titleError: String?,
    format: TournamentFormat, onEventTypeClick: () -> Unit,
    randomPairing: Boolean, onRandomPairingChange: (Boolean) -> Unit,
    venueName: String, venueAddress: String, onVenueClick: () -> Unit,
    date: Date, dateFmt: SimpleDateFormat, timeFmt: SimpleDateFormat,
    onDateClick: () -> Unit, onTimeClick: () -> Unit,
    dateIsTooSoon: Boolean,
    durationMinutes: String, onDurationChange: (String) -> Unit,
) {
    SectionLabel("SPORT")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        publishableSports.forEach { sport ->
            val sel = sportType == sport
            Surface(
                onClick = { onSportChange(sport) }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (sel) BrandPurple.copy(alpha = 0.1f) else Color(0xFFF2F2F7),
                border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BrandPurple), width = 2.dp) else null,
            ) {
                Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SportsTennis, null, Modifier.size(20.dp), tint = if (sel) BrandPurple else Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Text(sport.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (sel) BrandPurple else Color.Gray)
                }
            }
        }
    }

    SectionLabel("TOURNAMENT NAME")
    OutlinedTextField(
        value = title, onValueChange = onTitleChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Saturday's Open") }, shape = RoundedCornerShape(14.dp),
        isError = titleError != null, supportingText = titleError?.let { { Text(it, color = Color.Red) } },
        singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    SectionLabel("EVENT TYPE")
    PickerRow(text = format.displayName, onClick = onEventTypeClick)

    if (format.isDoubles) {
        ToggleRow(Icons.Default.Shuffle, "Random Pairing", "System assigns partners after deadline", randomPairing, onRandomPairingChange)
    }

    SectionLabel("VENUE")
    Surface(
        onClick = onVenueClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(Modifier.size(40.dp), CircleShape, BrandPurple.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = BrandPurple) }
            }
            if (venueName.isEmpty()) {
                Text("Select Venue", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f)) {
                    Text(venueName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (venueAddress.isNotEmpty()) Text(venueAddress, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                }
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = Color.Gray)
        }
    }

    SectionLabel("DATE & TIME")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = onDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = BrandPurple.copy(alpha = 0.6f))
                Spacer(Modifier.width(10.dp))
                Text(dateFmt.format(date), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Surface(onClick = onTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, Modifier.size(16.dp), tint = BrandPurple.copy(alpha = 0.6f))
                Spacer(Modifier.width(10.dp))
                Text(timeFmt.format(date), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    if (dateIsTooSoon) {
        Text("Tournament must be at least 3 hours from now.", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)
    }

    SectionLabel("ESTIMATED DURATION")
    OutlinedTextField(
        value = durationMinutes, onValueChange = onDurationChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("e.g. 120") }, suffix = { Text("minutes", color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(14.dp), singleLine = true,
    )
}

// ── Step 2 ───────────────────────────────────────────

@Composable
private fun Step2FormatRules(
    matchFormat: MatchFormat, onMatchFormatChange: (MatchFormat) -> Unit,
    ageGroup: AgeGroup, onAgeGroupClick: () -> Unit,
    formatConfig: FormatConfig, onFormatConfigChange: (FormatConfig) -> Unit,
) {
    SectionLabel("MATCH FORMAT")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in matchFormatItems.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    val sel = matchFormat == item.format
                    Surface(
                        onClick = { onMatchFormatChange(item.format) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) BrandPurple.copy(alpha = 0.1f) else Color(0xFFF2F2F7),
                        border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BrandPurple), width = 1.5.dp) else null,
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(item.icon, null, Modifier.size(24.dp), tint = if (sel) BrandPurple else Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text(item.format.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) BrandPurple else Color.Black, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(2.dp))
                            Text(item.tagline, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    SectionLabel("AGE GROUP")
    PickerRow(text = ageGroup.displayName, icon = Icons.Default.Person, onClick = onAgeGroupClick)

    // Format-specific config
    FormatConfigSection(matchFormat, formatConfig, onFormatConfigChange)
}

@Composable
private fun FormatConfigSection(
    matchFormat: MatchFormat, config: FormatConfig, onChange: (FormatConfig) -> Unit,
) {
    when (matchFormat) {
        MatchFormat.SINGLE_ELIMINATION -> {
            SectionLabel("SEEDING")
            SeedingModePicker(config, onChange)
            ToggleRow(Icons.Default.SwapVert, "Allow Byes", "Top seeds get a first-round bye when bracket isn't full", config.allowByes) {
                onChange(config.copy(allowByes = it))
            }
            ToggleRow(Icons.Default.MilitaryTech, "Bronze Match", "Semi-final losers play for 3rd place", config.bronzeMatch) {
                onChange(config.copy(bronzeMatch = it))
            }
            ToggleRow(Icons.Default.AccountTree, "Consolation Bracket", "First-round losers play a secondary bracket", config.consolationBracket) {
                onChange(config.copy(consolationBracket = it))
            }
        }
        MatchFormat.DOUBLE_ELIMINATION -> {
            SectionLabel("SEEDING")
            SeedingModePicker(config, onChange)
            ToggleRow(Icons.Default.SwapVert, "Allow Byes", "Top seeds get a first-round bye when bracket isn't full", config.allowByes) {
                onChange(config.copy(allowByes = it))
            }
            InfoBanner(Icons.Default.Info, BrandPurple, "Players must lose twice to be eliminated. A losers bracket runs alongside the main bracket.")
        }
        MatchFormat.ROUND_ROBIN -> {
            ToggleRow(Icons.Default.Repeat, "Double Round Robin", "Each team plays every other team twice", config.doubleRoundRobin) {
                onChange(config.copy(doubleRoundRobin = it))
            }
            SectionLabel("POINTS SYSTEM")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Win", config.pointsPerWin, 0..5, Modifier.weight(1f)) { onChange(config.copy(pointsPerWin = it)) }
                StepperField("Draw", config.pointsPerDraw, 0..5, Modifier.weight(1f)) { onChange(config.copy(pointsPerDraw = it)) }
                StepperField("Loss", config.pointsPerLoss, 0..5, Modifier.weight(1f)) { onChange(config.copy(pointsPerLoss = it)) }
            }
        }
        MatchFormat.GROUP_KNOCKOUT -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Groups", config.groupCount, 2..8, Modifier.weight(1f)) { onChange(config.copy(groupCount = it)) }
                StepperField("Per Group", config.teamsPerGroup, 3..8, Modifier.weight(1f)) { onChange(config.copy(teamsPerGroup = it)) }
            }
            StepperField("Advance Per Group", config.advancingPerGroup, 1..4, Modifier.fillMaxWidth()) { onChange(config.copy(advancingPerGroup = it)) }
            InfoBanner(Icons.Default.Info, BrandPurple, "Top finishers from each group advance to a single-elimination knockout stage.")
        }
        MatchFormat.SWISS -> {
            StepperField("Number of Rounds", config.swissRounds, 3..10, Modifier.fillMaxWidth()) { onChange(config.copy(swissRounds = it)) }
            InfoBanner(Icons.Default.Info, BrandPurple, "Each round, players with similar records are paired against each other. No one is eliminated.")
        }
        MatchFormat.MANUAL_DRAW -> {
            InfoBanner(Icons.Default.Draw, BrandPurple, "You will manually assign all matchups after registration closes. Full control over the draw.")
        }
    }

    Spacer(Modifier.height(4.dp))
    SectionLabel("MAX PARTICIPANTS (OPTIONAL)")
    OutlinedTextField(
        value = config.maxParticipants?.toString() ?: "",
        onValueChange = { onChange(config.copy(maxParticipants = it.toIntOrNull())) },
        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Unlimited") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp), singleLine = true,
    )
}

@Composable
private fun SeedingModePicker(config: FormatConfig, onChange: (FormatConfig) -> Unit) {
    val modes = listOf(
        SeedingMode.ELO_RANKED to "Elo Ranked",
        SeedingMode.RANDOM to "Random",
        SeedingMode.MANUAL to "Manual",
    )
    val currentMode = SeedingMode.fromRawValue(config.seedingMode)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { (mode, label) ->
            val sel = currentMode == mode
            Surface(
                onClick = { onChange(config.copy(seedingMode = mode.rawValue)) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                color = if (sel) BrandPurple.copy(alpha = 0.15f) else Color(0xFFF2F2F7),
                border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BrandPurple), width = 1.dp) else null,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    color = if (sel) BrandPurple else Color.Black, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StepperField(label: String, value: Int, range: IntRange, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            modifier = Modifier.background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))) {
            IconButton(onClick = { if (value > range.first) onChange(value - 1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Remove, null, Modifier.size(14.dp))
            }
            Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 30.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { if (value < range.last) onChange(value + 1) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
            }
        }
    }
}

// ── Step 3 ───────────────────────────────────────────

@Composable
private fun Step3RulesLogistics(
    entryFee: String, onEntryFeeChange: (String) -> Unit,
    currency: String, onCurrencyClick: () -> Unit,
    paymentInfo: String, onPaymentInfoChange: (String) -> Unit,
    prizeInfo: String, onPrizeInfoChange: (String) -> Unit,
    deadline: Date, dateFmt: SimpleDateFormat, timeFmt: SimpleDateFormat,
    onDeadlineDateClick: () -> Unit, onDeadlineTimeClick: () -> Unit,
    deadlineIsPast: Boolean,
) {
    val symbol = commonCurrencies.firstOrNull { it.first == currency }?.second ?: currency

    SectionLabel("PRICING")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onCurrencyClick, shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(currency, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp), tint = Color.Gray)
            }
        }
        OutlinedTextField(
            value = entryFee, onValueChange = onEntryFeeChange, modifier = Modifier.weight(1f),
            placeholder = { Text("0 = Free") },
            prefix = { Text(symbol, fontWeight = FontWeight.Bold, color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp), singleLine = true,
        )
    }

    val hasFee = (entryFee.toDoubleOrNull() ?: 0.0) > 0
    if (hasFee) {
        SectionLabel("PAYMENT INSTRUCTION")
        OutlinedTextField(
            value = paymentInfo, onValueChange = onPaymentInfoChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Venmo: @handle, or Pay cash at venue") },
            shape = RoundedCornerShape(14.dp), minLines = 2, maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        InfoBanner(Icons.Default.Shield, Color(0xFFFF9800), "TournMate does not process payments. You are responsible for collecting and refunding fees directly.")

        SectionLabel("WINNING REWARDS")
        OutlinedTextField(
            value = prizeInfo, onValueChange = onPrizeInfoChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 1st: \$150, 2nd: \$50, 3rd: Free entry") },
            shape = RoundedCornerShape(14.dp), minLines = 3, maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }

    SectionLabel("REGISTRATION DEADLINE")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = onDeadlineDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = BrandPurple.copy(alpha = 0.7f))
                Spacer(Modifier.width(10.dp))
                Text(dateFmt.format(deadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Surface(onClick = onDeadlineTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, Modifier.size(16.dp), tint = BrandPurple.copy(alpha = 0.7f))
                Spacer(Modifier.width(10.dp))
                Text(timeFmt.format(deadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    if (deadlineIsPast) Text("Registration deadline must be in the future.", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)
    InfoBanner(Icons.Default.Info, BrandPurple, "Players must register before this time. Defaults to 1 hour before start.")
}

// ── Step 4 ───────────────────────────────────────────

@Composable
private fun Step4Review(
    sportType: SportType, title: String, format: TournamentFormat, matchFormat: MatchFormat,
    formatConfig: FormatConfig, venueName: String, date: Date, deadline: Date,
    ageGroup: AgeGroup, entryFee: String, currency: String, prizeInfo: String,
    randomPairing: Boolean, dateFmt: SimpleDateFormat,
) {
    val symbol = commonCurrencies.firstOrNull { it.first == currency }?.second ?: currency
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = BrandPurple.copy(alpha = 0.05f),
    ) {
        Column(Modifier.padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Surface(Modifier.size(50.dp), CircleShape, BrandPurple.copy(alpha = 0.12f)) {}
                Icon(Icons.Default.Visibility, null, Modifier.size(26.dp), tint = BrandPurple)
            }
            Spacer(Modifier.height(12.dp))
            Text("CONFIRM DETAILS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("EVERYTHING LOOKS GREAT!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    ReviewRow("SPORT", sportType.displayName); ReviewRow("TOURNAMENT", title)
    ReviewRow("EVENT TYPE", format.displayName); ReviewRow("FORMAT", matchFormat.displayName)
    ReviewRow("VENUE", venueName); ReviewRow("DATE", dateFmt.format(date))
    ReviewRow("DEADLINE", dateFmt.format(deadline))
    if (ageGroup != AgeGroup.OPEN) ReviewRow("AGE GROUP", ageGroup.displayName)
    val fee = entryFee.toDoubleOrNull()
    if (fee != null && fee > 0) ReviewRow("ENTRY FEE", "$symbol${String.format("%.2f", fee)}", BrandPurple) else ReviewRow("ENTRY FEE", "Free", BrandPurple)
    if (prizeInfo.isNotEmpty()) ReviewRow("PRIZES", prizeInfo, Color(0xFFFF9800))
    if (format.isDoubles && randomPairing) ReviewRow("PAIRING", "Random Assignment")
    formatConfig.maxParticipants?.let { ReviewRow("MAX PLAYERS", "$it") }
    Spacer(Modifier.height(8.dp))
    InfoBanner(Icons.Default.Info, Color(0xFFFF9800).copy(alpha = 0.7f), "Once published, players will be able to see and register for this tournament immediately.")
}

// ── Shared Components ────────────────────────────────

@Composable
private fun SuccessScreen(title: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.size(100.dp), CircleShape, BrandPurple.copy(alpha = 0.15f)) {}
            Surface(Modifier.size(72.dp), CircleShape, BrandPurple) {}
            Icon(Icons.Default.Celebration, null, Modifier.size(30.dp), tint = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("Tournament Published!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Successfully published \"$title\".", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
            Text("DONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
}

@Composable
private fun ReviewRow(label: String, value: String, valueColor: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.Top) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor ?: Color.Black, textAlign = TextAlign.End)
    }
}

@Composable
private fun PickerRow(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, Modifier.size(18.dp), tint = BrandPurple); Spacer(Modifier.width(10.dp)) }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp), tint = Color.Gray)
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = BrandPurple)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 11.sp, color = Color.Gray) }
            Switch(checked = isOn, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = BrandPurple))
        }
    }
}

@Composable
private fun InfoBanner(icon: ImageVector, color: Color, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(color.copy(alpha = 0.06f), RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 1.dp), tint = color)
        Text(text, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
    }
}
