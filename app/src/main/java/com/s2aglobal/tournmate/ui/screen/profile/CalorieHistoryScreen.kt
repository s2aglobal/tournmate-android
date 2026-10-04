package com.s2aglobal.tournmate.ui.screen.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.CalorieActivityType
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieSource
import com.s2aglobal.tournmate.ui.screen.player.InfoBlue
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HistoryBg = Color(0xFFF2F2F7)

private data class CalorieDayGroup(val date: Date, val records: List<CalorieRecord>) {
    val totalCalories: Double get() = records.sumOf { it.calories }
}

private fun groupByDate(records: List<CalorieRecord>): List<CalorieDayGroup> =
    records.groupBy { record ->
        Calendar.getInstance().apply {
            time = record.date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }
        .map { (day, list) -> CalorieDayGroup(day, list.sortedByDescending { it.date }) }
        .sortedByDescending { it.date }

private val CalorieActivityType.badge: String
    get() = when (this) {
        CalorieActivityType.OPEN_PLAY -> "Open Play"
        CalorieActivityType.TOURNAMENT -> "Tournament"
        CalorieActivityType.QUICK_PLAY -> "Quick Play"
    }

private val CalorieActivityType.badgeColor: Color
    get() = when (this) {
        CalorieActivityType.OPEN_PLAY -> AppAccent
        CalorieActivityType.TOURNAMENT -> WarningOrange
        CalorieActivityType.QUICK_PLAY -> InfoBlue
    }

/** Pushed "Calorie History" screen (iOS `CalorieHistoryView`). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalorieHistoryScreen(
    records: List<CalorieRecord>,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(records) { groupByDate(records) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Scaffold(
        modifier = modifier,
        containerColor = HistoryBg,
        // Hosted inside MainScreen's Scaffold, whose padding already clears the
        // status bar and bottom nav bar — don't re-apply system-bar insets here.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calorie History", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppAccent) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = HistoryBg),
                windowInsets = WindowInsets(0),
            )
        },
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppAccent)
            }
            groups.isEmpty() -> EmptyHistory(Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                groups.forEach { group ->
                    stickyHeader(key = "h_${group.date.time}") { DateHeader(group, dayFormat) }
                    items(group.records, key = { it.id }) { record -> RecordRow(record, timeFormat) }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(group: CalorieDayGroup, dayFormat: SimpleDateFormat) {
    val count = group.records.size
    Row(
        Modifier.fillMaxWidth().background(HistoryBg.copy(alpha = 0.95f)).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(dayFormat.format(group.date), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("$count session${if (count == 1) "" else "s"}", fontSize = 11.sp, color = Color.Gray)
        }
        Text("${Math.round(group.totalCalories)} kcal", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = WarningOrange)
    }
}

@Composable
private fun RecordRow(record: CalorieRecord, timeFormat: SimpleDateFormat) {
    val fromHealth = record.source == CalorieSource.HEALTH_CONNECT
    val sourceColor = if (fromHealth) Color.Red else InfoBlue
    val type = record.activityType

    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        RoundedCornerShape(14.dp),
        Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Brush.linearGradient(listOf(WarningOrange, Color.Red.copy(alpha = 0.8f))), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(20.dp), tint = Color.White)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        record.sessionTitle ?: "Session", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        type.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = type.badgeColor,
                        modifier = Modifier
                            .background(type.badgeColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaLabel(Icons.Filled.Schedule, timeFormat.format(record.date))
                    MetaLabel(Icons.Filled.Timer, "${record.durationMinutes} min")
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${Math.round(record.calories)} kcal", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (fromHealth) "Health" else "Estimated",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = sourceColor,
                    modifier = Modifier
                        .background(sourceColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MetaLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, Modifier.size(11.dp), tint = Color.Gray)
        Text(text, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(80.dp).background(WarningOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(32.dp), tint = WarningOrange.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(16.dp))
        Text("No Calorie Records Yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Join an Open Play session and log your burn to see your history here.",
            fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}
