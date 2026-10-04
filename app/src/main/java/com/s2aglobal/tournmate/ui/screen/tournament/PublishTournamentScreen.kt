package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import com.s2aglobal.tournmate.ui.component.PrimaryCapsuleButton
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.s2aglobal.tournmate.ui.component.ScoringConfigEditor
import com.s2aglobal.tournmate.ui.component.ScoringDescription
import com.s2aglobal.tournmate.ui.component.SkillDivisionPicker
import com.s2aglobal.tournmate.ui.component.SportPickerRow
import com.s2aglobal.tournmate.ui.theme.theme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.s2aglobal.tournmate.ui.component.MapPinCircle
import com.s2aglobal.tournmate.ui.theme.AppAccent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.s2aglobal.tournmate.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.validation.InputValidator
import com.s2aglobal.tournmate.util.DatePickerUtils
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


private data class MatchFormatItem(
    val format: MatchFormat,
    val icon: ImageVector,
    val tagline: String,
)

private val LocalHostedSport = staticCompositionLocalOf { SportType.BADMINTON }

private val WizardAccent: Color
    @Composable @ReadOnlyComposable get() = LocalHostedSport.current.theme.primary

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
        scoringConfig: ScoringConfig, skillDivision: String?,
        onResult: (Boolean) -> Unit,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var wizardStep by remember { mutableIntStateOf(1) }
    var showSuccess by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

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
    var newScoringConfig by remember { mutableStateOf(preferredSport.scoringRules.defaultConfig) }
    var newSkillDivision by remember { mutableStateOf<String?>(null) }
    // Scoring as it will be saved (round robin is always a single game).
    val reviewScoringConfig = if (newMatchFormat == MatchFormat.ROUND_ROBIN) newScoringConfig.copy(gamesPerMatch = 1) else newScoringConfig

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

    // Wizard accent follows the sport being hosted (iOS `accentGreen`).
    val hostedTheme = newSportType.theme
    val hostedScheme = MaterialTheme.colorScheme.copy(
        primary = hostedTheme.primary,
        secondary = hostedTheme.primaryDeep,
        primaryContainer = hostedTheme.primary.copy(alpha = 0.15f),
    )
    CompositionLocalProvider(LocalHostedSport provides newSportType) {
    MaterialTheme(colorScheme = hostedScheme, typography = MaterialTheme.typography) {
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
        val todayMillis = DatePickerUtils.toUtcPickerMillis(Date())
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(newDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        newDate = DatePickerUtils.applyPickerDate(newDate, millis)
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
                        val now = Date()
                        newDate = if (isSameDay(cal.time, now) && cal.time.before(now)) now else cal.time
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WizardAccent),
                ) { Text("SET TIME") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }

    // Deadline date picker
    if (showDeadlineDatePicker) {
        val todayMillis = DatePickerUtils.toUtcPickerMillis(Date())
        val maxMillis = DatePickerUtils.toUtcPickerMillis(newDate)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(newDeadline),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in todayMillis..maxMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        newDeadline = DatePickerUtils.applyPickerDate(newDeadline, millis)
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
                        val now = Date()
                        var picked = cal.time
                        if (isSameDay(picked, now) && picked.before(now)) picked = now
                        if (isSameDay(picked, newDate) && picked.after(newDate)) picked = newDate
                        newDeadline = picked
                        showDeadlineTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WizardAccent),
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
            items = newSportType.ageGroups,
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
            SuccessScreen(
                title = newTitle,
                onDone = {
                    Toast.makeText(context, "Tournament published!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
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
                            sportType = newSportType,
                            onSportChange = {
                                newSportType = it
                                newScoringConfig = it.scoringRules.defaultConfig
                                newSkillDivision = null
                                if (newAgeGroup !in it.ageGroups) newAgeGroup = AgeGroup.OPEN
                            },
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
                            skillDivision = newSkillDivision, onSkillDivisionChange = { newSkillDivision = it },
                            sportType = newSportType,
                            scoringConfig = newScoringConfig, onScoringConfigChange = { newScoringConfig = it },
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
                            formatConfig = newFormatConfig, scoringConfig = reviewScoringConfig, venueName = venueName,
                            date = newDate, deadline = newDeadline,
                            ageGroup = newAgeGroup, skillDivision = newSkillDivision, entryFee = newEntryFee,
                            currency = newCurrency, prizeInfo = newPrizeInfo,
                            randomPairing = newRandomPairing, dateFmt = dateFmt, timeFmt = timeFmt,
                        )
                    }
                }

                WizardBottomButton(
                    step = wizardStep, disabled = nextDisabled || isPublishing,
                    isPublishing = isPublishing,
                    onNext = { wizardStep += 1 },
                    onPublish = {
                        isPublishing = true
                        onPublish(
                            newTitle, newDate, venueName, venueAddress, venueLatitude, venueLongitude,
                            newFormat, newMatchFormat, newFormatConfig,
                            if (newFormat.isDoubles) newRandomPairing else false,
                            newDeadline, firebaseUid,
                            newEntryFee.toDoubleOrNull(), newCurrency,
                            newPaymentInfo.ifBlank { null }, newPrizeInfo.ifBlank { null },
                            newDurationMinutes.toIntOrNull(), newAgeGroup, newSportType,
                            reviewScoringConfig, newSkillDivision,
                        ) { ok ->
                            isPublishing = false
                            if (ok) showSuccess = true
                        }
                    },
                )
            }
        }
    }

    // Venue picker renders as a fullscreen overlay on top of the wizard
    if (showVenueSheet) {
        VenuePickerScreen(
            sportType = newSportType,
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
                        color = if (isSelected) WizardAccent else Color.Black,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Surface(Modifier.size(24.dp), CircleShape, WizardAccent) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
            Surface(Modifier.weight(1f).height(4.dp), RoundedCornerShape(50), if (i < step) WizardAccent else Color.Gray.copy(alpha = 0.15f)) {}
        }
    }
}

@Composable
private fun WizardBottomButton(step: Int, disabled: Boolean, isPublishing: Boolean, onNext: () -> Unit, onPublish: () -> Unit) {
    // iOS: a plain floating capsule — no footer background, divider, or shadow.
    Box(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)) {
        PrimaryCapsuleButton(
            onClick = if (step < 4) onNext else onPublish,
            enabled = !disabled,
            color = WizardAccent,
        ) {
            if (step < 4) {
                Text("NEXT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
            } else {
                if (isPublishing) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("PUBLISH", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
    SportPickerRow(selection = sportType, onSelect = onSportChange)

    SectionLabel("TOURNAMENT NAME")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WizardTextField(
            value = title, onValueChange = onTitleChange, placeholder = "Saturday's Open",
            isError = titleError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        titleError?.let { ErrorRow(it) }
    }

    SectionLabel("EVENT TYPE")
    PickerRow(text = format.displayName, onClick = onEventTypeClick)

    if (format.isDoubles) {
        ToggleRow(Icons.Default.Shuffle, "Random Pairing", "System assigns partners after deadline", randomPairing, onRandomPairingChange)
    }

    SectionLabel("LOCATION & TIME")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onVenueClick, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), color = WizardFieldColor,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(Modifier.size(40.dp), CircleShape, WizardAccent.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { MapPinCircle(size = 20.dp, color = WizardAccent) }
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(onClick = onDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = WizardFieldColor) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = IndigoTint)
                    Spacer(Modifier.width(10.dp))
                    Text(dateFmt.format(date), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Surface(onClick = onTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = WizardFieldColor) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(16.dp), tint = IndigoTint)
                    Spacer(Modifier.width(10.dp))
                    Text(timeFmt.format(date), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (dateIsTooSoon) {
            Text("Tournament must be at least 3 hours from now.", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("ESTIMATED DURATION")
            WizardTextField(
                value = durationMinutes, onValueChange = onDurationChange, placeholder = "e.g. 120",
                leadingIcon = Icons.Default.HourglassEmpty, leadingTint = IndigoTint,
                suffix = "minutes",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                fontSize = 14,
            )
        }
    }
}

// ── Step 2 ───────────────────────────────────────────

@Composable
private fun Step2FormatRules(
    matchFormat: MatchFormat, onMatchFormatChange: (MatchFormat) -> Unit,
    ageGroup: AgeGroup, onAgeGroupClick: () -> Unit,
    skillDivision: String?, onSkillDivisionChange: (String?) -> Unit,
    sportType: SportType,
    scoringConfig: ScoringConfig, onScoringConfigChange: (ScoringConfig) -> Unit,
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
                        color = if (sel) WizardAccent.copy(alpha = 0.1f) else Color(0xFFF2F2F7),
                        border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(WizardAccent), width = 1.5.dp) else null,
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(item.icon, null, Modifier.size(24.dp), tint = if (sel) WizardAccent else Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text(item.format.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) WizardAccent else Color.Black, textAlign = TextAlign.Center)
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
    PickerRow(text = ageGroup.displayName, icon = Icons.Default.Badge, trailing = Icons.Default.ChevronRight, onClick = onAgeGroupClick)

    if (sportType.skillDivisions.isNotEmpty()) {
        SectionLabel("SKILL LEVEL (OPTIONAL)")
        SkillDivisionPicker(sport = sportType, selection = skillDivision, onSelectionChange = onSkillDivisionChange, accent = WizardAccent)
    }

    SectionLabel("SCORING")
    ScoringConfigEditor(
        sport = sportType,
        config = scoringConfig,
        onConfigChange = onScoringConfigChange,
        accent = WizardAccent,
        singleGameOnly = matchFormat == MatchFormat.ROUND_ROBIN,
    )

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
            InfoBanner(Icons.Default.Info, WizardAccent, "Players must lose twice to be eliminated. A losers bracket runs alongside the main bracket.")
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
            SectionLabel("TIE-BREAKER")
            TieBreakerPicker(config.tieBreaker) { onChange(config.copy(tieBreaker = it)) }
        }
        MatchFormat.GROUP_KNOCKOUT -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Groups", config.groupCount, 2..8, Modifier.weight(1f)) { onChange(config.copy(groupCount = it)) }
                StepperField("Per Group", config.teamsPerGroup, 3..8, Modifier.weight(1f)) { onChange(config.copy(teamsPerGroup = it)) }
            }
            StepperField("Advance Per Group", config.advancingPerGroup, 1..4, Modifier.fillMaxWidth()) { onChange(config.copy(advancingPerGroup = it)) }
            SectionLabel("GROUP TIE-BREAKER")
            TieBreakerPicker(config.groupTieBreaker) { onChange(config.copy(groupTieBreaker = it)) }
            InfoBanner(Icons.Default.Info, WizardAccent, "Top finishers from each group advance to a single-elimination knockout stage.")
        }
        MatchFormat.SWISS -> {
            StepperField("Number of Rounds", config.swissRounds, 3..10, Modifier.fillMaxWidth()) { onChange(config.copy(swissRounds = it)) }
            InfoBanner(Icons.Default.Info, WizardAccent, "Each round, players with similar records are paired against each other. No one is eliminated.")
        }
        MatchFormat.MANUAL_DRAW -> {
            InfoBanner(Icons.Default.Draw, WizardAccent, "You will manually assign all matchups after registration closes. Full control over the draw.")
        }
    }

    Spacer(Modifier.height(4.dp))
    SectionLabel("MAX PARTICIPANTS (OPTIONAL)")
    WizardTextField(
        value = config.maxParticipants?.toString() ?: "",
        onValueChange = { onChange(config.copy(maxParticipants = it.toIntOrNull())) },
        placeholder = "Unlimited",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                color = if (sel) WizardAccent.copy(alpha = 0.15f) else Color(0xFFF2F2F7),
                border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(WizardAccent), width = 1.dp) else null,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    color = if (sel) WizardAccent else Color.Black, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TieBreakerPicker(selectedRaw: String, onSelect: (String) -> Unit) {
    val options = listOf(
        TieBreaker.HEAD_TO_HEAD to "Head-to-Head",
        TieBreaker.POINT_DIFF to "Point Differential",
        TieBreaker.GAMES_WON to "Games Won",
    )
    val current = TieBreaker.fromRawValue(selectedRaw)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (tb, label) ->
            val sel = current == tb
            Surface(
                onClick = { onSelect(tb.rawValue) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                color = if (sel) WizardAccent.copy(alpha = 0.15f) else WizardFieldColor,
                border = if (sel) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(WizardAccent), width = 1.dp) else null,
            ) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1,
                    color = if (sel) WizardAccent else Color.Black, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth())
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
    Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onCurrencyClick, shape = RoundedCornerShape(14.dp), color = WizardFieldColor, modifier = Modifier.fillMaxHeight()) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(currency, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp), tint = Color.Gray)
            }
        }
        WizardTextField(
            value = entryFee, onValueChange = onEntryFeeChange, placeholder = "0 = Free",
            prefix = symbol, fontWeight = FontWeight.Bold,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }

    val hasFee = (entryFee.toDoubleOrNull() ?: 0.0) > 0
    if (hasFee) {
        SectionLabel("PAYMENT INSTRUCTION")
        WizardTextField(
            value = paymentInfo, onValueChange = onPaymentInfoChange,
            placeholder = "e.g. Venmo: @handle, or Pay cash at venue",
            singleLine = false, minLines = 2, fontSize = 14,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        InfoBanner(
            Icons.Default.GppMaybe, Color(0xFFFF9800),
            "TournMate does not process payments. You are responsible for collecting and refunding fees directly.",
            background = Color(0xFFFFCC00).copy(alpha = 0.08f),
        )

        SectionLabel("WINNING REWARDS")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WizardTextField(
                value = prizeInfo, onValueChange = onPrizeInfoChange,
                placeholder = "e.g. 1st: \$150, 2nd: \$50, 3rd: Free entry next event",
                singleLine = false, minLines = 3, fontSize = 14,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Text("Describe what winners receive. This will be shown on the tournament card.", fontSize = 11.sp, color = Color.Gray)
            InputValidator.validatePrizeInfo(prizeInfo).errorMessage?.let { ErrorRow(it) }
        }
    }

    SectionLabel("REGISTRATION DEADLINE")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = onDeadlineDateClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = WizardFieldColor) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WatchLater, null, Modifier.size(16.dp), tint = AppAccent.copy(alpha = 0.7f))
                Spacer(Modifier.width(10.dp))
                Text(dateFmt.format(deadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Surface(onClick = onDeadlineTimeClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = WizardFieldColor) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(timeFmt.format(deadline), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    if (deadlineIsPast) Text("Registration deadline must be in the future.", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = WizardAccent)
        Text(
            "Players must register before this time. Defaults to the night before, or 1 hour before start for same-day events.",
            fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp,
        )
    }
}

// ── Step 4 ───────────────────────────────────────────

@Composable
private fun Step4Review(
    sportType: SportType, title: String, format: TournamentFormat, matchFormat: MatchFormat,
    formatConfig: FormatConfig, scoringConfig: ScoringConfig, venueName: String, date: Date, deadline: Date,
    ageGroup: AgeGroup, skillDivision: String?, entryFee: String, currency: String, prizeInfo: String,
    randomPairing: Boolean, dateFmt: SimpleDateFormat, timeFmt: SimpleDateFormat,
) {
    val symbol = commonCurrencies.firstOrNull { it.first == currency }?.second ?: currency
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = WizardAccent.copy(alpha = 0.05f),
    ) {
        Column(Modifier.padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Surface(Modifier.size(50.dp), CircleShape, WizardAccent.copy(alpha = 0.12f)) {}
                Surface(Modifier.size(26.dp), CircleShape, WizardAccent) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(15.dp), tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("CONFIRM DETAILS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("EVERYTHING LOOKS GREAT!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    ReviewRow("SPORT", sportType.displayName); ReviewRow("TOURNAMENT", title)
    ReviewRow("EVENT TYPE", format.displayName); ReviewRow("FORMAT", matchFormat.displayName)
    ReviewRow("SCORING", ScoringDescription.summary(scoringConfig, sportType))
    ReviewRow("SEEDING", seedingLabel(SeedingMode.fromRawValue(formatConfig.seedingMode)))
    FormatRulesReviewRows(matchFormat, formatConfig)
    ReviewRow("VENUE", venueName)
    ReviewRow("DATE", "${dateFmt.format(date)}, ${timeFmt.format(date)}")
    ReviewRow("DEADLINE", "${dateFmt.format(deadline)}, ${timeFmt.format(deadline)}")
    if (ageGroup != AgeGroup.OPEN) ReviewRow("AGE GROUP", ageGroup.displayName)
    skillDivision?.let { ReviewRow("SKILL LEVEL", it) }
    val fee = entryFee.toDoubleOrNull()
    if (fee != null && fee > 0) ReviewRow("ENTRY FEE", "$symbol${String.format("%.2f", fee)}", WizardAccent) else ReviewRow("ENTRY FEE", "Free", WizardAccent)
    if (prizeInfo.isNotEmpty()) ReviewRow("PRIZES", prizeInfo, Color(0xFFFF9800))
    if (format.isDoubles && randomPairing) ReviewRow("PAIRING", "Random Assignment")
    formatConfig.maxParticipants?.let { ReviewRow("MAX PLAYERS", "$it") }
    Spacer(Modifier.height(8.dp))
    InfoBanner(Icons.Default.Info, Color(0xFFFF9800).copy(alpha = 0.7f), "Once published, players will be able to see and register for this tournament immediately.")
}

@Composable
private fun FormatRulesReviewRows(matchFormat: MatchFormat, config: FormatConfig) {
    when (matchFormat) {
        MatchFormat.SINGLE_ELIMINATION -> {
            if (config.bronzeMatch) ReviewRow("BRONZE MATCH", "Yes")
            if (config.consolationBracket) ReviewRow("CONSOLATION", "Yes")
        }
        MatchFormat.DOUBLE_ELIMINATION -> ReviewRow("BRACKET", "Winners + Losers")
        MatchFormat.ROUND_ROBIN -> {
            ReviewRow("POINTS", "W:${config.pointsPerWin} D:${config.pointsPerDraw} L:${config.pointsPerLoss}")
            ReviewRow("TIE-BREAKER", tieBreakerLabel(TieBreaker.fromRawValue(config.tieBreaker)))
            if (config.doubleRoundRobin) ReviewRow("DOUBLE RR", "Yes")
        }
        MatchFormat.GROUP_KNOCKOUT -> {
            ReviewRow("GROUPS", "${config.groupCount} groups of ${config.teamsPerGroup}")
            ReviewRow("ADVANCE", "${config.advancingPerGroup} per group")
        }
        MatchFormat.SWISS -> ReviewRow("SWISS ROUNDS", "${config.swissRounds}")
        MatchFormat.MANUAL_DRAW -> ReviewRow("DRAW", "Manual by organizer")
    }
}

private fun seedingLabel(mode: SeedingMode) = when (mode) {
    SeedingMode.ELO_RANKED -> "Elo Ranked"
    SeedingMode.RANDOM -> "Random"
    SeedingMode.MANUAL -> "Manual"
}

private fun tieBreakerLabel(tb: TieBreaker) = when (tb) {
    TieBreaker.HEAD_TO_HEAD -> "Head-to-Head"
    TieBreaker.POINT_DIFF -> "Point Differential"
    TieBreaker.GAMES_WON -> "Games Won"
}

private fun isSameDay(a: Date, b: Date): Boolean {
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

// ── Shared Components ────────────────────────────────

private val WizardFieldColor = Color(0xFFF2F2F7)
private val IndigoTint = Color(0xFF5856D6).copy(alpha = 0.6f)

/** Filled systemGray6 field (iOS wizard TextField style). */
@Composable
private fun WizardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = Color.Gray,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.Medium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val shape = RoundedCornerShape(14.dp)
    val style = TextStyle(fontSize = fontSize.sp, fontWeight = fontWeight, color = Color.Black)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxOf(minLines, 5),
        textStyle = style,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(WizardAccent),
        modifier = modifier
            .fillMaxWidth()
            .background(WizardFieldColor, shape)
            .border(1.dp, if (isError) Color.Red.copy(alpha = 0.5f) else Color.Transparent, shape),
        decorationBox = { inner ->
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = if (singleLine) 16.dp else 14.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leadingIcon?.let { Icon(it, null, Modifier.size(16.dp), tint = leadingTint) }
                prefix?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = Color.Gray.copy(alpha = 0.6f)))
                    inner()
                }
                suffix?.let { Text(it, fontSize = 13.sp, color = Color.Gray) }
            }
        },
    )
}

@Composable
private fun ErrorRow(message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(Icons.Default.Warning, null, Modifier.size(10.dp), tint = Color.Red)
        Text(message, fontSize = 12.sp, color = Color.Red)
    }
}

@Composable
private fun SuccessScreen(title: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.size(100.dp), CircleShape, WizardAccent.copy(alpha = 0.15f)) {}
            Surface(Modifier.size(72.dp), CircleShape, WizardAccent) {}
            Icon(Icons.Default.Celebration, null, Modifier.size(30.dp), tint = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("Tournament Published!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Successfully published \"$title\".", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WizardAccent)) {
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
private fun PickerRow(text: String, icon: ImageVector? = null, trailing: ImageVector = Icons.Default.KeyboardArrowDown, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = WizardFieldColor) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, Modifier.size(18.dp), tint = WizardAccent); Spacer(Modifier.width(10.dp)) }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(trailing, null, Modifier.size(16.dp), tint = Color.Gray)
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF2F2F7), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = WizardAccent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 11.sp, color = Color.Gray) }
            Switch(checked = isOn, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = WizardAccent))
        }
    }
}

@Composable
private fun InfoBanner(icon: ImageVector, color: Color, text: String, background: Color = color.copy(alpha = 0.06f)) {
    Row(
        modifier = Modifier.fillMaxWidth().background(background, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 1.dp), tint = color)
        Text(text, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
    }
}
