package com.s2aglobal.tournmate.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.PickleSurround
import com.s2aglobal.tournmate.ui.theme.theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Shared sport-selection UI (iOS SportPicker.swift): compact tiles, large cards,
// the switcher pill and sheet, and the Play hero banner.

private val SystemGray6 = Color(0xFFF2F2F7)
private val SystemGray5 = Color(0xFFE5E5EA)
private val SystemGray4 = Color(0xFFD1D1D6)

/** SwiftUI `.spring(response:dampingFraction:)` equivalent. */
private fun <T> iosSpring(response: Float, damping: Float) =
    spring<T>(dampingRatio = damping, stiffness = (2 * Math.PI / response).let { (it * it).toFloat() })

// ── Press style ─────────────────────────────────────

/** Springy press-down for sport controls (no ripple), iOS `SportPressStyle`. */
fun Modifier.sportPressable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, iosSpring(0.25f, 0.7f), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
}

// ── Sport tile (compact) ────────────────────────────

/** Square tile used in a row of sports. Selected tile becomes a mini court. */
@Composable
fun SportTile(
    sport: SportType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    unselectedColor: Color = SystemGray6,
    onClick: () -> Unit,
) {
    val theme = sport.theme
    val shape = RoundedCornerShape(18.dp)
    val anim = iosSpring<Float>(0.35f, 0.7f)
    val rotation by animateFloatAsState(if (isSelected) -12f else 0f, anim, label = "tileRot")
    val ballScale by animateFloatAsState(if (isSelected) 1.08f else 1f, anim, label = "tileScale")

    Box(
        modifier
            .height(100.dp)
            .sportPressable(onClick)
            .semantics { contentDescription = sport.displayName; selected = isSelected }
            .then(if (isSelected) Modifier.shadow(10.dp, shape, ambientColor = theme.primary.copy(alpha = 0.35f), spotColor = theme.primary.copy(alpha = 0.35f)) else Modifier)
            .clip(shape)
            .then(if (isSelected) Modifier.background(theme.gradient) else Modifier.background(unselectedColor).border(1.dp, SystemGray4, shape)),
    ) {
        if (isSelected) {
            CourtLines(sport, Modifier.matchParentSize().padding(10.dp), lineColor = Color.White.copy(alpha = 0.18f), lineWidth = 1.2.dp)
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SportBadge(
                sport, 40.dp,
                Modifier
                    .rotate(rotation)
                    .scale(ballScale)
                    .shadow(if (isSelected) 6.dp else 2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f)),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                sport.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) Color.White else Color.Black,
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Box(Modifier.size(18.dp).background(theme.accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = theme.onAccent)
            }
        }
    }
}

/** A row of [SportTile]s bound to a selection, with a selection haptic. */
@Composable
fun SportPickerRow(
    selection: SportType,
    onSelect: (SportType) -> Unit,
    modifier: Modifier = Modifier,
    sports: List<SportType> = SportType.SELECTABLE,
    unselectedColor: Color = SystemGray6,
) {
    val view = LocalView.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        sports.forEach { sport ->
            SportTile(sport, selection == sport, Modifier.weight(1f), unselectedColor) {
                if (sport != selection) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onSelect(sport)
            }
        }
    }
}

// ── Sport card (large) ──────────────────────────────

/** Full-width card with court art, used in the sport switcher sheet. */
@Composable
fun SportCard(sport: SportType, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val theme = sport.theme
    val shape = RoundedCornerShape(22.dp)
    val rotation by animateFloatAsState(if (isSelected) -15f else 0f, iosSpring(0.35f, 0.75f), label = "cardRot")
    val content = if (isSelected) Color.White else Color.Black

    Box(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .sportPressable(onClick)
            .semantics { contentDescription = "${sport.displayName}. ${theme.tagline}"; selected = isSelected }
            .then(if (isSelected) Modifier.shadow(12.dp, shape, ambientColor = theme.primary.copy(alpha = 0.35f), spotColor = theme.primary.copy(alpha = 0.35f)) else Modifier)
            .clip(shape)
            .then(if (isSelected) Modifier.background(theme.gradient) else Modifier.background(Color.White))
            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) theme.accent else SystemGray5, shape),
    ) {
        CourtLines(
            sport,
            Modifier.matchParentSize().padding(start = 120.dp, top = 12.dp, bottom = 12.dp),
            lineColor = if (isSelected) Color.White.copy(alpha = 0.2f) else theme.primary.copy(alpha = 0.10f),
            lineWidth = 1.5.dp,
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sport.displayName.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = content)
                Text(theme.tagline, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = content.copy(alpha = if (isSelected) 0.85f else 0.6f))
            }
            SportBadge(
                sport, 58.dp,
                Modifier.rotate(rotation).shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f)),
            )
        }
    }
}

// ── Switcher pill ───────────────────────────────────

/** Compact "current sport ▾" control. [onDark] when drawn on a coloured surface. */
@Composable
fun SportSwitcherPill(sport: SportType, modifier: Modifier = Modifier, onDark: Boolean = false, onClick: () -> Unit) {
    val theme = sport.theme
    val fg = if (onDark) Color.White else theme.primary
    Row(
        modifier
            .sportPressable(onClick)
            .semantics { contentDescription = "Sport: ${sport.displayName}. Double tap to change." }
            .background(if (onDark) Color.White.copy(alpha = 0.18f) else theme.tint, CircleShape)
            .then(if (onDark) Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape) else Modifier)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SportBadge(sport, 18.dp)
        Text(sport.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp), tint = fg)
    }
}

// ── Switcher sheet ──────────────────────────────────

/** Bottom sheet for choosing the sport the app shows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportSwitcherSheet(current: SportType, onSelect: (SportType) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var selection by remember { mutableStateOf(current) }
    var committing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Text("What are we playing?", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Tournaments, open play, and courts will follow your sport.", fontSize = 15.sp, color = Color.Gray)
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SportType.SELECTABLE.forEach { sport ->
                    SportCard(sport, selection == sport) {
                        if (committing) return@SportCard
                        if (sport != selection) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        selection = sport
                        committing = true
                        // Let the selection animate before the sheet closes.
                        scope.launch {
                            delay(300)
                            onSelect(sport)
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

// ── Hero banner ─────────────────────────────────────

/** Sport-themed banner at the top of the Play tab: court art, bouncing ball, tagline, switcher. */
@Composable
fun SportHeroBanner(sport: SportType, onSwitch: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = sport,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(iosSpring(0.45f, 0.8f), initialScale = 0.96f) + fadeIn()) togetherWith fadeOut()
        },
        label = "heroBanner",
    ) { s -> HeroBannerContent(s, onSwitch) }
}

@Composable
private fun HeroBannerContent(sport: SportType, onSwitch: () -> Unit) {
    val theme = sport.theme
    val shape = RoundedCornerShape(24.dp)
    // 0 → 1 drives the ball's tilt and hop; replayed whenever the sport changes.
    val bounce = remember(sport) { Animatable(0f) }
    LaunchedEffect(sport) {
        delay(100)
        bounce.animateTo(1f, iosSpring(0.5f, 0.45f))
    }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(118.dp)
            .shadow(14.dp, shape, ambientColor = theme.primary.copy(alpha = 0.3f), spotColor = theme.primary.copy(alpha = 0.3f))
            .clip(shape)
            .background(theme.gradient),
    ) {
        CourtLines(
            sport,
            Modifier
                .fillMaxHeight()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset(x = 150.dp)
                .width(maxWidth - 120.dp)
                .padding(vertical = 14.dp),
            lineColor = Color.White.copy(alpha = 0.22f),
            lineWidth = 1.5.dp,
            zoneColor = if (sport == SportType.PICKLEBALL) PickleSurround.copy(alpha = 0.35f) else null,
        )

        SportBadge(
            sport, 74.dp,
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .graphicsLayer {
                    val v = bounce.value
                    rotationZ = 10f + (-30f * v)
                    translationY = (4f + (-10f * v)) * density
                    shadowElevation = (6f + 8f * v) * density
                    this.shape = CircleShape
                },
        )

        Column(
            Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SportSwitcherPill(sport, onDark = true, onClick = onSwitch)
            Text(
                theme.tagline,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 210.dp),
            )
        }
    }
}
