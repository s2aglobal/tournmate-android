package com.s2aglobal.tournmate.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.ui.screen.court.CourtFinderScreen
import com.s2aglobal.tournmate.ui.screen.discover.DiscoverScreen
import com.s2aglobal.tournmate.ui.screen.profile.ProfilePlaceholder
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.CurrentSport

/** Outline icon when unselected, filled when selected — same as the iOS SF Symbol pairs. */
private data class TabItem(
    val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int = iconRes,
)

private val TabUnselected = Color(0xFF1C1C1E)
private val TabHighlight = Color(0xFFE9E9EE)

@Composable
fun MainScreen(
    isGuestMode: Boolean = false,
    onSignOut: () -> Unit,
    onNavigateToTournamentDetail: (String) -> Unit = {},
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToPlayerProfile: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
) {
    val tabs = remember {
        listOf(
            TabItem(R.string.tab_play, R.drawable.ic_figure_badminton),
            TabItem(R.string.tab_courts, R.drawable.ic_sportscourt, R.drawable.ic_sportscourt_fill),
            TabItem(R.string.tab_discover, R.drawable.ic_safari, R.drawable.ic_safari_fill),
            TabItem(R.string.tab_profile, R.drawable.ic_person_circle_outline, R.drawable.ic_person_circle),
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        // Tabs use the iOS grouped background; match it so the status-bar strip
        // above each tab (Scaffold padding) isn't a different shade.
        containerColor = Color(0xFFF2F2F7),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                // Play tab icon follows the user's sport.
                                painter = when {
                                    index == 0 -> sportIconPainter(CurrentSport.sport)
                                    selectedTab == index -> painterResource(tab.selectedIconRes)
                                    else -> painterResource(tab.iconRes)
                                },
                                contentDescription = stringResource(tab.titleRes),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                stringResource(tab.titleRes),
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppAccent,
                            selectedTextColor = AppAccent,
                            // iOS tab bar: unselected items use the primary label colour, and the
                            // selected item sits on a soft grey highlight.
                            unselectedIconColor = TabUnselected,
                            unselectedTextColor = TabUnselected,
                            indicatorColor = TabHighlight,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> PlayTabScreen(
                modifier = Modifier.padding(padding),
                isGuestMode = isGuestMode,
                onNavigateToTournamentDetail = onNavigateToTournamentDetail,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
                onNavigateToNotifications = onNavigateToNotifications,
            )
            1 -> CourtFinderScreen(modifier = Modifier.padding(padding))
            2 -> DiscoverScreen(modifier = Modifier.padding(padding))
            3 -> ProfilePlaceholder(modifier = Modifier.padding(padding), onSignOut = onSignOut)
        }
    }
}
