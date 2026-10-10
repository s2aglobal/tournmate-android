package com.s2aglobal.tournmate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Slot { Main, Extras, Footer }

/**
 * Non-scrolling empty-state layout (iOS `StickyEmptyState`). Hero + title + subtitle
 * (+ optional [extras]) are centered in the available space and [footer] (the CTA) is
 * pinned to the bottom above the floating tab bar. When the viewport is too short the
 * [extras] are dropped, so the screen never scrolls and text always wraps instead of
 * truncating.
 */
@Composable
fun StickyEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    hero: @Composable () -> Unit,
    extras: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val bottomInset = LocalTabBarClearance.current + TabBarContentGap
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val maxH = constraints.maxHeight
        val maxW = constraints.maxWidth
        val loose = Constraints(maxWidth = maxW)
        val bottomPx = bottomInset.roundToPx()
        val gapPx = 20.dp.roundToPx()

        val footerPlaceable = footer?.let {
            subcompose(Slot.Footer) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { it() }
            }.map { p -> p.measure(loose) }
        }.orEmpty()
        val footerH = footerPlaceable.maxOfOrNull { it.height }?.let { it + 12.dp.roundToPx() } ?: 0

        val mainPlaceable = subcompose(Slot.Main) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                hero()
                Column(
                    Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                    )
                    androidx.compose.material3.Text(
                        subtitle,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }.map { it.measure(loose) }
        val mainH = mainPlaceable.sumOf { it.height }

        val extrasPlaceable = extras?.let {
            subcompose(Slot.Extras) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { it() }
            }.map { p -> p.measure(loose) }
        }.orEmpty()
        val extrasH = extrasPlaceable.sumOf { it.height }

        val available = maxH - footerH - bottomPx
        val showExtras = extrasPlaceable.isNotEmpty() && mainH + gapPx + extrasH <= available
        val bodyH = mainH + if (showExtras) gapPx + extrasH else 0
        val top = ((available - bodyH) / 2).coerceAtLeast(0)

        layout(maxW, maxH) {
            var y = top
            mainPlaceable.forEach { it.placeRelative(0, y); y += it.height }
            if (showExtras) {
                y += gapPx
                extrasPlaceable.forEach { it.placeRelative(0, y); y += it.height }
            }
            val footerY = maxH - bottomPx - footerH
            footerPlaceable.forEach { it.placeRelative(0, footerY) }
        }
    }
}
