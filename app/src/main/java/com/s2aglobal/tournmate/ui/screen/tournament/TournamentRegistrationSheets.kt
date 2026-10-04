package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.s2aglobal.tournmate.domain.model.AgeGroup
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.AppAccentTint
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.util.DatePickerUtils
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val SheetGray6 = Color(0xFFF2F2F7)

@Composable
private fun SheetTopBar(title: String?, onCancel: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
            Text("Cancel", color = AppAccent, fontSize = 16.sp)
        }
        title?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center)) }
    }
}

/** "Date of Birth Required" prompt shown before registering for an age-restricted tournament. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DobPromptSheet(ageGroup: AgeGroup, onConfirm: (Date) -> Unit, onCancel: () -> Unit) {
    val maxDob = remember { Calendar.getInstance().apply { add(Calendar.YEAR, -5) }.time }
    var dob by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.YEAR, -18) }.time) }
    var showPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCancel, containerColor = Color.White, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SheetTopBar(null, onCancel)
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.EventAvailable, null, Modifier.size(40.dp), tint = AppAccent)
                Text("Date of Birth Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    buildAnnotatedString {
                        append("This tournament is restricted to ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ageGroup.displayName) }
                        append(". Please enter your date of birth to verify eligibility.")
                    },
                    fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SheetGray6)
                    .clickable { showPicker = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp), tint = AppAccent)
                Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(dob), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = Color.Gray)
            }

            Button(
                onClick = { onConfirm(dob) },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
            ) {
                Text("CONFIRM & CONTINUE", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }

    if (showPicker) {
        val maxMillis = DatePickerUtils.toUtcPickerMillis(maxDob)
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DatePickerUtils.toUtcPickerMillis(dob),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dob = DatePickerUtils.applyPickerDate(dob, it) }
                    showPicker = false
                }) { Text("SET DATE", color = AppAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel", color = AppAccent) } },
        ) {
            DatePicker(
                state = pickerState,
                title = { Text("DATE OF BIRTH", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            )
        }
    }
}

/** "Select Partner" sheet for doubles registration (iOS `registerPartnerSheet`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPartnerSheet(
    availablePartners: List<Player>,
    initialPartner: Player?,
    isRegistering: Boolean,
    onRegister: (Player?) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf(initialPartner) }
    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Color.White,
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // The sheet already pads (and consumes) the navigation-bar inset (material3 contentWindowInsets = BottomSheetDefaults.windowInsets).
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            SheetTopBar("Select Partner", onCancel)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(60.dp).background(AppAccentTint, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.People, null, Modifier.size(26.dp), tint = AppAccent)
                    }
                    Text("Choose a Partner", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Select a player who also needs a partner, or register solo and wait for one.",
                        fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                if (availablePartners.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Groups, null, Modifier.size(32.dp), tint = Color.Gray.copy(alpha = 0.3f))
                        Text("No available partners yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("You can register solo — someone can join your team later.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
                            .clip(RoundedCornerShape(16.dp)).background(Color.White),
                    ) {
                        availablePartners.forEachIndexed { index, player ->
                            val isSelected = selected?.id == player.id
                            Row(
                                Modifier.fillMaxWidth().clickable { selected = if (isSelected) null else player }
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(Modifier.size(36.dp), CircleShape, AppAccent.copy(alpha = 0.1f)) {
                                    AsyncImage(player.avatar.avatarUrl(72), null, Modifier.fillMaxSize())
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(player.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(player.gender.displayName, fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = AppAccent)
                            }
                            if (index < availablePartners.size - 1) HorizontalDivider(Modifier.padding(start = 64.dp), color = Color(0xFFE5E5EA))
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onRegister(selected) },
                    enabled = !isRegistering,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccent, contentColor = Color.White,
                        disabledContainerColor = AppAccent, disabledContentColor = Color.White,
                    ),
                ) {
                    if (isRegistering) {
                        TrophySpinner(size = 18.dp, style = TrophySpinnerStyle.INLINE)
                    } else {
                        Icon(sportIconPainter(CurrentSport.sport), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (selected != null) "Register with Partner" else "Register Solo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (selected == null && availablePartners.isNotEmpty()) {
                    Text("Registering solo — someone can pair with you later.", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
