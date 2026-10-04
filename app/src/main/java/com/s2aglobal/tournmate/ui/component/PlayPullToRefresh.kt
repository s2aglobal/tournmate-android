package com.s2aglobal.tournmate.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import com.s2aglobal.tournmate.ui.theme.AppAccent
import kotlinx.coroutines.launch

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
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                scope.launch {
                    try { currentOnRefresh() } finally { isRefreshing = false }
                }
            }
        },
        modifier = modifier.clipToBounds(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.White,
                color = AppAccent,
            )
        },
    ) {
        content()
    }
}
