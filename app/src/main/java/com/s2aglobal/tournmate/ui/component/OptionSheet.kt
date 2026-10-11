package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Look shared by option sheets (sort, team roles, player of the match), first
 * built for the Sort & filter sheet. Mirrors iOS `OptionSheetStyle` and friends
 * in `SortOptionsSheet.swift`.
 */
object OptionSheetStyle {
    val Background = Color(0xFFF2F2F7)
    val Secondary = Color(0xFF8E8E93)
    val Divider = Color(0xFFE5E5EA)
    val Selected = Color(0xFF0F172A)
    val UnselectedStroke = Color(0xFFC7C7CC)
    val Grabber = Color(0x4D3C3C43)
    val RowHeight = 52.dp
}

/**
 * Sheet chrome shared by option sheets: grey background, 28dp corners and an
 * iOS-style grabber inside the 28dp top padding (5dp from the edge, 36x5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionSheetScaffold(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = OptionSheetStyle.Background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .background(OptionSheetStyle.Grabber, CircleShape),
                )
            }
        },
        content = content,
    )
}

/** A sheet row with a radio indicator on the right (filled check when selected). */
@Composable
fun OptionSheetRow(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isEnabled: Boolean = true,
    titleColor: Color = Color.Black,
    role: Role = Role.RadioButton,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = OptionSheetStyle.RowHeight)
            .selectable(selected = isSelected, enabled = isEnabled, role = role, onClick = onClick)
            .alpha(if (isEnabled) 1f else 0.4f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = if (subtitle != null) 8.dp else 0.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = titleColor,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = OptionSheetStyle.Secondary)
            }
        }
        OptionSheetIndicator(isSelected)
    }
}

/** Filled circle with a check when selected, an empty ring otherwise. */
@Composable
fun OptionSheetIndicator(selected: Boolean) {
    if (selected) {
        Box(
            Modifier.size(22.dp).background(OptionSheetStyle.Selected, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Bold 11dp checkmark (iOS: checkmark, 11pt bold, white).
            Canvas(Modifier.size(11.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.08f, h * 0.55f)
                    lineTo(w * 0.38f, h * 0.84f)
                    lineTo(w * 0.94f, h * 0.18f)
                }
                drawPath(
                    path,
                    Color.White,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    } else {
        Box(Modifier.size(22.dp).border(1.5.dp, OptionSheetStyle.UnselectedStroke, CircleShape))
    }
}

/** White rounded card holding option rows (put [OptionSheetDivider] between them). */
@Composable
fun OptionSheetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)), content = content)
}

/** Hairline between option rows. */
@Composable
fun OptionSheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = OptionSheetStyle.Divider,
    )
}

/** "SORT BY"-style section label above an option card. */
@Composable
fun OptionSheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = OptionSheetStyle.Secondary,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
