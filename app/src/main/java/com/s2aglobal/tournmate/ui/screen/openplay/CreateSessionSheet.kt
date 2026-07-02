package com.s2aglobal.tournmate.ui.screen.openplay

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
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionSheet(
    preferredSport: SportType = SportType.BADMINTON,
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
    var sportType by remember { mutableStateOf(preferredSport) }
    var showVenuePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
    }
    var selectedDate by remember { mutableStateOf(calendar.time) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    if (showVenuePicker) {
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
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F2F7),
        dragHandle = {
            // Minimal drag indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .verticalScroll(rememberScrollState()),
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
                    Text("Cancel", color = BrandPurple, fontSize = 16.sp)
                }
                Text(
                    "Post Session",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Sport icon matching iOS figure.badminton
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(BrandPurple.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_figure_badminton),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = BrandPurple,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Post Open Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Invite others to play casual badminton", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

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
                            Icon(Icons.Default.LocationOn, null, tint = BrandPurple, modifier = Modifier.size(20.dp))
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
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(BrandPurple.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.CalendarToday, null, tint = BrandPurple, modifier = Modifier.size(16.dp)) }
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
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(BrandPurple.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.Schedule, null, tint = BrandPurple, modifier = Modifier.size(16.dp)) }
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
                                color = if (isSelected) BrandPurple else Color(.6f, .6f, .6f, .08f),
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

                // Skill Level - individual capsule chips without gray container
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
                                border = if (isSelected) null else null,
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

                // Game Type - individual capsule chips without gray container
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

                // Age Group dropdown
                FormSection("Preferred Age Group") {
                    AgeGroupDropdown(selected = ageGroup, onSelected = { ageGroup = it })
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
                            Text("$", color = BrandPurple, fontWeight = FontWeight.Bold)
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
                    FormTextField(value = notes, onValueChange = { notes = it.take(500) }, placeholder = "e.g. Court 3, bring shuttlecocks", singleLine = false, minHeight = 80.dp)
                }

                // Post Session button - tighter spacing
                Spacer(modifier = Modifier.height(12.dp))
                Button(
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
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedVenueName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, disabledContainerColor = Color.LightGray),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Post Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                    val oldCal = Calendar.getInstance().apply { time = selectedDate }
                    newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                    newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                    selectedDate = newCal.time
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
                selectedDate = newCal.time
                showTimePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
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
internal fun AgeGroupDropdown(selected: AgeGroup, onSelected: (AgeGroup) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        Surface(
            shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            onClick = { expanded = true },
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.displayName, fontSize = 14.sp, color = BrandPurple, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AgeGroup.entries.forEach { group ->
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
