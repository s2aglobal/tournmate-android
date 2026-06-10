package com.s2aglobal.tournmate.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.screen.openplay.OpenPlayListPlaceholder
import com.s2aglobal.tournmate.ui.screen.tournament.TournamentListPlaceholder
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.util.Calendar

@Composable
fun PlayTabScreen(modifier: Modifier = Modifier) {
    var selectedSegment by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)),
    ) {
        // Header matching iOS PlayTabView
        PlayHeader(
            selectedSegment = selectedSegment,
            onHostClick = { /* TODO: Phase 2 */ },
            onNotificationClick = { /* TODO: Phase 7 */ },
        )

        // Custom pill tab switcher
        PillTabSwitcher(
            selectedIndex = selectedSegment,
            onTabSelected = { selectedSegment = it },
        )

        // Content
        when (selectedSegment) {
            0 -> TournamentListPlaceholder()
            1 -> OpenPlayListPlaceholder()
        }
    }
}

@Composable
private fun PlayHeader(
    selectedSegment: Int,
    onHostClick: () -> Unit,
    onNotificationClick: () -> Unit,
) {
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Play.",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
            )
            Text(
                text = "SEASON $year",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.5.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Notification bell
        IconButton(onClick = onNotificationClick) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp),
                tint = Color.Black,
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // HOST button
        Surface(
            onClick = onHostClick,
            shape = RoundedCornerShape(50),
            color = BrandPurple,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HOST",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

@Composable
private fun PillTabSwitcher(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        PillTab("TOURNAMENTS", Icons.Default.EmojiEvents),
        PillTab("OPEN PLAY", Icons.Default.People),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        val totalWidth = maxWidth
        val tabWidth = (totalWidth - 10.dp) / 2
        val pillOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 5.dp else 5.dp + tabWidth,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "pillSlide",
        )

        // Outer track
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFE5E5EA).copy(alpha = 0.55f),
        ) {
            Box {
                // Sliding white pill
                Surface(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .height(38.dp)
                        .align(Alignment.CenterStart)
                        .shadow(6.dp, RoundedCornerShape(19.dp)),
                    shape = RoundedCornerShape(19.dp),
                    color = Color.White,
                ) {}

                // Tab labels
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onTabSelected(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (selectedIndex == index) BrandPurple else Color.Gray,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedIndex == index) BrandPurple else Color.Gray,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PillTab(
    val label: String,
    val icon: ImageVector,
)
