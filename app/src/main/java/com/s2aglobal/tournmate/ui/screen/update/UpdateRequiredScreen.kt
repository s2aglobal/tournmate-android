package com.s2aglobal.tournmate.ui.screen.update

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.service.config.AppUpdatePolicy
import com.s2aglobal.tournmate.service.config.AppVersion
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import com.s2aglobal.tournmate.ui.theme.TournmatePurpleDark
import com.s2aglobal.tournmate.ui.theme.DarkNavy

const val UPDATE_FALLBACK_URL = "https://www.tournmate.com/#download"

private const val DEFAULT_UPDATE_MESSAGE =
    "This version of TournMate is no longer supported. Update to keep playing, hosting, and scoring with everyone else."

/**
 * Full-screen blocker shown when the running version is below `config/app.minimumAndroidVersion`.
 * Uses fixed brand colours because it can appear before a sport is chosen.
 */
@Composable
fun UpdateRequiredScreen(
    policy: AppUpdatePolicy?,
    onUpdate: (url: String) -> Unit,
) {
    LightSystemBarIcons()
    BackHandler(enabled = true) {}

    val minimum = policy?.minimumVersion
    val current = AppVersion.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, TournmatePurpleDark))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(Brush.linearGradient(listOf(TournmatePurple, TournmatePurpleDark)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Download, null, Modifier.size(46.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text("Update Required", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                policy?.message ?: DEFAULT_UPDATE_MESSAGE,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            if (minimum != null && current != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "You have v$current · v$minimum or later required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onUpdate(policy?.storeUrl ?: UPDATE_FALLBACK_URL) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TournmatePurpleDark),
            ) {
                Icon(Icons.Filled.ArrowCircleDown, null)
                Spacer(Modifier.width(8.dp))
                Text("Update Now", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}
