package com.s2aglobal.tournmate.ui.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Orange = Color(0xFFFF9500)
private val SystemGreen = Color(0xFF34C759)
private val SystemGray6 = Color(0xFFF2F2F7)

@Composable
fun EmailVerificationScreen(
    email: String,
    onCheckVerified: () -> Unit,
    onResend: suspend () -> Result<Unit>,
    onSignOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var resendCooldown by remember { mutableIntStateOf(0) }
    var showResendConfirmation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        appeared = true
    }
    LaunchedEffect(resendCooldown > 0) {
        while (resendCooldown > 0) {
            delay(1_000)
            resendCooldown -= 1
        }
    }
    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(2_000)
            isChecking = false
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "appear",
    )
    val fade = Modifier.graphicsLayer { alpha = progress.coerceIn(0f, 1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 32.dp)) {
                Box(
                    Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * progress
                            scaleX = s; scaleY = s; alpha = progress.coerceIn(0f, 1f)
                        }
                        .background(TournmatePurple.copy(alpha = 0.08f), CircleShape),
                )
                Icon(
                    Icons.Filled.MarkEmailUnread, null,
                    tint = TournmatePurple,
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            val s = 0.3f + 0.7f * progress
                            scaleX = s; scaleY = s; alpha = progress.coerceIn(0f, 1f)
                        },
                )
            }

            Text(
                "Verify Your Email",
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                modifier = fade.graphicsLayer { translationY = (1f - progress) * 10.dp.toPx() },
            )
            Text(
                "We sent a verification link to",
                fontSize = 15.sp, color = Color.Gray,
                modifier = fade.padding(top = 12.dp),
            )
            Text(
                email,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black,
                modifier = fade.padding(top = 4.dp),
            )
            Text(
                "Open the link in the email, then tap the button below.",
                fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center,
                modifier = fade.padding(horizontal = 40.dp).padding(top = 12.dp),
            )

            Row(
                modifier = fade
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .background(Orange.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Error, null, tint = Orange, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        append("Check your ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Spam / Junk") }
                        append(" folder if you don't see the email in your inbox.")
                    },
                    fontSize = 11.sp, color = Color.Gray,
                )
            }

            AnimatedVisibility(visible = errorMessage != null) {
                VerificationBanner(Icons.Filled.Warning, Color.Red, errorMessage.orEmpty(), Modifier.padding(top = 20.dp))
            }
            AnimatedVisibility(visible = showResendConfirmation) {
                VerificationBanner(Icons.Filled.CheckCircle, SystemGreen, "Verification email sent!", Modifier.padding(top = 20.dp))
            }

            Button(
                onClick = {
                    isChecking = true
                    onCheckVerified()
                },
                enabled = !isChecking,
                modifier = fade
                    .padding(horizontal = 24.dp)
                    .padding(top = 36.dp)
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TournmatePurple,
                    disabledContainerColor = TournmatePurple,
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isChecking) {
                    TrophySpinner(size = 18.dp, style = TrophySpinnerStyle.INLINE)
                    Spacer(Modifier.width(8.dp))
                }
                Text("I've Verified My Email", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            TextButton(
                onClick = {
                    scope.launch {
                        errorMessage = null
                        showResendConfirmation = false
                        onResend()
                            .onSuccess {
                                showResendConfirmation = true
                                resendCooldown = 60
                            }
                            .onFailure { errorMessage = it.localizedMessage ?: "Failed to send email." }
                    }
                },
                enabled = resendCooldown == 0,
                modifier = fade.padding(top = 16.dp),
            ) {
                if (resendCooldown > 0) {
                    Text("Resend in ${resendCooldown}s", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                } else {
                    Text("Resend Verification Email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TournmatePurple)
                }
            }

            Column(
                modifier = fade
                    .graphicsLayer { translationY = (1f - progress) * 15.dp.toPx() }
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp)
                    .fillMaxWidth()
                    .background(SystemGray6, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepRow("1", "Open your email inbox (check Spam too)")
                StepRow("2", "Find the email from noreply@ and tap the link")
                StepRow("3", "Come back here and tap \"I've Verified\"")
            }

            Spacer(Modifier.height(40.dp))
        }

        TextButton(
            onClick = onSignOut,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 30.dp),
        ) {
            Text(
                buildAnnotatedString {
                    append("Use a different account? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Sign Out") }
                },
                fontSize = 13.sp, color = TournmatePurple,
            )
        }
    }
}

@Composable
private fun VerificationBanner(icon: ImageVector, color: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .background(TournmatePurple, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = Color.Black)
    }
}
