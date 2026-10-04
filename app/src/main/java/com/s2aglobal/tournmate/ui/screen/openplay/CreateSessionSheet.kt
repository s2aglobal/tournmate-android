package com.s2aglobal.tournmate.ui.screen.openplay

import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos
import com.s2aglobal.tournmate.ui.component.AppPrimaryButton
import com.s2aglobal.tournmate.ui.component.FullScreenCover
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.screen.tournament.VenuePickerScreen
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.component.SportArtworkImage
import com.s2aglobal.tournmate.ui.component.LocalSportCatalog
import com.s2aglobal.tournmate.ui.component.SportPickerRow
import com.s2aglobal.tournmate.ui.theme.gearNoun
import com.s2aglobal.tournmate.ui.theme.theme
import androidx.compose.ui.draw.rotate
import com.s2aglobal.tournmate.util.DatePickerUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionSheet(
    preferredSport: SportType = SportType.BADMINTON,
    isPosting: Boolean = false,
    postError: String? = null,
    onClearPostError: () -> Unit = {},
    onPost: (
        title: String,
        venue: String,
        venueAddress: String,
        venueLatitude: Double?,
        venueLongitude: Double?,
        date: Date,
        durationMinutes: Int?,
        skillLevel: SkillLevel,
        gameType: CasualGameType,
        costPerPerson: Double?,
        currency: String,
        notes: String?,
        ageGroup: AgeGroup,
        sportType: SportType,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("Open Play") }
    var selectedVenueName by remember { mutableStateOf("") }
    var selectedVenueAddress by remember { mutableStateOf("") }
    var selectedVenueLatitude by remember { mutableStateOf<Double?>(null) }
    var selectedVenueLongitude by remember { mutableStateOf<Double?>(null) }
    var selectedDuration by remember { mutableIntStateOf(120) }
    var skillLevel by remember { mutableStateOf(SkillLevel.ALL_LEVELS) }
    var gameType by remember { mutableStateOf(CasualGameType.ANY) }
    var costText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var ageGroup by remember { mutableStateOf(AgeGroup.OPEN) }
    // Only live sports can be posted; a soon/unknown preference starts on the first live one.
    val catalog = LocalSportCatalog.current
    var sportType by remember { mutableStateOf(catalog.defaultCreationSport(preferredSport)) }
    var showVenuePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf(Date(System.currentTimeMillis() + 3_600_000L)) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // iOS: venue picker is a fullScreenCover over the sheet (the sheet stays open).
    // Its window opens after the sheet's, so it draws on top.
    if (showVenuePicker) FullScreenCover(onDismissRequest = { showVenuePicker = false }) {
        VenuePickerScreen(
            sportType = sportType,
            onVenueSelected = { name, address, lat, lng ->
                selectedVenueName = name
                selectedVenueAddress = address
                selectedVenueLatitude = lat
                selectedVenueLongitude = lng
                showVenuePicker = false
            },
            onCancel = { showVenuePicker = false },
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F2F7),
        // iOS: NavigationStack sheet with an inline title, no drag indicator.
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .sheetScrollLikeIos().verticalScroll(rememberScrollState()),
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text("Cancel", color = AppAccent, fontSize = 16.sp)
                }
                Text(
                    "Post Session",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(sportType.theme.tint),
                    contentAlignment = Alignment.Center,
                ) {
                    SportArtworkImage(sportType, 48.dp, Modifier.rotate(-12f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Post Open Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Invite others to play casual ${sportType.inlineName}", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                // Sport
                FormSection("Sport") {
                    SportPickerRow(selection = sportType, onSelect = {
                        sportType = it
                        if (ageGroup !in it.ageGroups) ageGroup = AgeGroup.OPEN
                    })
                }

                // Session Title
                FormSection("Session Title") {
                    FormTextField(value = title, onValueChange = { title = it }, placeholder = "Open Play")
                }

                // Venue
                FormSection("Venue") {
                    Surface(
                        onClick = { showVenuePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            if (selectedVenueName.isEmpty()) {
                                Text("Select a court or venue", color = Color.Gray, fontSize = 14.sp)
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedVenueName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    if (selectedVenueAddress.isNotEmpty()) {
                                        Text(selectedVenueAddress, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        "We use the venue's map location to set country and postal for discovery (same as tournaments). Set your home area in Profile.",
                        fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Date & Time
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
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.CalendarToday, null, tint = AppAccent, modifier = Modifier.size(16.dp)) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(dateFormatter.format(selectedDate), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
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
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.Schedule, null, tint = AppAccent, modifier = Modifier.size(16.dp)) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(timeFormatter.format(selectedDate), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Duration - capsule chips
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
                                color = if (isSelected) AppAccent else Color(.6f, .6f, .6f, .08f),
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
                    SegmentedPicker(SkillLevel.entries, skillLevel, { it.displayName }) { skillLevel = it }
                }

                FormSection("Game Type") {
                    SegmentedPicker(CasualGameType.entries, gameType, { it.displayName }) { gameType = it }
                }

                // Age Group dropdown
                FormSection("Preferred Age Group") {
                    AgeGroupDropdown(selected = ageGroup, options = sportType.ageGroups, onSelected = { ageGroup = it })
                }

                // Cost
                FormSection("Cost per Person (Optional)") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("$", color = AppAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = costText,
                                onValueChange = { costText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 15.sp),
                                decorationBox = { inner ->
                                    if (costText.isEmpty()) Text("0.00", color = Color.LightGray, fontSize = 15.sp)
                                    inner()
                                },
                            )
                        }
                    }
                    Text("Leave blank or 0 if free (e.g. your home court).", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }

                // Notes
                FormSection("Notes (Optional)") {
                    FormTextField(value = notes, onValueChange = { notes = it.take(500) }, placeholder = "e.g. Court 3, bring your own ${sportType.gearNoun}", singleLine = false, minHeight = 80.dp)
                }

                // Post Session button - tighter spacing
                Spacer(modifier = Modifier.height(12.dp))
                // iOS: .buttonStyle(.appPrimary)
                AppPrimaryButton(
                    onClick = {
                        onPost(
                            title, selectedVenueName, selectedVenueAddress,
                            selectedVenueLatitude, selectedVenueLongitude,
                            selectedDate,
                            if (selectedDuration == 0) null else selectedDuration,
                            skillLevel, gameType,
                            costText.toDoubleOrNull(),
                            "USD", notes.ifBlank { null },
                            ageGroup, sportType,
                        )
                    },
                    enabled = selectedVenueName.isNotBlank() && !isPosting,
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isPosting) "Posting..." else "Post Session")
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    postError?.let { msg ->
        AlertDialog(
            onDismissRequest = onClearPostError,
            title = { Text("Unable to Post") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = onClearPostError) { Text("OK") } },
        )
    }

    if (showDatePicker) {
        val todayMillis = DatePickerUtils.toUtcPickerMillis(Date())
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(selectedDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = DatePickerUtils.applyPickerDate(selectedDate, millis).coerceAtLeastNow()
                }
                showDatePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val timePickerState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE))
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = {
                val newCal = Calendar.getInstance().apply { time = selectedDate }
                newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                newCal.set(Calendar.MINUTE, timePickerState.minute)
                selectedDate = newCal.time.coerceAtLeastNow()
                showTimePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private fun Date.coerceAtLeastNow(): Date = if (before(Date())) Date() else this

/** iOS `.pickerStyle(.segmented)` look: gray track, white selected thumb. */
@Composable
internal fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF767680).copy(alpha = 0.12f), RoundedCornerShape(9.dp))
            .padding(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(7.dp),
                color = if (isSelected) Color.White else Color.Transparent,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    label(option),
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun FormSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
internal fun FormTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String,
    singleLine: Boolean = true, minHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = Color.LightGray, fontSize = 15.sp)
            }
            inner()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgeGroupDropdown(selected: AgeGroup, options: List<AgeGroup>, onSelected: (AgeGroup) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Surface(
            shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            onClick = { expanded = true },
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.displayName, fontSize = 14.sp, color = AppAccent, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { group ->
                DropdownMenuItem(
                    text = {
                        Row {
                            if (group == selected) Text("✓ ", fontWeight = FontWeight.Bold)
                            Text(group.displayName)
                        }
                    },
                    onClick = { onSelected(group); expanded = false },
                )
            }
        }
    }
}
