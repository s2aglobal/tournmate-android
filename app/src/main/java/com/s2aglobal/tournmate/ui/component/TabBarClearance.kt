package com.s2aglobal.tournmate.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height the floating tab bar (plus the system navigation bar) covers at the bottom of a tab.
 * Tab content scrolls underneath the bar, so scrollable tab content pads its bottom by this
 * (plus a small gap) to keep the last item reachable. Zero outside MainScreen.
 */
val LocalTabBarClearance = staticCompositionLocalOf<Dp> { 0.dp }

/** Gap kept between the last scrollable item and the top of the floating tab bar. */
val TabBarContentGap: Dp = 24.dp
