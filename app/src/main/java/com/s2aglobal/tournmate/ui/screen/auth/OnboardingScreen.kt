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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
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
            icon = Icons.Default.EmojiEvents,
            title = stringResource(R.string.onboarding_title_1),
            description = stringResource(R.string.onboarding_desc_1),
        ),
        OnboardingPage(
            icon = Icons.Default.People,
            title = stringResource(R.string.onboarding_title_2),
            description = stringResource(R.string.onboarding_desc_2),
        ),
        OnboardingPage(
            icon = Icons.Default.TrendingUp,
            title = stringResource(R.string.onboarding_title_3),
            description = stringResource(R.string.onboarding_desc_3),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { viewModel.markOnboardingSeen(onComplete) }) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(3f),
        ) { page ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = pages[page].icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = BrandPurple,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pages[page].description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) BrandPurple
                            else Color.LightGray
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    viewModel.markOnboardingSeen(onComplete)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
        ) {
            Text(
                text = if (pagerState.currentPage == pages.size - 1) stringResource(R.string.get_started)
                else stringResource(R.string.next),
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
