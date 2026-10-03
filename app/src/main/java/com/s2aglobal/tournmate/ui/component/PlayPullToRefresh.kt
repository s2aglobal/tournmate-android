package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.s2aglobal.tournmate.ui.theme.AppAccent

/**
 * Pull-to-refresh wrapper for the Play tab lists (iOS `.refreshable`).
 * Uses a plain spinner for now; swap the indicator for TrophySpinner once it lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPullToRefresh(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    if (state.isRefreshing) {
        LaunchedEffect(Unit) {
            try { currentOnRefresh() } finally { state.endRefresh() }
        }
    }

    Box(modifier = modifier.clipToBounds().nestedScroll(state.nestedScrollConnection)) {
        content()
        PullToRefreshContainer(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Color.White,
            contentColor = AppAccent,
        )
    }
}
