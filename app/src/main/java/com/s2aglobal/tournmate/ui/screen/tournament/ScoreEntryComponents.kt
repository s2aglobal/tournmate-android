package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.AppAccentDeep
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import kotlin.math.cos
import kotlin.math.sin

internal val ScoreGroupedBg = Color(0xFFF2F2F7)
internal val ScoreDivider = Color(0xFFE5E5EA)
internal val ScoreDisabled = Color(0xFFD1D1D6)

/** Full-screen container used by the iOS-style score entry sheets. */
@Composable
internal fun FullScreenSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = ScoreGroupedBg) { content() }
    }
}

@Composable
internal fun ScoreTopBar(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(onClick = onClose, shape = CircleShape, color = Color.White, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = Color.Gray)
            }
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(32.dp))
    }
}

@Composable
internal fun MatchupHeader(teamAName: String, teamBName: String) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(AppAccent, AppAccentDeep)))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TeamIcon(AppAccent.copy(alpha = 0.7f))
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                repeat(6) { Sparkle(it) }
                Text("VS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
            }
            TeamIcon(WarningOrange.copy(alpha = 0.7f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(teamAName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("vs", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            Text(teamBName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TeamIcon(color: Color) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.People, null, Modifier.size(18.dp), tint = Color.White)
    }
}

@Composable
private fun Sparkle(index: Int) {
    val transition = rememberInfiniteTransition(label = "sparkle$index")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800 + index * 100, delayMillis = index * 150, easing = EaseInOut), RepeatMode.Reverse),
        label = "sparkleT$index",
    )
    val angle = Math.toRadians(index * 60.0)
    val radius = 8f + 8f * t
    Box(
        Modifier
            .offset(x = (cos(angle) * radius).dp, y = (sin(angle) * radius).dp)
            .size(4.dp)
            .background(Color.White.copy(alpha = 0.2f + 0.6f * t), CircleShape),
    )
}

/** Two big numeric inputs side by side (team A accent, team B orange). */
@Composable
internal fun ScoreFieldPair(a: String, b: String, onA: (String) -> Unit, onB: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(0.5.dp, ScoreDivider, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreField(a, AppAccent, onA, Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(50.dp).background(ScoreDivider))
        ScoreField(b, WarningOrange, onB, Modifier.weight(1f))
    }
}

@Composable
private fun ScoreField(value: String, color: Color, onChange: (String) -> Unit, modifier: Modifier) {
    Box(modifier.height(64.dp).background(color.copy(alpha = 0.04f)), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(color),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text("0", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, textAlign = TextAlign.Center)
                    inner()
                }
            },
        )
    }
}

@Composable
internal fun NumberBadge(text: String) {
    Box(Modifier.size(22.dp).background(AppAccent, CircleShape), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
internal fun ScoreCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun ScoreSubmitBar(label: String, enabled: Boolean, onSubmit: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding()) {
        HorizontalDivider(color = ScoreDivider)
        Button(
            onClick = onSubmit,
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppAccent, disabledContainerColor = ScoreDisabled, disabledContentColor = Color.White),
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
        }
    }
}
