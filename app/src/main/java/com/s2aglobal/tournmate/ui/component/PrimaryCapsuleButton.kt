package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.s2aglobal.tournmate.ui.theme.AppAccent

/**
 * The app's primary call-to-action, matching iOS: a full-width 56pt capsule,
 * white content, and 40% opacity when disabled (iOS `.opacity(0.4)`), with no
 * elevation and no Material tonal/disabled recolouring.
 */
@Composable
fun PrimaryCapsuleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppAccent,
    height: Dp = 56.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height).alpha(if (enabled) 1f else 0.4f),
        shape = RoundedCornerShape(50),
        color = color,
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
