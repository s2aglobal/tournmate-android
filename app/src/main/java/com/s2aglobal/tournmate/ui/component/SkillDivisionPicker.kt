package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.skillDivisions
import com.s2aglobal.tournmate.domain.model.skillRatingName
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Optional skill division for an event (pickleball DUPR, tennis NTRP). "Any" clears it. */
@Composable
fun SkillDivisionPicker(
    sport: SportType,
    selection: String?,
    onSelectionChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AppAccent,
) {
    val haptics = LocalHapticFeedback.current
    fun select(value: String?) {
        if (value != selection) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onSelectionChange(value)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DivisionChip("Any", selection == null, accent) { select(null) }
            sport.skillDivisions.forEach { division ->
                DivisionChip(division, selection == division, accent) { select(division) }
            }
        }
        Text(
            "Based on players' ${sport.skillRatingName}. Shown on the event so players pick the right level.",
            fontSize = 12.sp, color = Color.Gray,
        )
    }
}

@Composable
private fun DivisionChip(label: String, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) accent else Color(0xFFF2F2F7),
    ) {
        Box(Modifier.height(40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
        }
    }
}
