package com.s2aglobal.tournmate.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** STANDARD = accent trophy + pulsing ring, LIGHT = white for dark backgrounds, INLINE = white, no ring (inside buttons). */
enum class TrophySpinnerStyle { STANDARD, LIGHT, INLINE }

/** The app's branded loading indicator (port of iOS `TrophySpinner`). */
@Composable
fun TrophySpinner(
    size: Dp = 36.dp,
    style: TrophySpinnerStyle = TrophySpinnerStyle.STANDARD,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val iconColor = if (style == TrophySpinnerStyle.STANDARD) AppAccent else Color.White
    val showRing = style != TrophySpinnerStyle.INLINE

    val transition = rememberInfiniteTransition(label = "trophySpinner")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (showRing) {
                Box(
                    Modifier
                        .size(size * 1.6f)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .background(iconColor.copy(alpha = 0.08f), CircleShape),
                )
            }
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(size * 0.62f)
                    .graphicsLayer {
                        val scale = 0.9f + 0.2f * bounce
                        scaleX = scale
                        scaleY = scale
                        rotationZ = -6f + 12f * bounce
                    },
            )
        }
        if (message != null) {
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (style == TrophySpinnerStyle.LIGHT) Color.White.copy(alpha = 0.7f) else Color.Gray,
            )
        }
    }
}
