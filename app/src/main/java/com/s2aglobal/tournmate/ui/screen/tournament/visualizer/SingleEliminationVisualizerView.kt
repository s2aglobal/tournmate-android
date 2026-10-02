package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import kotlin.math.pow

private val CARD_WIDTH = 150.dp
private val CARD_HEIGHT = 56.dp
private val CONNECTOR_WIDTH = 28.dp
private val BASE_SPACING = 10.dp

@Composable
fun SingleEliminationVisualizerView(
    state: TournamentVisualState,
    onMatchTap: ((MatchNode) -> Unit)? = null,
) {
    if (state.rounds.isEmpty()) return

    Column(Modifier.fillMaxWidth()) {
        state.champion?.let { ChampionBadge(it.teamName) }
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.padding(8.dp)) {
                state.rounds.forEachIndexed { index, round ->
                    // Round column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Round header
                        Text(
                            round.roundName.uppercase(),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = AppAccent, letterSpacing = 0.5.sp,
                            modifier = Modifier.width(CARD_WIDTH),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))

                        // Match cards with exponential spacing
                        val spacing = spacingForRound(index + 1)
                        val topPad = topPaddingForRound(index + 1)

                        Column(modifier = Modifier.padding(top = topPad)) {
                            round.matches.forEachIndexed { mIdx, matchNode ->
                                BracketMatchCard(matchNode, onTap = { onMatchTap?.invoke(matchNode) })
                                if (mIdx < round.matches.size - 1) {
                                    Spacer(Modifier.height(spacing))
                                }
                            }
                        }
                    }

                    // Connector lines between rounds
                    if (index < state.rounds.size - 1) {
                        val matchCount = round.matches.size
                        val roundNum = index + 1
                        val spacing = spacingForRound(roundNum)
                        val topPad = topPaddingForRound(roundNum) + 8.dp
                        val nextTopPad = topPaddingForRound(roundNum + 1) + 8.dp
                        val nextSpacing = spacingForRound(roundNum + 1)

                        val density = LocalDensity.current
                        val cardH = with(density) { CARD_HEIGHT.toPx() }
                        val spacingPx = with(density) { spacing.toPx() }
                        val nextSpacingPx = with(density) { nextSpacing.toPx() }
                        val topPadPx = with(density) { topPad.toPx() }
                        val nextTopPadPx = with(density) { nextTopPad.toPx() }
                        val connW = with(density) { CONNECTOR_WIDTH.toPx() }
                        val slotH = cardH + spacingPx
                        val nextSlotH = cardH + nextSpacingPx
                        val pairCount = matchCount / 2

                        val totalHeight = topPad + CARD_HEIGHT * matchCount + spacing * (matchCount - 1).coerceAtLeast(0)

                        Canvas(
                            modifier = Modifier
                                .width(CONNECTOR_WIDTH)
                                .height(totalHeight)
                                .padding(top = 20.dp),
                        ) {
                            val lineColor = Color.Gray.copy(alpha = 0.3f)
                            for (i in 0 until pairCount) {
                                val topY = topPadPx + (i * 2) * slotH + cardH / 2
                                val bottomY = topPadPx + (i * 2 + 1) * slotH + cardH / 2
                                val midX = connW / 2
                                val nextY = nextTopPadPx + i * nextSlotH + cardH / 2

                                // Top horizontal
                                drawLine(lineColor, Offset(0f, topY), Offset(midX, topY), strokeWidth = 2f, cap = StrokeCap.Round)
                                // Vertical
                                drawLine(lineColor, Offset(midX, topY), Offset(midX, bottomY), strokeWidth = 2f, cap = StrokeCap.Round)
                                // Bottom horizontal
                                drawLine(lineColor, Offset(0f, bottomY), Offset(midX, bottomY), strokeWidth = 2f, cap = StrokeCap.Round)
                                // Connector to next round
                                drawLine(lineColor, Offset(midX, (topY + bottomY) / 2), Offset(connW, nextY), strokeWidth = 2f, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketMatchCard(node: MatchNode, onTap: () -> Unit) {
    val isFinished = node.isFinished
    val winnerIsA = node.match.winnerRegistrationId == node.match.teamAId
    val winnerIsB = node.match.winnerRegistrationId == node.match.teamBId

    Surface(
        onClick = onTap,
        modifier = Modifier.width(CARD_WIDTH).height(CARD_HEIGHT),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Team A row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val aColor = if (winnerIsA && isFinished) AppAccent else Color.Black
                Text(
                    node.teamAName, fontSize = 11.sp, fontWeight = if (winnerIsA && isFinished) FontWeight.Bold else FontWeight.Normal,
                    color = aColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    "${node.match.scoreA ?: "-"}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (winnerIsA && isFinished) AppAccent else Color.Gray,
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Gray.copy(alpha = 0.15f)))
            Spacer(Modifier.height(2.dp))
            // Team B row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val bColor = if (winnerIsB && isFinished) AppAccent else Color.Black
                Text(
                    node.teamBName, fontSize = 11.sp, fontWeight = if (winnerIsB && isFinished) FontWeight.Bold else FontWeight.Normal,
                    color = bColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    "${node.match.scoreB ?: "-"}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (winnerIsB && isFinished) AppAccent else Color.Gray,
                )
            }
        }
    }
}

private fun spacingForRound(round: Int): Dp =
    BASE_SPACING * 2f.pow(round - 1)

private fun topPaddingForRound(round: Int): Dp {
    if (round <= 1) return 0.dp
    val prevSlot = CARD_HEIGHT + spacingForRound(round - 1)
    return (prevSlot - CARD_HEIGHT) / 2 + topPaddingForRound(round - 1)
}

