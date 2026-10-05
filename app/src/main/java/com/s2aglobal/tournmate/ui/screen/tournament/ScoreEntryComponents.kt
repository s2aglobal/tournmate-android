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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.ScoringConfig
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.component.ScoringDescription
import com.s2aglobal.tournmate.ui.component.SportArtworkImage
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.AppAccentDeep
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

internal val ScoreGroupedBg = Color(0xFFF2F2F7)
internal val ScoreDivider = Color(0xFFE5E5EA)
internal val ScoreDisabled = Color(0xFFD1D1D6)

/**
 * Page sheet for the score entry flows (iOS presents them with `.sheet`).
 * [content] receives a `close` action that animates the sheet away before calling [onDismiss].
 * Material3 1.4 pads the sheet content by the vertical safe-drawing (system-bar + IME) insets
 * (BottomSheetDefaults.windowInsets) and consumes them, so content must not add its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullScreenSheet(onDismiss: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScoreGroupedBg,
        dragHandle = null,
    ) {
        content(close)
    }
}

@Composable
internal fun ScoreTopBar(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(onClick = onClose, shape = CircleShape, color = ScoreGroupedBg, modifier = Modifier.size(32.dp)) {
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
internal fun ScoreFieldPair(a: String, b: String, onA: (String) -> Unit, onB: (String) -> Unit, isError: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                if (isError) 1.5.dp else 0.5.dp,
                if (isError) Color.Red.copy(alpha = 0.6f) else ScoreDivider,
                RoundedCornerShape(14.dp),
            ),
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

/** Rule violation under a game's score fields, e.g. "Must win by 2". */
@Composable
internal fun GameErrorRow(message: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = Color.Red)
        Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
    }
}

/** Sport badge + one-line scoring summary for a tournament's rules. */
@Composable
internal fun ScoringHint(sport: SportType, config: ScoringConfig, badgeSize: Dp = 18.dp, fontSize: Int = 12, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (badgeSize > 14.dp) 8.dp else 5.dp), verticalAlignment = Alignment.CenterVertically) {
        SportArtworkImage(sport, badgeSize)
        Text(
            ScoringDescription.summary(config, sport),
            fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
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
internal fun ScoreSubmitBar(
    label: String,
    enabled: Boolean,
    message: String? = null,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(color = ScoreDivider)
        message?.let {
            Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarningOrange, modifier = Modifier.padding(top = 10.dp))
        }
        submitError?.let {
            ScoreSubmitErrorBanner(it, Modifier.padding(horizontal = 20.dp).padding(top = 12.dp))
        }
        Button(
            onClick = onSubmit,
            enabled = enabled && !isSubmitting,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppAccent,
                // While saving the button keeps the accent colour (iOS keeps it too).
                disabledContainerColor = if (isSubmitting) AppAccent else ScoreDisabled,
                disabledContentColor = Color.White,
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("SAVING…", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            } else {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(13.dp))
            }
        }
    }
}

/** Inline error shown above the submit button when a score failed to save (iOS `ScoreSubmitErrorBanner`). */
@Composable
internal fun ScoreSubmitErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Red.copy(alpha = 0.08f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = Color.Red)
        Text(message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
    }
}
