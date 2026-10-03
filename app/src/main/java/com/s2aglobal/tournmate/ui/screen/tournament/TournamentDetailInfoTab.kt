package com.s2aglobal.tournmate.ui.screen.tournament

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.component.ScoringDescription
import com.s2aglobal.tournmate.ui.component.SportBadge
import com.s2aglobal.tournmate.ui.theme.*
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

private val PrizeGoldDark = Color(0xFFFFDE94)
private val CoachIndigo = Color(0xFF5856D6)

@Composable
internal fun InfoTabContent(
    state: TournamentDetailUiState,
    isGuest: Boolean,
    onWithdraw: () -> Unit,
    onCalorieWeightChange: (Double) -> Unit,
    onLogCalories: () -> Unit,
) {
    val tournament = state.tournament ?: return
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CoachsCornerCard()

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoGridCard(Icons.Default.CalendarToday, "DATE", DateFormat.getDateInstance(DateFormat.LONG).format(tournament.date), Modifier.weight(1f))
            InfoGridCard(Icons.Default.Schedule, "START TIME", DateFormat.getTimeInstance(DateFormat.SHORT).format(tournament.date), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoGridCard(Icons.Default.EmojiEvents, "FORMAT", tournament.matchFormat.displayName, Modifier.weight(1f))
            InfoGridCard(Icons.Default.Stadium, "EVENT TYPE", tournament.format.displayName, Modifier.weight(1f))
        }
        if (tournament.enforcesScoringRules) ScoringRulesCard(tournament)

        val division = tournament.skillDivision
        if (division != null || tournament.ageGroup != AgeGroup.OPEN) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                division?.let { InfoGridCard(Icons.Default.BarChart, "SKILL LEVEL", it, Modifier.weight(1f)) }
                if (tournament.ageGroup != AgeGroup.OPEN) {
                    InfoGridCard(Icons.Default.Badge, "AGE GROUP", tournament.ageGroup.shortName, Modifier.weight(1f))
                }
                if (division == null || tournament.ageGroup == AgeGroup.OPEN) Spacer(Modifier.weight(1f))
            }
        }

        tournament.formattedDuration?.let { duration ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoGridCard(Icons.Default.HourglassEmpty, "DURATION", duration, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }

        tournament.prizeInfo?.takeIf { it.isNotEmpty() }?.let { DetailPrizeCard(it) }

        DetailFeeCard(tournament)

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (tournament.location.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.LocationOn,
                    title = tournament.location,
                    subtitle = tournament.locationAddress,
                    actionLabel = if (tournament.locationLatitude != null) "Open in Maps" else null,
                    onAction = {
                        val lat = tournament.locationLatitude
                        val lng = tournament.locationLongitude
                        if (lat != null && lng != null) openDirections(context, tournament.location, lat, lng)
                    },
                )
            }
            DeadlineRow(tournament)
        }

        if (!isGuest) RegistrationStatusCard(state)

        state.organizerName?.let { name ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Default.VerifiedUser)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Organized by", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.canLogTournamentCalories) {
            TournamentCalorieCard(state, onCalorieWeightChange, onLogCalories)
        }

        state.statusMessage?.let {
            Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.fillMaxWidth())
        }

        if (state.canWithdraw) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onWithdraw).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Cancel, null, Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.7f))
                Spacer(Modifier.width(6.dp))
                Text("Withdraw from Tournament", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun CoachsCornerCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = CoachIndigo.copy(alpha = 0.3f), spotColor = CoachIndigo.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(CoachIndigo)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Bolt, null, Modifier.size(16.dp), tint = Color.Yellow)
            Text("COACH'S CORNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f))
        }
        Text(
            "\"Stay focused on your footwork and shot placement. Consistency wins matches!\"",
            fontSize = 14.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
            color = Color.White, lineHeight = 21.sp,
        )
    }
}

/** How matches are scored in this tournament (games, target, win-by, system). */
@Composable
private fun ScoringRulesCard(tournament: Tournament) {
    val config = tournament.scoringConfig
    val sport = tournament.sportType
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Gray.copy(alpha = 0.05f))
            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SportBadge(sport, size = 40.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SCORING", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Color.Gray)
            Text(ScoringDescription.summary(config, sport), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(ScoringDescription.detail(config, sport), fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun InfoGridCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Gray.copy(alpha = 0.05f))
            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = AppAccent)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = Color.Gray.copy(alpha = 0.5f))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, subtitle: String, actionLabel: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        IconTile(icon)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            if (actionLabel != null) {
                Row(
                    Modifier.padding(top = 4.dp).clickable(onClick = onAction),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(actionLabel.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(11.dp), tint = AppAccent)
                }
            }
        }
    }
}

@Composable
private fun DeadlineRow(tournament: Tournament) {
    val deadline = tournament.effectiveDeadline
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    var now by remember { mutableStateOf(Date()) }
    val closedAtLoad = remember(tournament.id, deadline) { tournament.isRegistrationClosed }
    if (!closedAtLoad) {
        LaunchedEffect(deadline) {
            while (true) { now = Date(); delay(1000) }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        IconTile(Icons.Default.Schedule)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Deadline: ${fmt.format(deadline)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val remaining = (deadline.time - now.time) / 1000
            when {
                closedAtLoad -> DeadlineStatus(Icons.Outlined.ErrorOutline, "Registration Closed", Color.Red)
                remaining > 0 -> DeadlineStatus(Icons.Outlined.ErrorOutline, "Closes in ${countdownText(remaining)}", WarningOrange)
                else -> DeadlineStatus(Icons.Default.Lock, "Registration just closed!", Color.Red)
            }
        }
    }
}

@Composable
private fun DeadlineStatus(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun countdownText(totalSeconds: Long): String {
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> String.format("%dd %dh %02dm %02ds", days, hours, minutes, seconds)
        hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
        else -> String.format("%02dm %02ds", minutes, seconds)
    }
}

@Composable
private fun RegistrationStatusCard(state: TournamentDetailUiState) {
    val reg = state.currentPlayerRegistration ?: return
    val tournament = state.tournament ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppAccent.copy(alpha = 0.06f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(24.dp), tint = AppAccent)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("You're registered!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppAccent)
            val partner = reg.partner
            if (partner != null) {
                Text("Partner: ${partner.name}", fontSize = 12.sp, color = Color.Gray)
            } else if (tournament.format.isDoubles) {
                Text(
                    if (tournament.randomPairing) "Partner will be auto-assigned" else "Awaiting partner",
                    fontSize = 12.sp, color = WarningOrange,
                )
            }
        }
    }
}

@Composable
private fun DetailPrizeCard(text: String) {
    val transition = rememberInfiniteTransition(label = "prize")
    val tilt by transition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOut), RepeatMode.Reverse),
        label = "trophyTilt",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(PrizeGoldLight.copy(alpha = 0.55f), PrizeGoldDark.copy(alpha = 0.35f))))
            .border(1.dp, WarningOrange.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(44.dp).background(WarningOrange.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text("🏆", fontSize = 22.sp, modifier = Modifier.rotate(tilt))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("WINNING REWARDS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = WarningOrange.copy(alpha = 0.7f))
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrizeGold, lineHeight = 21.sp)
        }
    }
}

private val knownPaymentMethods = listOf(
    "venmo" to "Venmo", "zelle" to "Zelle", "cash app" to "Cash App", "cashapp" to "Cash App",
    "paypal" to "PayPal", "cash at venue" to "Cash", "cash at court" to "Cash", "cash at the" to "Cash",
    "collect cash" to "Cash", "pay cash" to "Cash",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailFeeCard(tournament: Tournament) {
    val context = LocalContext.current
    val info = tournament.paymentInfo?.takeIf { it.isNotEmpty() }
    val methods = remember(info) {
        val lower = info?.lowercase() ?: ""
        knownPaymentMethods.filter { lower.contains(it.first) }.map { it.second }.distinct()
    }
    val handle = remember(info) { info?.let { Regex("@[\\w\\-.]+").find(it)?.value } }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD1D1D6).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("ENTRY FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Color.Gray)
            val fee = tournament.entryFee
            if (fee != null && fee > 0) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatFeeAmount(fee, tournament.currency), fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text(tournament.currency, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                }
            } else {
                Text("Free", fontSize = 36.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
            }
        }

        if (info != null) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFE5E5EA))
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (handle != null) {
                    Text("PAY TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(handle, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        CopyButton(copied) { copyToClipboard(context, handle); copied = true }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(info, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                        CopyButton(copied) { copyToClipboard(context, info); copied = true }
                    }
                }
            }
        }

        if (methods.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFE5E5EA))
            FlowRow(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methods.forEach { method ->
                    Text(
                        method, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF2F2F7))
                            .border(0.5.dp, Color(0xFFD1D1D6), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyButton(copied: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.padding(start = 8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE5E5EA)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(12.dp), tint = Color.Black)
        Text(if (copied) "Copied" else "Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatFeeAmount(fee: Double, currency: String): String = try {
    NumberFormat.getCurrencyInstance().apply {
        this.currency = Currency.getInstance(currency)
        val whole = fee % 1.0 == 0.0
        maximumFractionDigits = if (whole) 0 else 2
        minimumFractionDigits = if (whole) 0 else 2
    }.format(fee)
} catch (_: Exception) {
    "$${fee.toInt()}"
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Payment", text))
}

private fun openDirections(context: Context, name: String, lat: Double, lng: Double) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).setPackage("com.google.android.apps.maps"))
        return
    } catch (_: Exception) { }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")))
        return
    } catch (_: Exception) { }
    com.s2aglobal.tournmate.util.openInBrowser(context, "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
}

@Composable
private fun TournamentCalorieCard(
    state: TournamentDetailUiState,
    onWeightChange: (Double) -> Unit,
    onLog: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(16.dp), tint = WarningOrange)
            Text("CALORIE TRACKING", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
        }
        val record = state.existingCalorieRecord
        if (record != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(WarningOrange.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(26.dp), tint = WarningOrange)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.formattedCalories, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    val source = if (record.source == CalorieSource.HEALTH_CONNECT) "Health Connect" else "Estimated"
                    Text("$source • ${record.weightUsedKg.toInt()} kg", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(WarningOrange.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(20.dp), tint = WarningOrange)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Estimate Your Burn", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("No Health Connect data found — we’ll estimate for you", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your weight", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                Text("${state.calorieWeight.toInt()} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
            }
            Slider(
                value = state.calorieWeight.toFloat(),
                onValueChange = { onWeightChange(it.toInt().toDouble()) },
                valueRange = 30f..200f,
                colors = SliderDefaults.colors(thumbColor = WarningOrange, activeTrackColor = WarningOrange),
            )
            Button(
                onClick = onLog,
                enabled = !state.isLoggingCalories,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
            ) {
                if (state.isLoggingCalories) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (state.isLoggingCalories) "Calculating…" else "Log Calories", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
