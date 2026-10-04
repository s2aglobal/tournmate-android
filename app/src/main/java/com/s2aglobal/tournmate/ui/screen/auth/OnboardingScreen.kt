package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.ui.component.MultiSportArtworkRow
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class OnboardingPage(
    val category: String,
    val title: String,
    val description: String,
    val icon: OnboardingIcon,
    val backgroundGradient: List<Color>,
    /** Shows pickleball, badminton and tennis artwork above the text (iOS `showsSportArtwork`). */
    val showsSportArtwork: Boolean = false,
)

/** Watermark icon: a Material vector, or a drawable traced from the iOS SF Symbol. */
private sealed interface OnboardingIcon {
    data class Vector(val image: ImageVector) : OnboardingIcon
    data class Drawable(@DrawableRes val res: Int) : OnboardingIcon
}

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
    LightSystemBarIcons()
    // Pages, icons, copy and gradients match iOS OnboardingView.swift exactly (same order).
    val pages = listOf(
        OnboardingPage(
            category = "TOURNAMENTS",
            title = "Win Glory",
            description = "Tournaments for your sport. Compete locally and climb the leaderboard rankings.",
            icon = OnboardingIcon.Vector(Icons.Filled.EmojiEvents), // trophy.fill
            backgroundGradient = listOf(Color(0xFF1A245C), Color(0xFF0F1438)),
            showsSportArtwork = true,
        ),
        OnboardingPage(
            category = "OPEN PLAY",
            title = "Game On",
            description = "Host casual sessions for any sport, meet players, and enjoy the game together.",
            icon = OnboardingIcon.Vector(Icons.Filled.Groups), // person.3.fill
            backgroundGradient = listOf(Color(0xFF006147), Color(0xFF003324)),
        ),
        OnboardingPage(
            category = "COURTS",
            title = "Your Turf",
            description = "Find nearby courts and venues by zip code or GPS and get directions instantly.",
            icon = OnboardingIcon.Drawable(R.drawable.ic_sportscourt_fill), // sportscourt.fill
            backgroundGradient = listOf(Color(0xFF143D6B), Color(0xFF0A1F42)),
        ),
        OnboardingPage(
            category = "DISCOVER",
            title = "Stay Sharp",
            description = "Browse news, live matches, rankings, and official rules for your sport.",
            icon = OnboardingIcon.Drawable(R.drawable.ic_safari_fill), // safari.fill
            backgroundGradient = listOf(Color(0xFF381A61), Color(0xFF1F0F3D)),
        ),
        OnboardingPage(
            category = "CALORIES",
            title = "Peak Form",
            // iOS says "Apple Health"; the platform's health store is the only difference.
            description = "Track calories burned with Health Connect or smart MET estimation.",
            icon = OnboardingIcon.Vector(Icons.Filled.LocalFireDepartment), // flame.fill
            backgroundGradient = listOf(Color(0xFF852914), Color(0xFF4D140A)),
        ),
        OnboardingPage(
            category = "NOTIFICATIONS",
            title = "Match Alerts",
            description = "Get notified when events for your sport are posted in your area. Never miss a game.",
            icon = OnboardingIcon.Vector(Icons.Filled.Notifications), // bell.fill
            backgroundGradient = listOf(Color(0xFF291F66), Color(0xFF140F3D)),
        ),
        OnboardingPage(
            category = "PROFILE",
            title = "Your Legacy",
            description = "Track match stats, fitness records, Elo rating, and sportsmanship.",
            icon = OnboardingIcon.Drawable(R.drawable.ic_person_circle), // person.crop.circle.fill
            backgroundGradient = listOf(Color(0xFF4D1A66), Color(0xFF290A38)),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

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
                val watermarkModifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .size(200.dp)
                val watermarkTint = Color.White.copy(alpha = 0.06f)
                when (val icon = page.icon) {
                    is OnboardingIcon.Vector -> Icon(icon.image, null, watermarkModifier, tint = watermarkTint)
                    is OnboardingIcon.Drawable -> Icon(painterResource(icon.res), null, watermarkModifier, tint = watermarkTint)
                }
                // Page text sits between the fixed top bar and the fixed bottom CTA
                // (iOS: TabView between the header HStack and the button, 48pt bottom padding).
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = OnboardingTopBarHeight, bottom = OnboardingCtaHeight + OnboardingCtaBottomPadding)
                        .padding(horizontal = 32.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (page.showsSportArtwork) {
                        MultiSportArtworkRow(size = 52.dp, spacing = 12.dp)
                        Spacer(Modifier.height(20.dp))
                    }
                    // Text content (iOS sizes: 12/bold/2.5 tracking, 42/black, 17 at 55%)
                    Text(
                        page.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp,
                        color = TournmatePurple,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.title,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = 46.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        page.description,
                        fontSize = 17.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 25.sp,
                    )

                    Spacer(Modifier.height(100.dp))
                }
            }
        }

        // Fixed top bar: page indicators + skip (iOS OnboardingView.swift:122-158)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(width = if (selected) 22.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) TournmatePurple else Color.White.copy(alpha = 0.25f)),
                    )
                }
            }

            if (!isLastPage) {
                Text(
                    "SKIP",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { viewModel.markOnboardingSeen(onComplete) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            } else {
                Spacer(Modifier.height(38.dp))
            }
        }

        // Fixed bottom CTA (iOS OnboardingView.swift:169-192)
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    viewModel.markOnboardingSeen(onComplete)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(bottom = OnboardingCtaBottomPadding)
                .fillMaxWidth()
                .height(OnboardingCtaHeight),
            shape = RoundedCornerShape(29.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Text(
                text = if (isLastPage) "GET STARTED" else "CONTINUE",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp,
                color = Color.Black,
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.ChevronRight, null, Modifier.size(13.dp), tint = Color.Black)
        }
    }
}

private val OnboardingTopBarHeight = 50.dp
private val OnboardingCtaHeight = 58.dp
private val OnboardingCtaBottomPadding = 48.dp
