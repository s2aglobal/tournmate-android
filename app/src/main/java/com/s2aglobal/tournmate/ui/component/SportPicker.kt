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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.s2aglobal.tournmate.service.config.SportCatalogState
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.PickleSurround
import com.s2aglobal.tournmate.ui.theme.theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Shared sport-selection UI (iOS SportPicker.swift): compact tiles, the switcher pill,
// the "Your sport" catalog picker sheet, and the Play hero banner.

private val SystemGray6 = Color(0xFFF2F2F7)
private val SystemGray5 = Color(0xFFE5E5EA)
private val SystemGray4 = Color(0xFFD1D1D6)

/** The resolved sport catalog, provided from `SportCatalog.state` in MainActivity. */
val LocalSportCatalog = compositionLocalOf { SportCatalogState.DEFAULT }

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
            SportArtworkImage(
                sport, 40.dp,
                Modifier
                    .rotate(rotation)
                    .scale(ballScale)
                    .shadow(if (isSelected) 6.dp else 2.dp, SportArtworkShape, ambientColor = Color.Black.copy(alpha = 0.25f)),
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

/**
 * A row of [SportTile]s bound to a selection, with a selection haptic. Defaults to the catalog's
 * live sports; more than three scroll horizontally at a fixed tile width.
 */
@Composable
fun SportPickerRow(
    selection: SportType,
    onSelect: (SportType) -> Unit,
    modifier: Modifier = Modifier,
    sports: List<SportType> = LocalSportCatalog.current.liveSports,
    unselectedColor: Color = SystemGray6,
) {
    val view = LocalView.current
    val select = { sport: SportType ->
        if (sport != selection) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onSelect(sport)
    }
    if (sports.size <= 3) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sports.forEach { sport ->
                SportTile(sport, selection == sport, Modifier.weight(1f), unselectedColor) { select(sport) }
            }
        }
    } else {
        Row(
            modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            sports.forEach { sport ->
                SportTile(sport, selection == sport, Modifier.width(104.dp), unselectedColor) { select(sport) }
            }
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
        SportArtworkImage(sport, 18.dp)
        Text(sport.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp), tint = fg)
    }
}

// ── Current sport row ───────────────────────────────

/** "[ball] Badminton      Change ›" row that opens [SportPickerSheet] (Profile, profile setup). */
@Composable
fun SportChangeRow(sport: SportType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val theme = sport.theme
    Row(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "Change sport", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Sport: ${sport.displayName}. Change" }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SportArtworkImage(sport, 38.dp)
        Spacer(Modifier.width(12.dp))
        Text(sport.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
        Text("Change", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = theme.primary)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = theme.primary)
    }
}

// ── Sport picker sheet ──────────────────────────────

/** Grouped-background grey behind the white tiles (iOS systemGroupedBackground). */
private val SheetBackground = SystemGray6
/** iOS-matched dark capsule for the Done button. */
private val DoneNavy = Color(0xFF0F172A)
private val SecondaryGrey = Color(0xFF8E8E93)

/**
 * "Your sport" picker (spec: scalable picker, single-select). Sections come from the server-driven
 * catalog; live sports are selectable, the rest show as dimmed "SOON" tiles. Tapping a live tile
 * applies it right away through [onSelect]; Done closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportPickerSheet(current: SportType, onSelect: (SportType) -> Unit, onDismiss: () -> Unit) {
    val catalog = LocalSportCatalog.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val focus = LocalFocusManager.current
    var selection by remember { mutableStateOf(current) }
    var query by remember { mutableStateOf("") }

    val trimmed = query.trim()
    val sections = remember(catalog, trimmed) {
        catalog.categories.mapNotNull { c ->
            val matches = if (trimmed.isEmpty()) c.sports else c.sports.filter { it.displayName.contains(trimmed, ignoreCase = true) }
            if (matches.isEmpty()) null else c to matches
        }
    }

    fun close() {
        focus.clearFocus()
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SheetBackground,
        tonalElevation = 0.dp,
    ) {
        // Nearly full height: leaves a strip of the screen above the sheet, and stays put while searching.
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Your sport",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    Text(
                        "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .sportPressable(::close)
                            .background(DoneNavy, CircleShape)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick the sport you play. It sets your home, rules and alerts.",
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF6E6E73),
                )
                Spacer(Modifier.height(16.dp))
                SportSearchField(query, { query = it }, placeholder = "Search ${catalog.allSports.size} sports")
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .sheetScrollLikeIos()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                if (sections.isEmpty()) {
                    Text(
                        "No sports match \u201C$trimmed\u201D",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                sections.forEach { (category, sports) ->
                    Spacer(Modifier.height(18.dp))
                    Text(
                        category.title.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = SecondaryGrey,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp).semantics { heading() },
                    )
                    sports.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { sport ->
                                CatalogSportTile(
                                    sport = sport,
                                    isLive = catalog.isLive(sport),
                                    isSelected = sport == selection,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (sport == selection) return@CatalogSportTile
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    selection = sport
                                    onSelect(sport)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, SystemGray4, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = SecondaryGrey)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = SecondaryGrey, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Cancel, "Clear search",
                Modifier
                    .size(18.dp)
                    .clickable(role = Role.Button) { onValueChange("") },
                tint = Color(0xFFAEAEB2),
            )
        }
    }
}

/** Picker tile: themed gradient when selected, white when live, dimmed with "SOON" otherwise. */
@Composable
private fun CatalogSportTile(
    sport: SportType,
    isLive: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val theme = sport.theme
    val shape = RoundedCornerShape(16.dp)
    // A no-longer-live current sport still shows as selected (spec), just not re-selectable.
    val filled = isSelected
    val stateText = when {
        isSelected -> "selected"
        isLive -> "not selected"
        else -> "coming soon"
    }

    Box(
        modifier
            .aspectRatio(1f / 0.85f)
            .then(if (isLive) Modifier.sportPressable(onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = "${sport.displayName}, $stateText"
                selected = isSelected
                if (!isLive) disabled()
            }
            .then(
                when {
                    filled -> Modifier.shadow(8.dp, shape, ambientColor = theme.primary.copy(alpha = 0.35f), spotColor = theme.primary.copy(alpha = 0.35f))
                    isLive -> Modifier.shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
                    else -> Modifier // soon: no shadow
                }
            )
            .clip(shape)
            .then(
                when {
                    filled -> Modifier.background(theme.gradient)
                    isLive -> Modifier.background(Color.White).border(1.dp, SystemGray5, shape)
                    else -> Modifier.background(Color.White.copy(alpha = 0.55f)).border(1.dp, SystemGray5.copy(alpha = 0.55f), shape)
                }
            )
            .padding(12.dp),
    ) {
        Box(Modifier.align(Alignment.TopStart).size(34.dp), contentAlignment = Alignment.Center) {
            if (filled) {
                // iOS: a white 85% disc 2dp wider than the ball, with a soft shadow, so the
                // artwork reads on its own sport's gradient (e.g. the purple badminton ball).
                Box(
                    Modifier
                        .size(34.dp * 44f / 48f + 2.dp)
                        .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                        .background(Color.White.copy(alpha = 0.85f), CircleShape),
                )
            }
            SportArtworkImage(
                sport, 34.dp,
                Modifier.graphicsLayer { alpha = if (isLive || filled) 1f else 0.55f },
            )
        }
        BasicText(
            sport.displayName,
            style = TextStyle(
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    filled -> Color.White
                    isLive -> Color.Black
                    else -> SecondaryGrey
                },
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // iOS minimumScaleFactor(0.85): 13sp down to ~11sp before truncating.
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
            modifier = Modifier.align(Alignment.BottomStart),
        )
        if (filled) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(13.dp), tint = theme.primary)
            }
        } else if (!isLive) {
            Text(
                "SOON",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = SecondaryGrey,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp),
            )
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

        SportArtworkImage(
            sport, 74.dp,
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .graphicsLayer {
                    val v = bounce.value
                    rotationZ = 10f + (-30f * v)
                    translationY = (4f + (-10f * v)) * density
                    shadowElevation = (6f + 8f * v) * density
                    this.shape = SportArtworkShape
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
