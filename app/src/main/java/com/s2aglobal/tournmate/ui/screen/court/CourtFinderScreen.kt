package com.s2aglobal.tournmate.ui.screen.court

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.service.court.CourtResult
import com.s2aglobal.tournmate.service.court.CourtSearchService
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.net.URLEncoder

private val GroupedBg = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtFinderScreen(
    modifier: Modifier = Modifier,
    viewModel: CourtFinderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    fun fetchCurrentLocation() {
        requestCurrentLocation(context) { lat, lng ->
            viewModel.searchNearLocation(lat, lng)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchCurrentLocation()
    }

    fun onUseCurrentLocation() {
        focusManager.clearFocus()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            fetchCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Find Courts", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = GroupedBg,
                    scrolledContainerColor = GroupedBg,
                ),
            )
        },
        containerColor = GroupedBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    focusManager.clearFocus()
                },
        ) {
            SearchBar(
                query = uiState.zipCode,
                onQueryChange = viewModel::onZipCodeChange,
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.searchByZipCode()
                },
                onClear = { viewModel.resetSearch() },
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!) { viewModel.dismissError() }
                    uiState.courts.isEmpty() && uiState.hasSearched -> NoResultsState(uiState.zipExamples) {
                        viewModel.onZipCodeChange(it)
                        viewModel.searchByZipCode()
                    }
                    uiState.courts.isEmpty() -> WelcomeHero(
                        state = uiState,
                        onUseCurrentLocation = { onUseCurrentLocation() },
                        onExampleClick = {
                            viewModel.onZipCodeChange(it)
                            focusManager.clearFocus()
                            viewModel.searchByZipCode()
                        },
                    )
                    else -> ResultsList(uiState.courts, uiState.zipCode)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Search Bar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasText = query.trim().isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Input field
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = if (isFocused) BorderStroke(1.5.dp, BrandPurple.copy(alpha = 0.5f)) else null,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    null,
                    Modifier.size(18.dp),
                    tint = if (isFocused) BrandPurple else Color.Gray,
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Enter zip or postal code", fontSize = 15.sp, color = Color.Gray)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                        singleLine = true,
                        cursorBrush = SolidColor(BrandPurple),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp), tint = Color.Gray)
                    }
                }
            }
        }

        // Circular submit button (matches iOS)
        Surface(
            onClick = { if (hasText) onSearch() },
            enabled = hasText,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = if (hasText) BrandPurple else Color.Gray.copy(alpha = 0.3f),
            shadowElevation = if (hasText) 3.dp else 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Search", Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Welcome Hero
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun WelcomeHero(
    state: CourtFinderUiState,
    onUseCurrentLocation: () -> Unit,
    onExampleClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.discover_courts),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Discover Courts\nNear You", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            state.welcomeSubtitle,
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(24.dp))

        FeatureRow(Icons.Filled.SportsTennis, BrandPurple, "Courts & Clubs", "Badminton courts, academies, and sports facilities")
        Spacer(Modifier.height(10.dp))
        FeatureRow(Icons.Filled.LocationOn, Color(0xFF2196F3), "Distance Info", "See how far each court is from your area")
        Spacer(Modifier.height(10.dp))
        FeatureRow(Icons.Filled.Directions, Color(0xFFFF9800), "Quick Actions", "Get directions or search their website")
        Spacer(Modifier.height(10.dp))
        if (state.usesHomeRegion) {
            FeatureRow(Icons.Filled.Public, BrandPurple, "Region Aligned", "Matches your Profile home country for open play and tournaments")
        } else {
            FeatureRow(Icons.Filled.Public, BrandPurple, "Set Your Region", "Add home country and postal in Profile for regional discovery")
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            onClick = onUseCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = BrandPurple,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, null, Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Use Current Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Or enter a postal code (e.g. ${state.zipExamples.take(2).joinToString(", ")})",
            fontSize = 12.sp,
            color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.popularExamples.forEach { ex ->
                Surface(
                    onClick = { onExampleClick(ex.zip) },
                    shape = CircleShape,
                    color = BrandPurple.copy(alpha = 0.08f),
                ) {
                    Text(
                        ex.zip,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = color)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Loading
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun LoadingState() {
    val t = rememberInfiniteTransition(label = "radar")
    val ring1 by t.animateFloat(0.6f, 1.2f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "r1")
    val ring2 by t.animateFloat(0.8f, 1.4f, infiniteRepeatable(tween(1200, delayMillis = 200, easing = EaseInOut), RepeatMode.Reverse), label = "r2")
    val ring3 by t.animateFloat(1.0f, 1.6f, infiniteRepeatable(tween(1200, delayMillis = 400, easing = EaseInOut), RepeatMode.Reverse), label = "r3")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                listOf(ring3 to 0.05f, ring2 to 0.08f, ring1 to 0.12f).forEachIndexed { i, (scale, alpha) ->
                    Surface(
                        modifier = Modifier.size((60 + i * 30).dp).scale(scale),
                        shape = CircleShape,
                        color = BrandPurple.copy(alpha = alpha),
                    ) {}
                }
                Icon(painterResource(R.drawable.play_icon), null, Modifier.size(32.dp), tint = BrandPurple)
            }
            Spacer(Modifier.height(24.dp))
            Text("Searching for courts…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Finding badminton facilities\nnear your location", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(Modifier.size(20.dp), color = BrandPurple, strokeWidth = 2.dp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Error
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, null, Modifier.size(52.dp), tint = Color(0xFFFF9800))
            Spacer(Modifier.height(16.dp))
            Text("Search Error", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            Surface(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = BrandPurple,
            ) {
                Text(
                    "OK",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// No Results
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun NoResultsState(examples: List<String>, onExampleClick: (String) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            Icon(Icons.Filled.SearchOff, null, Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text("No Courts Found", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "We couldn't find badminton courts for that area.\nTry a different postal code.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(20.dp))
            Text("Examples to try:", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { zip ->
                    Surface(
                        onClick = { onExampleClick(zip) },
                        shape = CircleShape,
                        color = BrandPurple.copy(alpha = 0.08f),
                    ) {
                        Text(
                            zip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Results
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ResultsList(courts: List<CourtResult>, zipCode: String) {
    val context = LocalContext.current
    val apiKey = remember { mapsApiKey(context) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = BrandPurple.copy(alpha = 0.12f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(painterResource(R.drawable.play_icon), null, Modifier.size(12.dp), tint = BrandPurple)
                    Text("${courts.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                }
            }
            Text("court${if (courts.size == 1) "" else "s"} near", fontSize = 13.sp, color = Color.Gray)
            Text(zipCode.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        courts.forEach { court ->
            CourtCard(court, apiKey)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CourtCard(court: CourtResult, apiKey: String) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable { openDirections(context, court) },
            ) {
                val mapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                    "?center=${court.latitude},${court.longitude}" +
                    "&zoom=15&size=600x300&scale=2&maptype=roadmap" +
                    "&markers=color:0x630893%7C${court.latitude},${court.longitude}" +
                    "&key=$apiKey"
                AsyncImage(
                    model = mapUrl,
                    contentDescription = court.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                court.distanceMeters?.let { dist ->
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                    ) {
                        Text(
                            CourtSearchService.formatDistance(dist),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(court.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = BrandPurple.copy(alpha = 0.1f)) {
                        Text(
                            "Badminton",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                if (court.address.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.6f))
                        Text(court.address, fontSize = 12.sp, color = Color.Gray, maxLines = 2, lineHeight = 16.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GroupedBg)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(Icons.Filled.Directions, "Directions", BrandPurple) { openDirections(context, court) }
                    ActionPill(Icons.Filled.Public, "Website", Color(0xFF9C27B0)) { searchWebsite(context, court) }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = tint.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = tint)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tint)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Helpers
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun mapsApiKey(context: Context): String = try {
    context.packageManager
        .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
} catch (_: Exception) {
    ""
}

private fun openDirections(context: Context, court: CourtResult) {
    val label = Uri.encode(court.name)
    // Try the Google Maps app first (turn-by-turn navigation).
    try {
        val navUri = Uri.parse("google.navigation:q=${court.latitude},${court.longitude}")
        context.startActivity(
            Intent(Intent.ACTION_VIEW, navUri).setPackage("com.google.android.apps.maps"),
        )
        return
    } catch (_: Exception) {
    }
    // Fall back to any map app via a generic geo URI.
    try {
        val geoUri = Uri.parse("geo:${court.latitude},${court.longitude}?q=${court.latitude},${court.longitude}($label)")
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        return
    } catch (_: Exception) {
    }
    // Last resort: open Google Maps directions in the browser.
    com.s2aglobal.tournmate.util.openInBrowser(
        context,
        "https://www.google.com/maps/dir/?api=1&destination=${court.latitude},${court.longitude}",
    )
}

private fun searchWebsite(context: Context, court: CourtResult) {
    val query = URLEncoder.encode("${court.name} official website", "UTF-8")
    com.s2aglobal.tournmate.util.openInBrowser(context, "https://duckduckgo.com/?q=!ducky+$query")
}

@SuppressLint("MissingPermission")
private fun requestCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onLocation(location.latitude, location.longitude)
            } else {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { fresh ->
                        if (fresh != null) onLocation(fresh.latitude, fresh.longitude)
                    }
            }
        }
}
