package com.s2aglobal.tournmate.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.domain.model.SeedTierRules
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isGuest: Boolean = false,
    val player: Player? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val firebaseUser = authService.currentUser
            if (firebaseUser == null) {
                _uiState.value = ProfileUiState(isLoading = false, isGuest = true)
                return@launch
            }

            val playerId = currentUserStore.currentPlayerId()
            val player = if (playerId != null) {
                playerRepo.findPlayerById(playerId)
            } else {
                playerRepo.findPlayerByFirebaseUid(firebaseUser.uid)
            }

            _uiState.value = ProfileUiState(
                isLoading = false,
                isGuest = false,
                player = player,
            )
        }
    }
}

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
        topBar = {
            TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 34.sp) })
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPurple)
                }
            }
            uiState.isGuest -> {
                GuestPromptScreen(modifier = Modifier.padding(padding), onJoinClick = onSignOut)
            }
            uiState.player != null -> {
                SignedInProfileScreen(
                    modifier = Modifier.padding(padding),
                    player = uiState.player!!,
                    onSignOut = onSignOut,
                )
            }
            else -> {
                GuestPromptScreen(modifier = Modifier.padding(padding), onJoinClick = onSignOut)
            }
        }
    }
}

@Composable
private fun SignedInProfileScreen(
    modifier: Modifier = Modifier,
    player: Player,
    onSignOut: () -> Unit,
) {
    val tier = SeedTierRules.tier(player.elo)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = BrandPurple.copy(alpha = 0.1f),
        ) {
            AsyncImage(
                model = player.avatar.avatarUrl(128),
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(player.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(player.email, fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem("ELO", "${player.elo.toInt()}")
            StatItem("Streak", "${player.streak}")
            StatItem("Tier", tier.title)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Home region
        if (player.homeCountryCode != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF2F2F7),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Home Region", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${player.homeCountryCode ?: ""} ${player.homePostalCode ?: ""}".trim(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "(Full profile coming in Phase 5+)",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        // Sign out button
        TextButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(26.dp)),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp), tint = Color.Red)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun GuestPromptScreen(
    modifier: Modifier = Modifier,
    onJoinClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BrandPurple.copy(alpha = 0.18f),
                                    BrandPurple.copy(alpha = 0.04f),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(BrandPurple, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BrandPurple,
                    )
                }
            }
        }

        Text(
            text = "Guest Mode",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            modifier = Modifier.padding(top = 24.dp),
        )

        Text(
            text = "Sign in to see your profile, track your stats,\nand rate other players.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 10.dp, start = 40.dp, end = 40.dp),
        )

        Row(
            modifier = Modifier
                .padding(top = 32.dp, start = 40.dp, end = 40.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureTile(Icons.Default.BarChart, "ANALYTICS", BrandPurple, Modifier.weight(1f))
            FeatureTile(Icons.Default.EmojiEvents, "LEAGUES", WarningOrange, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(60.dp)
                .background(BrandPurple, RoundedCornerShape(30.dp)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("JOIN THE ELITE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, label: String, iconColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = iconColor)
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}
