package com.s2aglobal.tournmate.ui.screen.discover

import com.s2aglobal.tournmate.ui.component.LocalTabBarClearance
import com.s2aglobal.tournmate.ui.component.TabBarContentGap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.s2aglobal.tournmate.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s2aglobal.tournmate.domain.model.rulebook
import com.s2aglobal.tournmate.domain.model.RuleSection
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.AppAccentTint
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.util.openInBrowser

private val GroupedBg = Color(0xFFF2F2F7)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val sport by viewModel.preferredSport.collectAsStateWithLifecycle()
    var selectedRule by remember { mutableStateOf<RuleSection?>(null) }

    val activeRule = selectedRule
    if (activeRule != null) {
        RuleDetailScreen(
            modifier = modifier,
            section = activeRule,
            source = sport.rulebook?.source ?: "Official governing body",
            onBack = { selectedRule = null },
        )
        return
    }

    DiscoverContent(
        modifier = modifier,
        sport = sport,
        onRuleClick = { selectedRule = it },
    )
}

@Composable
private fun DiscoverContent(
    modifier: Modifier = Modifier,
    sport: SportType,
    onRuleClick: (RuleSection) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroupedBg),
    ) {
        Text(
            "Discover",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(GroupedBg)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
        HeaderSection(sport)

        QuickLinksBar(sport) { openInBrowser(context, it.url) }

        NewsSection(sport) { openInBrowser(context, it.url) }

        ListSection(
            title = "Live Matches",
            icon = Icons.Filled.Sensors,
            links = DiscoverData.liveMatches(sport),
            onClick = { openInBrowser(context, it.url) },
        )

        ListSection(
            title = "World Rankings",
            icon = Icons.Filled.BarChart,
            links = DiscoverData.worldRankings(sport),
            onClick = { openInBrowser(context, it.url) },
        )

        GridSection(
            title = "Highlights & Videos",
            icon = Icons.Filled.PlayCircle,
            items = DiscoverData.highlights(sport).map {
                GridCardData(it.icon, it.title, it.subtitle) { openInBrowser(context, it.url) }
            },
        )

        sport.rulebook?.let { rulebook ->
            GridSection(
                title = "${sport.displayName} Rules",
                icon = Icons.Filled.MenuBook,
                items = rulebook.sections.map { section ->
                    GridCardData(section.icon, section.title, section.subtitle) { onRuleClick(section) }
                },
            )
        }

        // Column spacing (24) + this keeps the last section above the floating tab bar.
        Spacer(Modifier.height(LocalTabBarClearance.current))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Header
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun HeaderSection(sport: SportType) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(AppAccent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sportIconPainter(sport), null, Modifier.size(34.dp), tint = AppAccent)
        }
        Text("Your ${sport.displayName} Hub", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "News, live scores, rankings & more — all in one place.",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Quick Links
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun QuickLinksBar(sport: SportType, onClick: (DiscoverLink) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(DiscoverData.quickLinks(sport)) { link ->
            Surface(
                onClick = { onClick(link) },
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppAccent.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(link.icon, null, Modifier.size(15.dp), tint = AppAccent)
                    Text(link.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppAccent)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Section header
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = AppAccent)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// News (horizontal cards)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun NewsSection(sport: SportType, onClick: (DiscoverLink) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Latest News", Icons.Filled.Newspaper)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(DiscoverData.latestNews(sport)) { link ->
                Surface(
                    onClick = { onClick(link) },
                    modifier = Modifier.width(200.dp).height(190.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(AppAccent.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(link.icon, null, Modifier.size(22.dp), tint = AppAccent)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ArrowOutward, null, Modifier.size(14.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(link.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(link.subtitle, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp)
                        Spacer(Modifier.weight(1f))
                        Surface(shape = CircleShape, color = AppAccent.copy(alpha = 0.10f)) {
                            Text(
                                link.source,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// List section (vertical rows)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ListSection(
    title: String,
    icon: ImageVector,
    links: List<DiscoverLink>,
    onClick: (DiscoverLink) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, icon)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
        ) {
            Column {
                links.forEachIndexed { index, link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onClick(link) })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(AppAccent.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(link.icon, null, Modifier.size(20.dp), tint = AppAccent)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(link.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(link.subtitle, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text(link.source, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AppAccent)
                        Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = AppAccent)
                    }
                    if (index < links.size - 1) {
                        HorizontalDivider(Modifier.padding(start = 72.dp), color = GroupedBg)
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Grid section (2 columns)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class GridCardData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
private fun GridSection(title: String, icon: ImageVector, items: List<GridCardData>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, icon)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        GridCard(item, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GridCard(data: GridCardData, modifier: Modifier = Modifier) {
    Surface(
        onClick = data.onClick,
        modifier = modifier.heightIn(min = 130.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AppAccent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(data.icon, null, Modifier.size(18.dp), tint = AppAccent)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(14.dp), tint = Color.Gray.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(10.dp))
            Text(data.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(data.subtitle, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Rule Detail
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleDetailScreen(
    section: RuleSection,
    source: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }

    // Hosted inside MainScreen's Scaffold, whose padding already clears the status
    // bar and bottom nav bar — so no system-bar insets here (iOS inline nav title).
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        section.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppAccent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GroupedBg),
                windowInsets = WindowInsets(0),
            )
        },
        containerColor = GroupedBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(AppAccentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(section.icon, null, Modifier.size(26.dp), tint = AppAccent)
                }
                Text(section.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(section.subtitle, fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }

            section.rules.forEachIndexed { index, rule ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(AppAccentTint),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                            }
                            Text(rule.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(rule.content, fontSize = 16.sp, color = Color.Black.copy(alpha = 0.85f), lineHeight = 24.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
