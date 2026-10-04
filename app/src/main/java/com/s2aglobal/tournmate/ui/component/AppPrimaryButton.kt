package com.s2aglobal.tournmate.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AccentGradient
import com.s2aglobal.tournmate.ui.theme.AppAccent

private val PrimaryShape = RoundedCornerShape(14.dp)
private val DisabledFill = Color.Gray.copy(alpha = 0.35f)

/**
 * iOS `.appPrimary` (AppPrimaryButtonStyle): full width, 17sp SemiBold white
 * label, 16dp vertical padding (~54dp tall), radius 14, accent gradient with a
 * soft accent glow, flat grey at 35% when disabled, 0.97 press scale.
 * For 56dp capsule CTAs (wizard NEXT, Register) use [PrimaryCapsuleButton].
 */
@Composable
fun AppPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Brush = AccentGradient,
    glow: Color = AppAccent,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(150), label = "pressScale")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (enabled) Modifier.shadow(8.dp, PrimaryShape, ambientColor = glow.copy(alpha = 0.25f), spotColor = glow.copy(alpha = 0.25f))
                else Modifier,
            )
            .clip(PrimaryShape)
            .then(if (enabled) Modifier.background(fill) else Modifier.background(DisabledFill))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
        ) { content() }
    }
}
