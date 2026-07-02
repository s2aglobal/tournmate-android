package com.s2aglobal.tournmate.ui.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.text.SimpleDateFormat
import java.util.Locale

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
    onBack: () -> Unit,
    onRatePlayer: () -> Unit,
) {
    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator(color = BrandPurple)
            else Text("Player not found", color = Color.Gray)
        }
        return
    }

    val tier = SeedTier.fromElo(player.elo)
    val isOwnProfile = currentPlayerId == player.id.toString().uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F2F7)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(player.avatar.avatarUrl())
                                .crossfade(true)
                                .build(),
                            contentDescription = player.name,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            player.name.ifEmpty { "Player" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (player.gender) {
                                    Gender.MALE -> Color(0xFF007AFF).copy(alpha = 0.15f)
                                    Gender.FEMALE -> Color(0xFFFF2D55).copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                },
                            ) {
                                Text(
                                    player.gender.displayName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when (player.gender) {
                                        Gender.MALE -> Color(0xFF007AFF)
                                        Gender.FEMALE -> Color(0xFFFF2D55)
                                        else -> Color.Gray
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = tier.color.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    tier.displayName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tier.color,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ELO Rating", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatColumn("Rating", "${player.elo.toInt()}", BrandPurple)
                            StatColumn("Tier", tier.displayName, tier.color)
                            StatColumn("Streak", "${player.streak}", Color(0xFF34C759))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Match Stats", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatColumn("Played", "${matchStats.played}", Color.DarkGray)
                            StatColumn("Wins", "${matchStats.wins}", Color(0xFF34C759))
                            StatColumn(
                                "Win Rate",
                                if (matchStats.played > 0) "${(matchStats.wins * 100 / matchStats.played)}%"
                                else "—",
                                BrandPurple,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sportsmanship", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                            Spacer(modifier = Modifier.weight(1f))
                            if (averageRating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFCC00))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        String.format("%.1f", averageRating),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        " (${ratings.size})",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }

                        if (!isOwnProfile && !hasRated) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRatePlayer,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Default.Star, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rate Sportsmanship", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (ratings.isNotEmpty()) {
                items(ratings.take(5), key = { it.id }) { rating ->
                    RatingRow(rating = rating)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun RatingRow(rating: PlayerRating) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row {
                    repeat(5) { idx ->
                        Icon(
                            imageVector = if (idx < rating.stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (idx < rating.stars) Color(0xFFFFCC00) else Color.LightGray,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    dateFormatter.format(rating.createdAt),
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
            }
            if (!rating.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    rating.comment,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

data class MatchStats(
    val played: Int = 0,
    val wins: Int = 0,
)
