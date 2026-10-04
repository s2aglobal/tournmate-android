package com.s2aglobal.tournmate.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * iOS-like scrolling inside a ModalBottomSheet. Material hands any fling
 * momentum left over at the top of a list to the sheet, which then dips and
 * springs back ("bounces"). iOS sheets just stop at the top; only a deliberate
 * drag pulls the sheet down. This absorbs leftover fling momentum but keeps
 * finger drags flowing to the sheet, so drag-to-dismiss still works.
 *
 * Apply to the scrollable sheet content, before `verticalScroll` / on the lazy list.
 */
fun Modifier.sheetScrollLikeIos(): Modifier = nestedScroll(AbsorbFlingIntoSheet)

private object AbsorbFlingIntoSheet : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.SideEffect) available else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}
