package com.s2aglobal.tournmate.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SeedTierRules
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.component.BottomSheetPicker
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.component.SportPickerRow
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import com.s2aglobal.tournmate.util.openInBrowser
import kotlin.math.abs
import kotlin.math.min

private val GroupedBg = Color(0xFFF2F2F7)
private val FieldBg = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePlaceholder(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        modifier = modifier,
        containerColor = GroupedBg,
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppAccent) }
            uiState.isGuest -> GuestPromptScreen(Modifier.padding(padding), onSignOut)
            uiState.player != null -> SignedInProfile(Modifier.padding(padding), uiState.player!!, viewModel, onSignOut)
            else -> GuestPromptScreen(Modifier.padding(padding), onSignOut)
        }
    }
}

@Composable
private fun SignedInProfile(modifier: Modifier, player: Player, viewModel: ProfileViewModel, onSignOut: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEloInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showQuickPlay by remember { mutableStateOf(false) }
    var showCalorieHistory by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Text(
            "Profile",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(GroupedBg).fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ProfileHeader(player, onAvatarEdit = { showAvatarPicker = true })
        Spacer(Modifier.height(24.dp))
        ContactCard(player)
        Spacer(Modifier.height(16.dp))
        AgeCard(player)
        Spacer(Modifier.height(16.dp))
        SportCard(player, viewModel)
        Spacer(Modifier.height(16.dp))
        HomeRegionCard(player, viewModel)
        Spacer(Modifier.height(16.dp))
        MatchStatsSection()
        Spacer(Modifier.height(16.dp))
        FitnessSection(uiState, player, onQuickPlay = { showQuickPlay = true }, onViewHistory = { showCalorieHistory = true })
        Spacer(Modifier.height(16.dp))
        SkillRatingCard(player, showEloInfo) { showEloInfo = !showEloInfo }
        Spacer(Modifier.height(16.dp))
        SportsmanshipCard()
        Spacer(Modifier.height(16.dp))
        LegalSection()
        Spacer(Modifier.height(16.dp))
        SignOutSection(onSignOut) { showDeleteDialog = true }
        Spacer(Modifier.height(40.dp))
    }}

    if (showCalorieHistory) {
        CalorieHistorySheet(
            records = uiState.calorieRecords,
            totalCalories = uiState.totalCalories,
            onDismiss = { showCalorieHistory = false },
        )
    }

    if (showQuickPlay) {
        var showDuplicateAlert by remember { mutableStateOf(false) }
        var pendingDuration by remember { mutableStateOf(60) }
        var pendingWeight by remember { mutableStateOf(player.weightKg ?: 70.0) }

        QuickPlayDialog(
            playerWeight = player.weightKg ?: 70.0,
            onDismiss = { showQuickPlay = false },
            onSave = { duration, weight ->
                pendingDuration = duration
                pendingWeight = weight
                viewModel.saveQuickPlay(duration, weight) { result ->
                    if (result == "DUPLICATE") {
                        showDuplicateAlert = true
                    } else {
                        showQuickPlay = false
                    }
                }
            },
        )

        if (showDuplicateAlert) {
            AlertDialog(
                onDismissRequest = { showDuplicateAlert = false },
                title = { Text("Already Logged", fontWeight = FontWeight.Bold) },
                text = { Text("You've already logged a Quick Play session this hour. Do you want to update it with new values?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDuplicateAlert = false
                        viewModel.saveQuickPlay(pendingDuration, pendingWeight, forceSave = true) { showQuickPlay = false }
                    }) { Text("Log Again", color = WarningOrange, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDuplicateAlert = false }) { Text("Cancel") }
                },
            )
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = player.avatarId,
            onDismiss = { showAvatarPicker = false },
            onSave = { avatarId ->
                viewModel.updateAvatar(avatarId)
                showAvatarPicker = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account, player profile, and all associated data. This action cannot be undone.") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

// ── Profile Header ──────────────────────────────────

@Composable
private fun ProfileHeader(player: Player, onAvatarEdit: () -> Unit = {}) {
    val tier = SeedTierRules.tier(player.elo)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { onAvatarEdit() }) {
            Box(Modifier.size(140.dp).background(Brush.radialGradient(listOf(AppAccent.copy(alpha = 0.12f), Color.Transparent), radius = 200f), CircleShape))
            Surface(Modifier.size(100.dp), CircleShape, AppAccent.copy(alpha = 0.1f)) {
                AsyncImage(player.avatar.avatarUrl(128), "Avatar", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Surface(Modifier.size(32.dp).offset(x = 38.dp, y = 38.dp).shadow(4.dp, CircleShape), CircleShape, Color.White) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, null, Modifier.size(15.dp), tint = AppAccent) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(player.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (player.gender != Gender.PREFER_NOT_TO_SAY) {
                val c = if (player.gender == Gender.MALE) Color(0xFF2196F3) else Color(0xFFE91E63)
                BadgePill(player.gender.displayName, c)
            }
            BadgePill(tier.title, tier.badgeColor)
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Row(Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.Star, null, Modifier.size(10.dp), tint = color)
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Contact ─────────────────────────────────────────

@Composable
private fun ContactCard(player: Player) {
    CardSection("Contact", Icons.Default.Person) {
        if (player.email.isNotBlank()) InfoRow(Icons.Default.Email, "Email", player.email)
        if (player.phone.isNotBlank()) {
            if (player.email.isNotBlank()) Divider(Modifier.padding(start = 60.dp))
            InfoRow(Icons.Default.Phone, "Phone", player.phone)
        }
    }
}

// ── Age ─────────────────────────────────────────────

@Composable
private fun AgeCard(player: Player) {
    CardSection("Age", Icons.Default.Cake) {
        InfoRow(Icons.Default.Cake, if (player.age != null) "${player.age} years old" else "Not set", if (player.age != null) "Used for age-group eligibility" else "")
    }
}

// ── Sport Selector ──────────────────────────────────

@Composable
private fun SportCard(player: Player, vm: ProfileViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("My Sport", Icons.Default.SportsTennis)
        Spacer(Modifier.height(10.dp))
        // Mirrors the app-wide sport so a switch from the Play tab shows here too.
        SportPickerRow(selection = CurrentSport.sport, onSelect = vm::updateSport)
    }
}

// ── Home Region (editable, matching iOS) ────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRegionCard(player: Player, vm: ProfileViewModel) {
    var selectedCountry by remember { mutableStateOf(HomeRegionCountry.matching(player.homeCountryCode ?: "US") ?: HomeRegionCountry.fallback) }
    var postalCode by remember { mutableStateOf(player.homePostalCode ?: "") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val fieldColors = TextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)

    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Tournament area", Icons.Default.Map)
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Choose your country and enter a postal code that matches that country (e.g. 5-digit US ZIP, 6-digit India PIN).",
                    fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(16.dp))

                Text("Country", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showCountryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedCountry.name, fontSize = 14.sp, color = Color.Black)
                        Text(selectedCountry.code, fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(16.dp), tint = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))

                Text("Postal / ZIP / PIN", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                TextField(
                    value = postalCode, onValueChange = { postalCode = it; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 75201 or 500081", color = Color.Gray) },
                    shape = RoundedCornerShape(10.dp), colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 11.sp, color = if (it.contains("Saved")) AppAccent else Color.Red)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSaving = true
                        vm.updateHomeRegion(selectedCountry.code, postalCode) { result ->
                            isSaving = false
                            message = result
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    enabled = !isSaving,
                ) {
                    Text(if (isSaving) "Saving..." else "Save home area", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showCountryPicker) {
        BottomSheetPicker(
            title = "Home Country",
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
private fun MatchStatsSection() {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Match Stats", Icons.Default.BarChart)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Icons.Default.SportsTennis, "0", "Matches", AppAccent, Modifier.weight(1f))
            StatTile(Icons.Default.EmojiEvents, "0", "Wins", WarningOrange, Modifier.weight(1f))
            StatTile(Icons.Default.Percent, "0%", "Win Rate", Color(0xFF2196F3), Modifier.weight(1f))
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
            SectionHeader("Skill Rating", Icons.Default.Star)
            Spacer(Modifier.weight(1f))
            Icon(if (showInfo) Icons.Default.CheckCircle else Icons.Default.Info, null, Modifier.size(20.dp).clickable { onToggle() }, tint = if (showInfo) Color.Gray else AppAccent)
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
                        Text("${player.elo.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Elo", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Security, null, Modifier.size(14.dp), tint = tier.badgeColor)
                        Text(tier.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (player.streak != 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("${abs(player.streak)} ${if (player.streak > 0) "win" else "loss"} streak", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(rankHint(player.elo), fontSize = 10.sp, color = Color.LightGray)
                }
            }
        }
        AnimatedVisibility(showInfo, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            EloExplainer()
        }
    }
}

@Composable
private fun EloExplainer() {
    Surface(Modifier.padding(top = 10.dp).fillMaxWidth(), RoundedCornerShape(16.dp), AppAccent.copy(alpha = 0.04f), border = androidx.compose.foundation.BorderStroke(1.dp, AppAccent.copy(alpha = 0.15f))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💡", fontSize = 16.sp)
                Text("What is Elo Rating?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "The Elo rating system, created by physicist Arpad Elo, is a method for calculating the relative skill level of players. It's widely used in chess, esports, and competitive sports including badminton.",
                fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(14.dp)); Divider(color = Color.Gray.copy(alpha = 0.15f)); Spacer(Modifier.height(14.dp))

            // How it works
            EloInfoRow("🟢", "Win a match", "Your rating increases. Beat a higher-rated opponent for a bigger boost.", Color(0xFF4CAF50))
            Spacer(Modifier.height(10.dp))
            EloInfoRow("🔴", "Lose a match", "Your rating decreases. Losing to a lower-rated opponent costs more points.", Color(0xFFE53935))
            Spacer(Modifier.height(10.dp))
            EloInfoRow("⚖️", "K-Factor = 24", "Controls how much ratings change per match. Higher K = faster swings.", Color(0xFF2196F3))
            Spacer(Modifier.height(10.dp))
            EloInfoRow("👥", "Doubles", "Team Elo is the average of both partners. Rating change is split equally.", AppAccent)

            Spacer(Modifier.height(14.dp)); Divider(color = Color.Gray.copy(alpha = 0.15f)); Spacer(Modifier.height(14.dp))

            Text("Skill Tiers", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TierRow("👑", "Seed 1", "1600+", Color(0xFF4CAF50))
            TierRow("🏅", "Seed 2", "1400 – 1599", Color(0xFF2196F3))
            TierRow("⭐", "Seed 3", "1200 – 1399", WarningOrange)
            TierRow("🌿", "Seed 4", "Below 1200", Color.Gray)

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("▶️", fontSize = 12.sp)
                Text("All new players start at 1200 Elo (Seed 3).", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun EloInfoRow(emoji: String, title: String, desc: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 14.sp)
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun TierRow(emoji: String, tier: String, range: String, color: Color) {
    Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 12.sp)
        Text(tier, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(55.dp))
        Text(range, fontSize = 12.sp, color = Color.Gray)
    }
}

// ── Fitness & Health ────────────────────────────────

@Composable
private fun FitnessSection(uiState: ProfileUiState, player: Player, onQuickPlay: () -> Unit, onViewHistory: () -> Unit = {}) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Fitness & Health Connect", Icons.Default.LocalFireDepartment)
        Spacer(Modifier.height(10.dp))

        // Health Connect integration card
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.15f))) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Favorite, null, Modifier.size(28.dp), tint = Color.Red)
                    Text("Health Connect Integration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text("TournMate connects with Health Connect to provide accurate fitness tracking for your ${CurrentSport.sport.inlineName} sessions.", fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                HealthDataRow("Reads", "Workout calories from Health Connect", Color(0xFF4CAF50))
                Spacer(Modifier.height(8.dp))
                HealthDataRow("Writes", "Calories burned during ${CurrentSport.sport.inlineName} sessions", Color(0xFF2196F3))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (uiState.sessionsTracked > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Default.LocalFireDepartment, formatCalories(uiState.totalCalories), "Total Calories", WarningOrange, Modifier.weight(1f))
                StatTile(sportIconPainter(CurrentSport.sport), "${uiState.sessionsTracked}", "Sessions", AppAccent, Modifier.weight(1f))
                StatTile(Icons.Default.BarChart, formatCalories(uiState.avgCaloriesPerSession), "Avg / Session", Color(0xFF2196F3), Modifier.weight(1f))
            }

            player.weightKg?.let {
                Spacer(Modifier.height(8.dp))
                Text("⚖️ Weight: ${it.toInt()} kg", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = WarningOrange.copy(alpha = 0.08f),
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("View History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(12.dp), tint = WarningOrange)
                    }
                }
                Surface(
                    onClick = onQuickPlay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = AppAccent.copy(alpha = 0.08f),
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ Quick Play", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                    }
                }
            }
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(36.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Play sessions and log calories to see your fitness stats here.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Surface(onClick = onQuickPlay, shape = RoundedCornerShape(12.dp), color = AppAccent.copy(alpha = 0.08f)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                            Text("⚡ Log a Quick Play", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthDataRow(title: String, detail: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(if (title == "Reads") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, Modifier.size(16.dp), tint = color)
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun formatCalories(value: Double): String =
    if (value >= 1000) String.format("%.1fk", value / 1000) else "${value.toInt()}"

// ── Quick Play Dialog ───────────────────────────────

@Composable
private fun QuickPlayDialog(playerWeight: Double, onDismiss: () -> Unit, onSave: (Int, Double) -> Unit) {
    var durationMinutes by remember { mutableStateOf(60f) }
    var weight by remember { mutableStateOf(playerWeight.toFloat()) }
    val estimatedCal = (5.5 * weight * (durationMinutes / 60f)).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(42.dp), CircleShape, AppAccent.copy(alpha = 0.12f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SportsTennis, null, Modifier.size(22.dp), tint = AppAccent) }
                }
                Column {
                    Text("Quick Play", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Log calories from a casual session", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration", fontSize = 12.sp, color = Color.Gray)
                    Text("${durationMinutes.toInt()} min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                }
                androidx.compose.material3.Slider(value = durationMinutes, onValueChange = { durationMinutes = it }, valueRange = 15f..240f, steps = 14, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = AppAccent, activeTrackColor = AppAccent))

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your weight", fontSize = 12.sp, color = Color.Gray)
                    Text("${weight.toInt()} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                }
                androidx.compose.material3.Slider(value = weight, onValueChange = { weight = it }, valueRange = 30f..200f, steps = 169, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = WarningOrange, activeTrackColor = WarningOrange))

                Spacer(Modifier.height(16.dp))
                Text("Estimated: ~$estimatedCal kcal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarningOrange, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(durationMinutes.toInt(), weight.toDouble()) }, colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)) {
                Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log Calories", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Sportsmanship ───────────────────────────────────

@Composable
private fun SportsmanshipCard() {
    CardSection("Sportsmanship", Icons.Default.Favorite) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(5) { Icon(Icons.Default.StarBorder, null, Modifier.size(24.dp), tint = Color.Gray.copy(alpha = 0.3f)) } }
            Spacer(Modifier.width(14.dp))
            Column { Text("0.0 / 5.0", fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text("0 ratings", fontSize = 12.sp, color = Color.Gray) }
        }
    }
}

// ── Legal ───────────────────────────────────────────

@Composable
private fun LegalSection() {
    val ctx = LocalContext.current
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("Legal", Icons.Default.Description)
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp) {
            Column {
                LegalRow("Terms & Conditions", Icons.Default.Description) { openInBrowser(ctx, "https://www.tournmate.com/terms") }
                Divider(Modifier.padding(start = 44.dp))
                LegalRow("Privacy Policy", Icons.Default.Security) { openInBrowser(ctx, "https://www.tournmate.com/privacy") }
            }
        }
    }
}

@Composable
private fun LegalRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = AppAccent); Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 14.sp); Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, Modifier.size(12.dp), tint = Color.LightGray)
    }
}

// ── Sign Out ────────────────────────────────────────

@Composable
private fun SignOutSection(onSignOut: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp).border(1.5.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(14.dp))) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp), tint = Color.Red); Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDelete) { Icon(Icons.Default.PersonRemove, null, Modifier.size(14.dp), tint = Color.Red); Spacer(Modifier.width(8.dp)); Text("Delete Account", fontSize = 14.sp, color = Color.Red) }
        Spacer(Modifier.height(8.dp))
        Text("v1.0.0 · TournMate", fontSize = 10.sp, color = Color.LightGray)
    }
}

// ── Shared Components ───────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = AppAccent)
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppAccent)
    }
}

@Composable
private fun CardSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(title, icon); Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) { Column(Modifier.fillMaxWidth()) { content() } }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(32.dp), RoundedCornerShape(8.dp), AppAccent.copy(alpha = 0.08f)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(14.dp), tint = AppAccent) } }
        Spacer(Modifier.width(14.dp))
        Column { Text(title, fontSize = 15.sp); if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = Color.Gray) }
    }
}

private fun rankHint(elo: Double) = when { elo < 1400 -> "${(1400 - elo).toInt()} points to next tier"; elo < 1600 -> "${(1600 - elo).toInt()} points to next tier"; elo < 2000 -> "${(2000 - elo).toInt()} points to next tier"; else -> "Top tier achieved!" }

// ── Guest Prompt ────────────────────────────────────

@Composable
private fun GuestPromptScreen(modifier: Modifier, onJoinClick: () -> Unit) {
    Column(modifier.fillMaxSize().background(Color.White), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(130.dp).background(Brush.radialGradient(listOf(AppAccent.copy(alpha = 0.18f), AppAccent.copy(alpha = 0.04f))), CircleShape))
                Box(Modifier.size(96.dp).background(AppAccent, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(44.dp), tint = Color.White.copy(alpha = 0.85f)) }
            }
            Surface(Modifier.size(36.dp).offset(x = 2.dp, y = 2.dp).shadow(4.dp, RoundedCornerShape(10.dp)), RoundedCornerShape(10.dp), Color.Black) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(14.dp), tint = AppAccent) } }
        }
        Text("Guest Mode", fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 24.dp))
        Text("Sign in to see your profile, track your stats,\nand rate other players.", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp, start = 40.dp, end = 40.dp))
        Row(Modifier.padding(top = 32.dp, start = 40.dp, end = 40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(Icons.Default.BarChart, "ANALYTICS", AppAccent, Modifier.weight(1f)); Tile(Icons.Default.EmojiEvents, "LEAGUES", WarningOrange, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onJoinClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(60.dp).background(AppAccent, RoundedCornerShape(30.dp))) {
            Text("JOIN THE ELITE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp); Spacer(Modifier.width(10.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp), tint = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Avatar Picker Dialog ────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarPickerDialog(
    currentAvatarId: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(currentAvatarId) }
    val selectedAvatar = com.s2aglobal.tournmate.domain.model.PlayerAvatar.fromId(selected)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Avatar", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(Modifier.size(80.dp), CircleShape, AppAccent.copy(alpha = 0.1f)) {
                    AsyncImage(selectedAvatar.avatarUrl(128), "Preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.height(8.dp))
                Text(selectedAvatar.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.s2aglobal.tournmate.domain.model.PlayerAvatar.selectable.forEach { avatar ->
                        val isSelected = avatar.id == selected
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .then(if (isSelected) Modifier.border(3.dp, AppAccent, CircleShape) else Modifier.border(1.dp, Color.LightGray, CircleShape))
                                .clickable { selected = avatar.id },
                            shape = CircleShape,
                        ) {
                            AsyncImage(avatar.avatarUrl(64), avatar.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected) }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) {
                Text("Save Avatar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun Tile(icon: ImageVector, label: String, color: Color, modifier: Modifier) {
    Surface(modifier.border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(16.dp)), RoundedCornerShape(16.dp), Color.White) {
        Column(Modifier.padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(22.dp), tint = color); Spacer(Modifier.height(10.dp)); Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
    }
}
