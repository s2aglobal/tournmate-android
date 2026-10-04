package com.s2aglobal.tournmate.ui.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.s2aglobal.tournmate.BuildConfig
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.domain.model.SeedTierRules
import com.s2aglobal.tournmate.service.calorie.HealthConnectCalorieResult
import com.s2aglobal.tournmate.service.calorie.METEstimator
import com.s2aglobal.tournmate.service.calorie.PlayIntensity
import com.s2aglobal.tournmate.ui.component.AppPrimaryButton
import com.s2aglobal.tournmate.ui.component.BottomSheetPicker
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.component.SportPickerRow
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.screen.player.EloExplainerCard
import com.s2aglobal.tournmate.ui.screen.player.InfoBlue
import com.s2aglobal.tournmate.ui.screen.player.LossRed
import com.s2aglobal.tournmate.ui.screen.player.SportsmanshipStars
import com.s2aglobal.tournmate.ui.screen.player.rankProgressHint
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import com.s2aglobal.tournmate.util.openInBrowser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private val GroupedBg = Color(0xFFF2F2F7)
private val FieldBg = Color(0xFFF2F2F7)
private val MaleBlue = Color(0xFF2196F3)
private val FemalePink = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePlaceholder(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCalorieHistory by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.logScreenView()
        viewModel.load()
    }

    if (showCalorieHistory && uiState.player != null) {
        BackHandler { showCalorieHistory = false }
        CalorieHistoryScreen(
            modifier = modifier,
            records = uiState.calorieRecords,
            isLoading = uiState.isLoading,
            onBack = { showCalorieHistory = false },
        )
        return
    }

    // MainScreen's Scaffold already pads for the status bar and bottom nav bar,
    // so this nested Scaffold must not re-apply system-bar insets.
    Scaffold(
        modifier = modifier,
        containerColor = GroupedBg,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when {
            uiState.isGuest -> GuestPromptScreen(Modifier.padding(padding), onSignOut)
            uiState.isLoading -> LoadingState(Modifier.padding(padding), "Loading profile…")
            uiState.player != null -> SignedInProfile(
                Modifier.padding(padding), uiState.player!!, viewModel, onSignOut,
                onViewHistory = { showCalorieHistory = true },
            )
            else -> ProfileNotFound(Modifier.padding(padding), onSignOut)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier, message: String) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = AppAccent)
        Spacer(Modifier.height(12.dp))
        Text(message, fontSize = 15.sp, color = Color.Gray)
    }
}

@Composable
private fun ProfileNotFound(modifier: Modifier, onSignOut: () -> Unit) {
    val accent = WarningOrange
    Column(
        modifier.fillMaxSize().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(130.dp).border(1.5.dp, accent.copy(alpha = 0.08f), CircleShape))
            Box(Modifier.size(110.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.12f), accent.copy(alpha = 0.03f))), CircleShape))
            Box(Modifier.size(80.dp).background(accent.copy(alpha = 0.1f), CircleShape))
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(40.dp), tint = accent.copy(alpha = 0.6f))
            Box(
                Modifier.offset(x = 32.dp, y = (-32).dp).size(30.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(28.dp).background(accent.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(14.dp), tint = accent)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Profile Not Found", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text(
            "Your player profile could not be loaded. Try signing out and back in.",
            fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        Spacer(Modifier.height(24.dp))
        Surface(onClick = onSignOut, shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(15.dp), tint = accent)
                Text("Sign Out & Retry", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInProfile(
    modifier: Modifier,
    player: Player,
    viewModel: ProfileViewModel,
    onSignOut: () -> Unit,
    onViewHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEloInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showQuickPlay by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    var isPullRefreshing by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Text(
            "Profile",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(GroupedBg).fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        PullToRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = {
                isPullRefreshing = true
                viewModel.refresh { isPullRefreshing = false }
            },
            modifier = Modifier.fillMaxSize(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isPullRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = AppAccent,
                )
            },
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ProfileHeader(player, onAvatarEdit = { showAvatarPicker = true })
                Spacer(Modifier.height(24.dp))
                ContactCard(player)
                Spacer(Modifier.height(24.dp))
                AgeCard(player)
                Spacer(Modifier.height(24.dp))
                SportCard(viewModel)
                Spacer(Modifier.height(24.dp))
                HomeRegionCard(player, viewModel)
                Spacer(Modifier.height(24.dp))
                MatchStatsSection(uiState)
                Spacer(Modifier.height(24.dp))
                FitnessSection(uiState, player, onQuickPlay = { showQuickPlay = true }, onViewHistory = onViewHistory)
                Spacer(Modifier.height(24.dp))
                SkillRatingCard(player, showEloInfo) { showEloInfo = !showEloInfo }
                Spacer(Modifier.height(24.dp))
                SportsmanshipCard(uiState)
                Spacer(Modifier.height(24.dp))
                LegalSection()
                Spacer(Modifier.height(24.dp))
                SignOutSection(uiState, onSignOut) { showDeleteDialog = true }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showQuickPlay) {
        QuickPlaySheet(
            playerWeight = player.weightKg ?: 70.0,
            viewModel = viewModel,
            onDismiss = { showQuickPlay = false },
        )
    }

    if (showAvatarPicker) {
        AvatarPickerSheet(
            currentAvatarId = player.avatarId,
            viewModel = viewModel,
            onDismiss = { showAvatarPicker = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account, player profile, and all associated data. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount(onDeleted = onSignOut)
                }) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

// ── Profile Header ──────────────────────────────────

@Composable
private fun ProfileHeader(player: Player, onAvatarEdit: () -> Unit = {}) {
    val tier = SeedTierRules.tier(player.elo)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(140.dp).background(Brush.radialGradient(listOf(AppAccent.copy(alpha = 0.12f), Color.Transparent), radius = 200f), CircleShape))
            Surface(Modifier.size(100.dp), CircleShape, AppAccent.copy(alpha = 0.1f)) {
                AsyncImage(player.avatar.avatarUrl(128), "Avatar", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Surface(
                onClick = onAvatarEdit,
                modifier = Modifier.size(32.dp).offset(x = 38.dp, y = 38.dp).shadow(4.dp, CircleShape),
                shape = CircleShape,
                color = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Settings, "Edit avatar", Modifier.size(15.dp), tint = AppAccent) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(player.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (player.gender != Gender.PREFER_NOT_TO_SAY) {
                val isMale = player.gender == Gender.MALE
                BadgePill(player.gender.displayName, if (isMale) MaleBlue else FemalePink, if (isMale) Icons.Filled.Man else Icons.Filled.Woman)
            }
            BadgePill(tier.title, tier.badgeColor, Icons.Filled.Star)
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color, icon: ImageVector) {
    Row(Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(11.dp), tint = color)
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Contact ─────────────────────────────────────────

@Composable
private fun ContactCard(player: Player) {
    CardSection("Contact", rememberVectorPainter(Icons.Filled.Badge)) {
        if (player.email.isNotBlank()) {
            ContactRow(Icons.Filled.Email, "Email", player.email)
            if (player.phone.isNotBlank()) Divider(Modifier.padding(start = 60.dp))
        }
        if (player.phone.isNotBlank()) ContactRow(Icons.Filled.Phone, "Phone", player.phone)
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp)
        }
    }
}

// ── Age ─────────────────────────────────────────────

@Composable
private fun AgeCard(player: Player) {
    CardSection("Age", rememberVectorPainter(Icons.Filled.Event)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Filled.Cake)
            Spacer(Modifier.width(14.dp))
            Column {
                val age = player.age
                if (age != null) {
                    Text("$age years old", fontSize = 15.sp)
                    Text("Used for age-group eligibility", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Text("Not set", fontSize = 15.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Surface(Modifier.size(32.dp), RoundedCornerShape(8.dp), AppAccent.copy(alpha = 0.08f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp), tint = AppAccent) }
    }
}

// ── Sport Selector ──────────────────────────────────

@Composable
private fun SportCard(vm: ProfileViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("My Sport", painterResource(R.drawable.ic_sportscourt))
        Spacer(Modifier.height(12.dp))
        // Mirrors the app-wide sport so a switch from the Play tab shows here too.
        SportPickerRow(selection = CurrentSport.sport, onSelect = vm::updateSport)
    }
}

// ── Home Region ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRegionCard(player: Player, vm: ProfileViewModel) {
    val initialCountry = remember(player.id, player.homeCountryCode) { player.homeCountryCode?.let { HomeRegionCountry.matching(it) } ?: HomeRegionCountry.fallback }
    var originalCountry by remember(player.id, player.homeCountryCode, player.homePostalCode) { mutableStateOf(initialCountry) }
    var originalPostal by remember(player.id, player.homeCountryCode, player.homePostalCode) { mutableStateOf(player.homePostalCode ?: "") }
    var selectedCountry by remember(player.id, player.homeCountryCode, player.homePostalCode) { mutableStateOf(initialCountry) }
    var postalCode by remember(player.id, player.homeCountryCode, player.homePostalCode) { mutableStateOf(player.homePostalCode ?: "") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val hasChanged = selectedCountry.code != originalCountry.code || postalCode != originalPostal
    val fieldColors = TextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)

    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Tournament area", rememberVectorPainter(Icons.Filled.PinDrop))
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Choose your country and enter a postal code that matches that country (e.g. 5-digit US ZIP, 6-digit India PIN).",
                    fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))

                Text("Country", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { showCountryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = FieldBg,
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(selectedCountry.name, fontSize = 15.sp, color = Color.Black)
                            Text(selectedCountry.code, fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Filled.ExpandMore, null, Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("Postal / ZIP / PIN", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                TextField(
                    value = postalCode, onValueChange = { postalCode = it; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 75201 or 500081", color = Color.Gray) },
                    shape = RoundedCornerShape(10.dp), colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                )

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, fontSize = 11.sp, color = if (it.contains("Saved")) AppAccent else Color.Red)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        message = null
                        isSaving = true
                        vm.updateHomeRegion(selectedCountry.code, postalCode) { success, result ->
                            isSaving = false
                            if (success) {
                                originalCountry = selectedCountry
                                originalPostal = postalCode
                            }
                            message = result
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    enabled = !isSaving && hasChanged,
                ) {
                    if (isSaving) {
                        TrophySpinner(size = 16.dp, style = TrophySpinnerStyle.STANDARD)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save home area", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showCountryPicker) {
        BottomSheetPicker(
            title = "HOME COUNTRY",
            items = HomeRegionCountry.pickerOptions,
            selectedItem = selectedCountry,
            onItemSelected = { selectedCountry = it; showCountryPicker = false; message = null },
            onDismiss = { showCountryPicker = false },
            itemLabel = { it.name },
            itemSubtitle = { it.code },
            searchable = true,
            searchFilter = { item, query ->
                item.name.contains(query, ignoreCase = true) || item.code.contains(query, ignoreCase = true)
            },
        )
    }
}

// ── Match Stats ─────────────────────────────────────

@Composable
private fun MatchStatsSection(uiState: ProfileUiState) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Match Stats", rememberVectorPainter(Icons.Filled.BarChart))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(painterResource(R.drawable.ic_sportscourt_fill), "${uiState.matchesPlayed}", "Matches", AppAccent, Modifier.weight(1f))
            StatTile(Icons.Filled.EmojiEvents, "${uiState.wins}", "Wins", WarningOrange, Modifier.weight(1f))
            StatTile(Icons.Filled.Percent, "${(uiState.winRate * 100).toInt()}%", "Win Rate", InfoBlue, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) =
    StatTile(rememberVectorPainter(icon), value, label, color, modifier)

@Composable
private fun StatTile(icon: Painter, value: String, label: String, color: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), Color.White, shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(36.dp), CircleShape, color.copy(alpha = 0.1f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp), tint = color) } }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// ── Skill Rating (ELO) ─────────────────────────────

@Composable
private fun SkillRatingCard(player: Player, showInfo: Boolean, onToggle: () -> Unit) {
    val tier = SeedTierRules.tier(player.elo)
    val progress = min(player.elo / 2500.0, 1.0).toFloat()

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("Skill Rating", rememberVectorPainter(Icons.Filled.Stars))
            Spacer(Modifier.weight(1f))
            Icon(
                if (showInfo) Icons.Filled.Cancel else Icons.Outlined.Info,
                if (showInfo) "Hide Elo info" else "What is Elo?",
                Modifier.size(20.dp).clickable { onToggle() },
                tint = if (showInfo) Color.Gray else AppAccent,
            )
        }
        Spacer(Modifier.height(10.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    val p = AppAccent
                    androidx.compose.foundation.Canvas(Modifier.size(80.dp)) {
                        drawArc(p.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(4.dp.toPx()))
                        drawArc(p, -90f, 360f * progress, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${player.elo.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Elo", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(15.dp), tint = tier.badgeColor)
                        Text(tier.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    if (player.streak != 0) {
                        val winning = player.streak > 0
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (winning) Icons.Filled.LocalFireDepartment else Icons.Filled.ArrowCircleDown, null,
                                Modifier.size(13.dp), tint = if (winning) WarningOrange else LossRed,
                            )
                            Text("${abs(player.streak)} ${if (winning) "win" else "loss"} streak", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Text(rankProgressHint(player.elo), fontSize = 11.sp, color = Color.LightGray, maxLines = 2)
                }
            }
        }
        AnimatedVisibility(showInfo, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            EloExplainerCard(Modifier.padding(top = 10.dp))
        }
    }
}

// ── Fitness & Health ────────────────────────────────

@Composable
private fun FitnessSection(uiState: ProfileUiState, player: Player, onQuickPlay: () -> Unit, onViewHistory: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Fitness & Health Connect", rememberVectorPainter(Icons.Filled.LocalFireDepartment))
        Spacer(Modifier.height(10.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.15f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(26.dp), tint = Color.Red)
                    Text("Health Connect Integration", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("TournMate connects with Health Connect to provide accurate fitness tracking for your ${CurrentSport.sport.inlineName} sessions.", fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
                Divider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthDataRow(Icons.Filled.ArrowCircleDown, "Reads", "Workout calories from Health Connect", SuccessGreen)
                    HealthDataRow(Icons.Filled.ArrowCircleUp, "Writes", "Calories burned during ${CurrentSport.sport.inlineName} sessions", InfoBlue)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Watch, null, Modifier.size(13.dp), tint = Color.Gray)
                    Text("Start a ${CurrentSport.sport.displayName} workout on your watch for automatic calorie tracking.", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (uiState.sessionsTracked > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Filled.LocalFireDepartment, formatCalories(uiState.totalCalories), "Total Calories", WarningOrange, Modifier.weight(1f))
                StatTile(sportIconPainter(CurrentSport.sport), "${uiState.sessionsTracked}", "Sessions", AppAccent, Modifier.weight(1f))
                StatTile(Icons.AutoMirrored.Filled.ShowChart, formatCalories(uiState.avgCaloriesPerSession), "Avg / Session", InfoBlue, Modifier.weight(1f))
            }

            player.weightKg?.let {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Scale, null, Modifier.size(13.dp), tint = Color.Gray)
                    Text("Weight: ${it.toInt()} kg", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = onViewHistory, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = WarningOrange.copy(alpha = 0.08f)) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("View History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ChevronRight, null, Modifier.size(12.dp), tint = WarningOrange)
                    }
                }
                QuickPlayButton("Quick Play", onQuickPlay, Modifier.weight(1f))
            }
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(36.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Play sessions and log calories to see your fitness stats here.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    QuickPlayButton("Log a Quick Play", onQuickPlay, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun QuickPlayButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), color = AppAccent.copy(alpha = 0.08f)) {
        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(14.dp), tint = AppAccent)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppAccent)
        }
    }
}

@Composable
private fun HealthDataRow(icon: ImageVector, title: String, detail: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = color)
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun formatCalories(value: Double): String =
    if (value >= 1000) String.format("%.1fk", value / 1000) else "${Math.round(value)}"

// ── Quick Play Sheet ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPlaySheet(playerWeight: Double, viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hasHealthConnect = remember { viewModel.healthConnectService.isAvailable }
    var durationMinutes by remember { mutableFloatStateOf(60f) }
    var weight by remember { mutableFloatStateOf(playerWeight.toFloat()) }
    var healthChecked by remember { mutableStateOf(!hasHealthConnect) }
    var healthResult by remember { mutableStateOf<HealthConnectCalorieResult?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var showDuplicateAlert by remember { mutableStateOf(false) }
    val foundHealthData = healthResult?.let { it.fromHealthConnect && it.calories > 0 } == true

    LaunchedEffect(Unit) {
        if (!healthChecked) {
            healthResult = viewModel.checkHealthConnect(durationMinutes.toInt())
            healthChecked = true
        }
    }

    fun doSave() {
        isSaving = true
        viewModel.saveQuickPlay(durationMinutes.toInt(), weight.toDouble(), healthResult) { success, msg ->
            savedMessage = msg
            if (success) {
                scope.launch {
                    delay(1000)
                    isSaving = false
                    onDismiss()
                }
            } else {
                isSaving = false
            }
        }
    }

    fun save() {
        if (isSaving) return
        viewModel.isQuickPlayDuplicate { duplicate ->
            if (duplicate) showDuplicateAlert = true else doSave()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 520.dp)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) { Text("Cancel", color = AppAccent) }
                Text("Quick Play", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(Modifier.size(50.dp), CircleShape, AppAccent.copy(alpha = 0.12f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, Modifier.size(24.dp), tint = AppAccent) }
                    }
                    Column {
                        Text("Quick Play", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Log calories from a casual court session", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                when {
                    !healthChecked -> Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(48.dp), tint = Color.Red)
                        Text("Checking Health Connect…", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Looking for recent workout data to track your calories accurately.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        CircularProgressIndicator(Modifier.size(28.dp), color = AppAccent)
                    }

                    foundHealthData -> Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.Red.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.15f)),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Verified, null, Modifier.size(16.dp), tint = SuccessGreen)
                                        Text("Health Connect Data Found", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${Math.round(healthResult!!.calories)} kcal", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                    Text("from Health Connect workout", fontSize = 12.sp, color = Color.Gray)
                                }
                                Icon(Icons.Filled.Watch, null, Modifier.size(36.dp), tint = Color.Red.copy(alpha = 0.5f))
                            }
                        }
                        savedMessage?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent) }
                        Surface(
                            onClick = { save() },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.Red, FemalePink))).padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                else Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isSaving) "Saving…" else "Save to My Stats", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }

                    else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hasHealthConnect) {
                            Row(
                                Modifier.fillMaxWidth().background(WarningOrange.copy(alpha = 0.06f), RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Info, null, Modifier.size(16.dp), tint = WarningOrange)
                                Text("No Health Connect workout found. Enter details manually.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Duration", fontSize = 12.sp, color = Color.Gray)
                                Text("${durationMinutes.toInt()} min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                            }
                            Slider(value = durationMinutes, onValueChange = { durationMinutes = it }, valueRange = 15f..240f, steps = 14, colors = SliderDefaults.colors(thumbColor = AppAccent, activeTrackColor = AppAccent))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Your weight", fontSize = 12.sp, color = Color.Gray)
                                Text("${weight.toInt()} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                            }
                            Slider(value = weight, onValueChange = { weight = Math.round(it).toFloat() }, valueRange = 30f..200f, colors = SliderDefaults.colors(thumbColor = WarningOrange, activeTrackColor = WarningOrange))
                        }

                        val estimatedCal = Math.round(METEstimator.estimate(durationMinutes.toInt(), weight.toDouble(), PlayIntensity.CASUAL, CurrentSport.sport))
                        Text("Estimated: ~$estimatedCal kcal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarningOrange)

                        savedMessage?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent) }

                        Button(
                            onClick = { save() },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                        ) {
                            if (isSaving) {
                                TrophySpinner(size = 16.dp, style = TrophySpinnerStyle.STANDARD)
                                Spacer(Modifier.width(6.dp))
                            }
                            Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSaving) "Saving…" else "Log Calories", fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            "Tip: Start a ${CurrentSport.sport.displayName} workout on your watch for automatic tracking next time.",
                            fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showDuplicateAlert) {
        AlertDialog(
            onDismissRequest = { showDuplicateAlert = false },
            title = { Text("Already Logged", fontWeight = FontWeight.Bold) },
            text = { Text("You've already logged a Quick Play session this hour. Do you want to update it with new values?") },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateAlert = false
                    doSave()
                }) { Text("Log Again", color = WarningOrange, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDuplicateAlert = false }) { Text("Cancel") } },
        )
    }
}

// ── Sportsmanship ───────────────────────────────────

@Composable
private fun SportsmanshipCard(uiState: ProfileUiState) {
    val count = uiState.sportsmanshipCount
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Sportsmanship", rememberVectorPainter(Icons.Filled.Favorite), circled = true)
        Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, shadowElevation = 2.dp) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SportsmanshipStars(uiState.sportsmanshipAvg)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(String.format("%.1f", uiState.sportsmanshipAvg) + " / 5.0", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("$count rating${if (count == 1) "" else "s"}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ── Legal ───────────────────────────────────────────

@Composable
private fun LegalSection() {
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Legal", rememberVectorPainter(Icons.Filled.Description))
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp) {
            Column {
                LegalRow("Terms & Conditions", Icons.AutoMirrored.Outlined.Article) { openInBrowser(ctx, "https://www.tournmate.com/terms") }
                Divider(Modifier.padding(start = 44.dp))
                LegalRow("Privacy Policy", Icons.Filled.PanTool) { openInBrowser(ctx, "https://www.tournmate.com/privacy") }
            }
        }
    }
}

@Composable
private fun LegalRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(15.dp), tint = AppAccent) }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 15.sp); Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.NorthEast, null, Modifier.size(12.dp), tint = Color.LightGray)
    }
}

// ── Sign Out ────────────────────────────────────────

@Composable
private fun SignOutSection(uiState: ProfileUiState, onSignOut: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // iOS `.appDestructive`: headline (17 SemiBold) red, 14pt vertical padding, radius 14, 1.5 red@40% stroke.
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).border(1.5.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(17.dp), tint = Color.Red); Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDelete, enabled = !uiState.isDeletingAccount) {
            if (uiState.isDeletingAccount) CircularProgressIndicator(Modifier.size(14.dp), color = Color.Red, strokeWidth = 2.dp)
            else Icon(Icons.Filled.PersonRemove, null, Modifier.size(14.dp), tint = Color.Red)
            Spacer(Modifier.width(8.dp))
            Text(if (uiState.isDeletingAccount) "Deleting…" else "Delete Account", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Red)
        }
        uiState.deleteErrorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontSize = 12.sp, color = Color.Red, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text("v${BuildConfig.VERSION_NAME} · TournMate", fontSize = 11.sp, color = Color.LightGray)
    }
}

// ── Shared Components ───────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: Painter, circled: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (circled) {
            Box(Modifier.size(16.dp).background(AppAccent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(9.dp), tint = Color.White)
            }
        } else {
            Icon(icon, null, Modifier.size(16.dp), tint = AppAccent)
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppAccent)
    }
}

@Composable
private fun CardSection(title: String, icon: Painter, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(title, icon); Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) { Column(Modifier.fillMaxWidth()) { content() } }
    }
}

// ── Guest Prompt ────────────────────────────────────

@Composable
private fun GuestPromptScreen(modifier: Modifier, onJoinClick: () -> Unit) {
    Column(modifier.fillMaxSize().background(Color.White), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(130.dp).background(Brush.radialGradient(listOf(AppAccent.copy(alpha = 0.18f), AppAccent.copy(alpha = 0.04f))), CircleShape))
                Box(Modifier.size(96.dp).background(AppAccent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_person_circle_outline), null, Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.85f))
                }
            }
            Surface(Modifier.size(36.dp).offset(x = 2.dp, y = 2.dp).shadow(4.dp, RoundedCornerShape(10.dp)), RoundedCornerShape(10.dp), Color.Black) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardTab, null, Modifier.size(15.dp), tint = AppAccent) }
            }
        }
        Text("Guest Mode", fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 24.dp))
        Text("Sign in to see your profile, track your stats,\nand rate other players.", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp, start = 40.dp, end = 40.dp))
        Row(Modifier.padding(top = 32.dp, start = 40.dp, end = 40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(Icons.Filled.BarChart, "ANALYTICS", AppAccent, Modifier.weight(1f)); Tile(Icons.Filled.EmojiEvents, "LEAGUES", WarningOrange, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onJoinClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(60.dp).background(AppAccent, RoundedCornerShape(30.dp))) {
            Text("JOIN THE ELITE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp); Spacer(Modifier.width(10.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp), tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, color: Color, modifier: Modifier) {
    Surface(modifier.border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(16.dp)), RoundedCornerShape(16.dp), Color.White) {
        Column(Modifier.padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(22.dp), tint = color); Spacer(Modifier.height(10.dp)); Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
    }
}

// ── Avatar Picker Sheet ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerSheet(
    currentAvatarId: String,
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(currentAvatarId) }
    var isSaving by remember { mutableStateOf(false) }
    val selectedAvatar = PlayerAvatar.fromId(selected)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) { Text("Cancel", color = AppAccent) }
                Text("Choose Avatar", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }

            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).background(Brush.radialGradient(listOf(AppAccent.copy(alpha = 0.10f), Color.Transparent)), CircleShape))
                    Surface(Modifier.size(110.dp), CircleShape, AppAccent.copy(alpha = 0.1f)) {
                        AsyncImage(selectedAvatar.avatarUrl(128), "Preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(selectedAvatar.displayName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(PlayerAvatar.selectable, key = { it.id }) { avatar ->
                    val isSelected = avatar.id == selected
                    Column(
                        Modifier.clickable { selected = avatar.id },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                Modifier.size(58.dp).then(if (isSelected) Modifier.border(3.dp, AppAccent, CircleShape) else Modifier),
                                CircleShape,
                                AppAccent.copy(alpha = 0.1f),
                            ) {
                                AsyncImage(avatar.avatarUrl(64), avatar.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            if (isSelected) {
                                Box(Modifier.offset(x = 2.dp, y = 2.dp).size(20.dp).background(AppAccent, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Check, null, Modifier.size(11.dp), tint = Color.White)
                                }
                            }
                        }
                        Text(
                            avatar.displayName,
                            fontSize = 11.sp,
                            color = if (isSelected) AppAccent else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            AppPrimaryButton(
                onClick = {
                    isSaving = true
                    viewModel.updateAvatar(selected) { success ->
                        isSaving = false
                        if (success) onDismiss()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            ) {
                if (isSaving) {
                    TrophySpinner(size = 18.dp, style = TrophySpinnerStyle.INLINE)
                } else {
                    Text("Save Avatar")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                }
            }
        }
    }
}
