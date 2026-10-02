package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.domain.model.TournamentStatus
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.ErrorRed
import com.s2aglobal.tournmate.ui.theme.PrizeGold
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TournamentCard(
    tournament: Tournament,
    isPast: Boolean,
    modifier: Modifier = Modifier,
) {
    val (statusText, statusColor) = tournamentStatusBadge(tournament, isPast)
    val accentColor = statusColor

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .padding(vertical = 30.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Row 1: Status badges + date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Badge(statusText.uppercase(), statusColor)
                        Badge(tournament.format.shortName, Color.Gray)
                        Badge(tournament.matchFormat.displayName, AppAccent)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SportBadge(tournament.sportType, 16.dp)
                        Text(
                            text = formatDateShort(tournament.date),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                        )
                    }
                }

                // Row 2: Title
                Text(
                    text = tournament.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) Color.Gray else Color.Black,
                    maxLines = 2,
                )

                // Row 3: Location
                if (tournament.location.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray.copy(alpha = 0.5f),
                        )
                        Column {
                            Text(
                                text = tournament.location,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                            if (tournament.locationAddress.isNotEmpty()) {
                                Text(
                                    text = tournament.locationAddress,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Prize banner
                tournament.prizeInfo?.takeIf { it.isNotEmpty() }?.let { prize ->
                    PrizeBanner(prize)
                }

                Divider(color = Color(0xFFF2F2F7), modifier = Modifier.padding(vertical = 4.dp))

                // Row 4: Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray,
                        )
                        Text(
                            text = cardFooterText(tournament, isPast),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fee = tournament.formattedFee
                        if (fee != null) {
                            FeeBadge(fee, isFree = false)
                        } else {
                            FeeBadge("Free", isFree = true)
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun FeeBadge(text: String, isFree: Boolean) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isFree) AppAccent else Color.Black,
        modifier = Modifier
            .background(
                if (isFree) AppAccent.copy(alpha = 0.1f) else Color(0xFFF2F2F7),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun PrizeBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                PrizeGold.copy(alpha = 0.08f),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "🏆", fontSize = 18.sp)
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrizeGold,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun tournamentStatusBadge(t: Tournament, isPast: Boolean): Pair<String, Color> {
    if (t.status == TournamentStatus.CANCELLED) return "Cancelled" to ErrorRed
    if (isPast) return "Completed" to Color.Gray
    val calendar = Calendar.getInstance()
    val tCal = Calendar.getInstance().apply { time = t.date }
    if (calendar.get(Calendar.YEAR) == tCal.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == tCal.get(Calendar.DAY_OF_YEAR) ||
        t.isRegistrationClosed
    ) {
        return "Live" to SuccessGreen
    }
    return "Upcoming" to WarningOrange
}

private fun cardFooterText(t: Tournament, isPast: Boolean): String {
    if (isPast) return "Completed"
    val deadline = t.effectiveDeadline
    if (deadline.before(Date(Long.MAX_VALUE - 1))) {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return "Register by ${fmt.format(deadline)}"
    }
    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return fmt.format(t.date)
}

private fun formatDateShort(date: Date): String {
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
    return fmt.format(date)
}
