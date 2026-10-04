package com.s2aglobal.tournmate.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.ui.screen.court.CourtFinderScreen
import com.s2aglobal.tournmate.ui.screen.discover.DiscoverScreen
import com.s2aglobal.tournmate.ui.screen.profile.ProfilePlaceholder
import com.s2aglobal.tournmate.ui.component.LocalTabBarClearance
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.CurrentSport

/**
 * The iOS 26 floating (Liquid Glass) tab bar renders the filled symbol for every item,
 * so both states use the filled drawable.
 */
private data class TabItem(
    val titleRes: Int,
    @DrawableRes val iconRes: Int,
)

private val TabUnselected = Color(0xFF1C1C1E)
private val TabHighlight = Color(0xFFE9E9EE)

private val TabBarHeight = 62.dp
private val TabBarVerticalMargin = 8.dp

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
            TabItem(R.string.tab_courts, R.drawable.ic_sportscourt_fill),
            TabItem(R.string.tab_discover, R.drawable.ic_safari_fill),
            TabItem(R.string.tab_profile, R.drawable.ic_person_circle),
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        // Tabs use the iOS grouped background; match it so the status-bar strip
        // above each tab (Scaffold padding) isn't a different shade.
        containerColor = Color(0xFFF2F2F7),
    ) { padding ->
        // Only the top inset goes to the tabs: content scrolls underneath the floating bar.
        val contentModifier = Modifier.padding(top = padding.calculateTopPadding())
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val clearance = navBarBottom + TabBarHeight + TabBarVerticalMargin * 2
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalTabBarClearance provides clearance) {
            when (selectedTab) {
                0 -> PlayTabScreen(
                    modifier = contentModifier,
                    isGuestMode = isGuestMode,
                    onNavigateToTournamentDetail = onNavigateToTournamentDetail,
                    onNavigateToSessionDetail = onNavigateToSessionDetail,
                    onNavigateToNotifications = onNavigateToNotifications,
                )
                1 -> CourtFinderScreen(modifier = contentModifier)
                2 -> DiscoverScreen(modifier = contentModifier)
                3 -> ProfilePlaceholder(modifier = contentModifier, onSignOut = onSignOut)
            }
            }

            FloatingTabBar(
                tabs = tabs,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** iOS 26 floating capsule tab bar. */
@Composable
private fun FloatingTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = TabBarVerticalMargin)
            .fillMaxWidth()
            .height(TabBarHeight)
            .shadow(
                16.dp,
                CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .background(Color.White, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            // Play tab icon follows the user's sport.
            val painter: Painter =
                if (index == 0) sportIconPainter(CurrentSport.sport) else painterResource(tab.iconRes)
            val tint = if (selected) AppAccent else TabUnselected
            val title = stringResource(tab.titleRes)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (selected) TabHighlight else Color.Transparent, CircleShape)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = tint,
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
