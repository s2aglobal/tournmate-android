package com.s2aglobal.tournmate.ui.screen.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class OnboardingPage(
    val category: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundGradient: List<Color>,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {
    fun markOnboardingSeen(onComplete: () -> Unit) {
        viewModelScope.launch {
            currentUserStore.setHasSeenOnboarding(true)
            onComplete()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit,
) {
    val pages = listOf(
        OnboardingPage(
            category = "TOURNAMENTS",
            title = "Win Glory",
            description = "Compete in local tournaments and climb the leaderboard rankings.",
            icon = Icons.Default.EmojiEvents,
            backgroundGradient = listOf(Color(0xFF0D1B3E), Color(0xFF1A237E)),
        ),
        OnboardingPage(
            category = "OPEN PLAY",
            title = "Rally Up",
            description = "Host casual sessions, meet players, and enjoy the game together.",
            icon = Icons.Default.SportsTennis,
            backgroundGradient = listOf(Color(0xFF0D3B2E), Color(0xFF1B5E20)),
        ),
        OnboardingPage(
            category = "COURTS",
            title = "Your Turf",
            description = "Find nearby courts by zip code or GPS and get directions instantly.",
            icon = Icons.Default.Stadium,
            backgroundGradient = listOf(Color(0xFF0D1B3E), Color(0xFF1A237E)),
        ),
        OnboardingPage(
            category = "DISCOVER",
            title = "Stay Sharp",
            description = "Browse badminton news, live matches, rankings, and official rules.",
            icon = Icons.Default.Explore,
            backgroundGradient = listOf(Color(0xFF2E1A47), Color(0xFF4A148C)),
        ),
        OnboardingPage(
            category = "CALORIES",
            title = "Peak Form",
            description = "Track calories burned with Health Connect or smart MET estimation.",
            icon = Icons.Default.LocalFireDepartment,
            backgroundGradient = listOf(Color(0xFF4E1A0D), Color(0xFFBF360C)),
        ),
        OnboardingPage(
            category = "NOTIFICATIONS",
            title = "Match Alerts",
            description = "Get notified when events are posted in your area. Never miss a game.",
            icon = Icons.Default.Notifications,
            backgroundGradient = listOf(Color(0xFF1A1A3E), Color(0xFF311B92)),
        ),
        OnboardingPage(
            category = "PROFILE",
            title = "Your Legacy",
            description = "Track match stats, fitness records, Elo rating, and sportsmanship.",
            icon = Icons.Default.Person,
            backgroundGradient = listOf(Color(0xFF2E1A47), Color(0xFF4A148C)),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(page.backgroundGradient)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 32.dp),
                ) {
                    // Top bar: indicators + skip
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Page indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(pages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (pagerState.currentPage == index)
                                                Modifier.size(width = 20.dp, height = 6.dp)
                                            else Modifier.size(6.dp)
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) BrandPurple
                                            else Color.White.copy(alpha = 0.3f)
                                        ),
                                )
                            }
                        }

                        TextButton(onClick = { viewModel.markOnboardingSeen(onComplete) }) {
                            Text("SKIP", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Icon in center
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(140.dp),
                            tint = Color.White.copy(alpha = 0.15f),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Text content
                    Text(
                        page.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = BrandPurple,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        page.title,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 24.sp,
                    )

                    Spacer(Modifier.height(40.dp))

                    // Button
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                viewModel.markOnboardingSeen(onComplete)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pages.size - 1) "GET STARTED  \u203A" else "CONTINUE  \u203A",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black,
                        )
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
