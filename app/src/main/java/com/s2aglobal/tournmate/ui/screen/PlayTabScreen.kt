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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.screen.openplay.CreateSessionSheet
import com.s2aglobal.tournmate.ui.screen.openplay.OpenPlayListScreen
import com.s2aglobal.tournmate.ui.screen.openplay.OpenPlayListViewModel
import com.s2aglobal.tournmate.ui.screen.tournament.PublishTournamentScreen
import com.s2aglobal.tournmate.ui.screen.tournament.TournamentListScreen
import com.s2aglobal.tournmate.ui.screen.tournament.TournamentListViewModel
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.util.Calendar

@Composable
fun PlayTabScreen(
    modifier: Modifier = Modifier,
    isGuestMode: Boolean = false,
    onNavigateToTournamentDetail: (String) -> Unit = {},
    onNavigateToSessionDetail: (String) -> Unit = {},
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    var showPublish by remember { mutableStateOf(false) }
    var showCreateSession by remember { mutableStateOf(false) }
    val tournamentListVM: TournamentListViewModel = hiltViewModel()
    val openPlayVM: OpenPlayListViewModel = hiltViewModel()
    val uiState by tournamentListVM.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7)),
        ) {
            PlayHeader(
                selectedSegment = selectedSegment,
                showHostButton = !isGuestMode,
                onHostClick = {
                    if (selectedSegment == 0) showPublish = true
                    else showCreateSession = true
                },
                onNotificationClick = { },
            )

            PillTabSwitcher(
                selectedIndex = selectedSegment,
                onTabSelected = { selectedSegment = it },
            )

            when (selectedSegment) {
                0 -> TournamentListScreen(
                    isGuest = isGuestMode,
                    onTournamentClick = { tournament ->
                        onNavigateToTournamentDetail(tournament.id.toString().uppercase())
                    },
                    onHostClick = { showPublish = true },
                    viewModel = tournamentListVM,
                )
                1 -> OpenPlayListScreen(
                    viewModel = openPlayVM,
                    isGuest = isGuestMode,
                    onSessionClick = { session ->
                        onNavigateToSessionDetail(session.id.toString().uppercase())
                    },
                    onHostClick = { showCreateSession = true },
                )
            }
        }

        if (showPublish && !isGuestMode) {
            PublishTournamentScreen(
                firebaseUid = uiState.firebaseUid,
                preferredSport = uiState.preferredSport,
                onPublish = { title, date, location, locationAddress,
                              locationLatitude, locationLongitude,
                              format, matchFormat, formatConfig,
                              randomPairing, registrationDeadline, createdBy,
                              entryFee, currency, paymentInfo, prizeInfo,
                              durationMinutes, ageGroup, sportType ->
                    tournamentListVM.create(
                        title = title, date = date,
                        location = location, locationAddress = locationAddress,
                        locationLatitude = locationLatitude, locationLongitude = locationLongitude,
                        format = format, matchFormat = matchFormat, formatConfig = formatConfig,
                        randomPairing = randomPairing, registrationDeadline = registrationDeadline,
                        createdBy = createdBy, entryFee = entryFee, currency = currency,
                        paymentInfo = paymentInfo, prizeInfo = prizeInfo,
                        durationMinutes = durationMinutes, ageGroup = ageGroup, sportType = sportType,
                    )
                },
                onDismiss = { showPublish = false },
            )
        }

    }

    if (showCreateSession && !isGuestMode) {
        CreateSessionSheet(
            preferredSport = uiState.preferredSport,
            onPost = { title, venue, venueAddress, venueLatitude, venueLongitude,
                       date, durationMinutes, skillLevel, gameType,
                       costPerPerson, currency, notes, ageGroup, sportType ->
                openPlayVM.createSession(
                    title = title, venue = venue, venueAddress = venueAddress,
                    venueLatitude = venueLatitude, venueLongitude = venueLongitude,
                    date = date, durationMinutes = durationMinutes,
                    skillLevel = skillLevel, gameType = gameType,
                    costPerPerson = costPerPerson, currency = currency,
                    notes = notes, ageGroup = ageGroup, sportType = sportType,
                )
            },
            onDismiss = { showCreateSession = false },
        )
    }
}

@Composable
private fun PlayHeader(
    selectedSegment: Int,
    showHostButton: Boolean = true,
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

        IconButton(onClick = onNotificationClick) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp),
                tint = Color.Black,
            )
        }

        if (showHostButton) {
            Spacer(modifier = Modifier.width(6.dp))

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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFE5E5EA).copy(alpha = 0.55f),
        ) {
            Box {
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
