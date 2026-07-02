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
// ProfilePlaceholder now lives in ProfileScreen.kt
import com.s2aglobal.tournmate.ui.theme.BrandPurple

private data class TabItem(
    val titleRes: Int,
    @DrawableRes val iconRes: Int,
)

@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    onNavigateToTournamentDetail: (String) -> Unit = {},
    onNavigateToSessionDetail: (String) -> Unit = {},
    onNavigateToPlayerProfile: (String) -> Unit = {},
) {
    val tabs = remember {
        listOf(
            TabItem(R.string.tab_play, R.drawable.ic_badminton),
            TabItem(R.string.tab_courts, R.drawable.ic_sportscourt),
            TabItem(R.string.tab_discover, R.drawable.ic_safari),
            TabItem(R.string.tab_profile, R.drawable.ic_person_circle),
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
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
                                painter = painterResource(tab.iconRes),
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
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> PlayTabScreen(
                modifier = Modifier.padding(padding),
                onNavigateToTournamentDetail = onNavigateToTournamentDetail,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
            )
            1 -> CourtFinderScreen(modifier = Modifier.padding(padding))
            2 -> DiscoverScreen(modifier = Modifier.padding(padding))
            3 -> ProfilePlaceholder(modifier = Modifier.padding(padding), onSignOut = onSignOut)
        }
    }
}
