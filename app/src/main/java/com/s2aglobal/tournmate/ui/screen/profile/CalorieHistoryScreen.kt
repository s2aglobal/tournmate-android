package com.s2aglobal.tournmate.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.CalorieActivityType
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieSource
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieHistorySheet(
    records: List<CalorieRecord>,
    totalCalories: Double,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    // Group records by day
    val grouped = records.groupBy { record ->
        val cal = Calendar.getInstance().apply { time = record.date }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.time
    }.toSortedMap(compareByDescending { it })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Calorie History", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", Modifier.size(18.dp), tint = Color.Gray) }
            }

            Divider(color = Color(0xFFF2F2F7))

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text("No calorie records yet", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                    grouped.forEach { (day, dayRecords) ->
                        val dayCalories = dayRecords.sumOf { it.calories }
                        item {
                            Spacer(Modifier.height(16.dp))
                            // Day header
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text(dayFormat.format(day), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("${dayRecords.size} session${if (dayRecords.size != 1) "s" else ""}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("${dayCalories.toInt()} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        items(dayRecords) { record ->
                            RecordCard(record, dateFormat)
                            Spacer(Modifier.height(6.dp))
                        }

                        item { Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF2F2F7)) }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: CalorieRecord, timeFormat: SimpleDateFormat) {
    val isHealthConnect = record.source == CalorieSource.HEALTH_CONNECT
    val activityLabel = when (record.activityType) {
        CalorieActivityType.QUICK_PLAY -> "Quick Play"
        CalorieActivityType.TOURNAMENT -> "Tournament"
        CalorieActivityType.OPEN_PLAY -> "Open Play"
    }
    val sourceColor = if (isHealthConnect) Color(0xFFE53935) else AppAccent
    val sourceLabel = if (isHealthConnect) "Health" else "Estimated"

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(14.dp),
        Color(0xFFFAFAFA),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Fire icon
            Surface(Modifier.size(40.dp), CircleShape, WarningOrange.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(18.dp), tint = WarningOrange)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.sessionTitle ?: activityLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    // Activity type badge
                    Text(
                        activityLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        modifier = Modifier
                            .background(WarningOrange.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, Modifier.size(10.dp), tint = Color.Gray)
                    Text(timeFormat.format(record.date), fontSize = 11.sp, color = Color.Gray)
                    Text("·", fontSize = 11.sp, color = Color.Gray)
                    Icon(Icons.Default.Timer, null, Modifier.size(10.dp), tint = Color.Gray)
                    Text("${record.durationMinutes} min", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${record.calories.toInt()} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(sourceLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = sourceColor)
            }
        }
    }
}
