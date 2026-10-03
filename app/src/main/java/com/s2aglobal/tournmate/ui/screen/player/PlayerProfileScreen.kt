package com.s2aglobal.tournmate.ui.screen.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerRating
import com.s2aglobal.tournmate.domain.model.SeedTier
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.AppAccentTint
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import kotlin.math.abs

private val ScreenBg = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    player: Player?,
    ratings: List<PlayerRating>,
    averageRating: Double,
    matchStats: MatchStats,
    currentPlayerId: String?,
    hasRated: Boolean,
    isLoading: Boolean,
    viewerSport: SportType,
    onBack: () -> Unit,
    onRatePlayer: () -> Unit,
    viewModel: PlayerProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(player?.name.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppAccent) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ScreenBg),
            )
        },
    ) { padding ->
        if (player == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppAccent)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading…", fontSize = 15.sp, color = Color.Gray)
                    }
                } else {
                    Text("Player not found", color = Color.Gray)
                }
            }
            return@Scaffold
        }

        val isMyProfile = currentPlayerId != null && currentPlayerId == player.id.toString().uppercase()
        PlayerProfileContent(
            modifier = Modifier.padding(padding),
            player = player,
            elo = player.elo(viewerSport),
            matchStats = matchStats,
            averageRating = averageRating,
            ratingCount = ratings.size,
            recentMatches = uiState.recentMatches,
            isMyProfile = isMyProfile,
            isGuest = uiState.isGuest,
            canRate = uiState.hasCurrentPlayer,
            onRatePlayer = onRatePlayer,
        )
    }
}

@Composable
private fun PlayerProfileContent(
    modifier: Modifier,
    player: Player,
    elo: Double,
    matchStats: MatchStats,
    averageRating: Double,
    ratingCount: Int,
    recentMatches: List<com.s2aglobal.tournmate.domain.model.Match>,
    isMyProfile: Boolean,
    isGuest: Boolean,
    canRate: Boolean,
    onRatePlayer: () -> Unit,
) {
    var showEloInfo by remember { mutableStateOf(false) }
    val tier = SeedTier.fromElo(elo)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Avatar & name
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(player.avatar.avatarUrl()).crossfade(true).build(),
                contentDescription = player.name,
                modifier = Modifier.size(80.dp).clip(CircleShape).background(AppAccent.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop,
            )
            Text(player.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (player.gender != Gender.PREFER_NOT_TO_SAY) {
                Badge(player.gender.displayName, if (player.gender == Gender.MALE) InfoBlue else Color(0xFFE91E63))
            }
        }

        if (isMyProfile) {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(AppAccentTint, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.HowToReg, null, Modifier.size(18.dp), tint = AppAccent)
                Text("This is your profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppAccent)
            }
        }

        // Match stats
        Section("Match Stats", rememberVectorPainter(Icons.Filled.BarChart)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val winRate = if (matchStats.played == 0) 0 else (matchStats.wins * 100 / matchStats.played)
                StatTile("Matches", "${matchStats.played}", AppAccent, Modifier.weight(1f))
                StatTile("Wins", "${matchStats.wins}", SuccessGreen, Modifier.weight(1f))
                StatTile("Win Rate", "$winRate%", InfoBlue, Modifier.weight(1f))
            }
        }

        // Skill rating
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Skill Rating", rememberVectorPainter(Icons.Filled.Stars))
                Spacer(Modifier.weight(1f))
                Icon(
                    if (showEloInfo) Icons.Filled.Cancel else Icons.Outlined.Info,
                    if (showEloInfo) "Hide Elo info" else "What is Elo?",
                    Modifier.size(20.dp).clickable { showEloInfo = !showEloInfo },
                    tint = if (showEloInfo) Color.Gray else AppAccent,
                )
            }
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Elo", fontSize = 12.sp, color = Color.Gray)
                        Text("${elo.toInt()}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Badge(tier.title, tier.badgeColor)
                        if (player.streak != 0) {
                            val winning = player.streak > 0
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(
                                    if (winning) Icons.Filled.LocalFireDepartment else Icons.Filled.ArrowCircleDown, null,
                                    Modifier.size(14.dp), tint = if (winning) WarningOrange else LossRed,
                                )
                                Text("${abs(player.streak)} ${if (winning) "win" else "loss"} streak", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(showEloInfo, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                EloExplainerCard(cornerRadius = 14.dp)
            }
        }

        // Sportsmanship
        Section("Sportsmanship", rememberVectorPainter(Icons.Filled.Favorite), circled = true) {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(starsText(averageRating), fontSize = 22.sp, color = WarningOrange)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(String.format("%.1f / 5.0", averageRating), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("$ratingCount rating${if (ratingCount == 1) "" else "s"}", fontSize = 12.sp, color = Color.Gray)
                    }
                    when {
                        isMyProfile -> Badge("Your Profile", AppAccent)
                        isGuest -> Text("Sign in to rate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                        canRate -> Surface(onClick = onRatePlayer, shape = RoundedCornerShape(50), color = AppAccent.copy(alpha = 0.12f)) {
                            Text("Rate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                        else -> Text("Register to rate", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Recent matches
        Section("Recent Matches", painterResource(R.drawable.ic_sportscourt_fill)) {
            if (recentMatches.isEmpty()) {
                Card {
                    Text("No finished matches yet.", fontSize = 15.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            } else {
                val top = recentMatches.take(10)
                Card {
                    Column {
                        top.forEachIndexed { index, match ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(match.historyTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(match.historyScoreLine, fontSize = 12.sp, color = Color.Gray)
                            }
                            if (index < top.lastIndex) Divider(Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun starsText(avg: Double): String {
    val rounded = Math.round(avg).toInt()
    return "★".repeat(rounded.coerceIn(0, 5)) + "☆".repeat((5 - rounded).coerceAtLeast(0))
}

@Composable
private fun Section(title: String, icon: Painter, circled: Boolean = false, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, icon, circled)
        content()
    }
}

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
private fun Card(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color.White, shadowElevation = 2.dp) { content() }
}

@Composable
private fun StatTile(title: String, value: String, color: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), Color.White, shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

data class MatchStats(
    val played: Int = 0,
    val wins: Int = 0,
)
