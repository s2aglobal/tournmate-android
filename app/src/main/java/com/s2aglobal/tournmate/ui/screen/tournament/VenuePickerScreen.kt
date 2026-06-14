package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.s2aglobal.tournmate.service.court.CourtResult
import com.s2aglobal.tournmate.service.court.CourtSearchService
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VenuePickerScreen(
    onVenueSelected: (name: String, address: String, latitude: Double, longitude: Double) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val courtSearchService = remember { CourtSearchService(context) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CourtResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val popularExamples = listOf("78641" to "Leander", "78660" to "Pflugerville", "78745" to "Austin")

    fun doSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.length < 2) return
        scope.launch {
            focusManager.clearFocus()
            isSearching = true
            hasSearched = true
            results = try { courtSearchService.searchCourts(trimmed) } catch (_: Exception) { emptyList() }
            isSearching = false
        }
    }

    BackHandler { onCancel() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FA))
            .statusBarsPadding()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            },
    ) {
        // ── Navigation Bar ──
        Surface(color = Color.White, shadowElevation = 0.5.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = BrandPurple, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    "Select Venue",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(72.dp))
            }
        }

        // ── Search Bar ──
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFEFF4),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search venue name, address, or zip code", fontSize = 15.sp, color = Color.Gray)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                        singleLine = true,
                        cursorBrush = SolidColor(BrandPurple),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch(query) }),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList(); hasSearched = false }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = Color.Gray)
                    }
                } else {
                    Surface(
                        onClick = { doSearch(query) },
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = BrandPurple,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }
            }
        }

        // ── Body ──
        when {
            isSearching -> SearchingState()
            results.isNotEmpty() -> ResultsState(results, onVenueSelected)
            hasSearched -> EmptyResultsState(popularExamples) { query = it; doSearch(it) }
            else -> WelcomeState(popularExamples) { query = it; doSearch(it) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Searching Animation (matches iOS radar animation)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SearchingState() {
    val t = rememberInfiniteTransition(label = "radar")
    val ring1 by t.animateFloat(0.6f, 1.2f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "r1")
    val ring2 by t.animateFloat(0.8f, 1.4f, infiniteRepeatable(tween(1200, delayMillis = 200, easing = EaseInOut), RepeatMode.Reverse), label = "r2")
    val ring3 by t.animateFloat(1.0f, 1.6f, infiniteRepeatable(tween(1200, delayMillis = 400, easing = EaseInOut), RepeatMode.Reverse), label = "r3")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                listOf(ring3 to 0.05f, ring2 to 0.08f, ring1 to 0.12f).forEachIndexed { i, (scale, alpha) ->
                    val size = (60 + i * 30).dp
                    Surface(
                        modifier = Modifier.size(size).scale(scale),
                        shape = CircleShape,
                        color = BrandPurple.copy(alpha = alpha),
                    ) {}
                }
                Icon(Icons.Default.SportsTennis, null, Modifier.size(32.dp), tint = BrandPurple)
            }
            Spacer(Modifier.height(24.dp))
            Text("Searching for courts...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Finding badminton facilities\nnear your location", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(Modifier.size(20.dp), BrandPurple, strokeWidth = 2.dp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Welcome (matches iOS hero + popular courts)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun WelcomeState(examples: List<Pair<String, String>>, onSearch: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            // Hero
            Surface(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(20.dp),
                color = BrandPurple.copy(alpha = 0.06f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp).offset(x = (-45).dp, y = (-25).dp), tint = BrandPurple.copy(alpha = 0.7f))
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp).offset(x = 30.dp, y = 12.dp), tint = BrandPurple.copy(alpha = 0.5f))
                    Icon(Icons.Default.LocationOn, null, Modifier.size(12.dp).offset(x = (-12).dp, y = 35.dp), tint = BrandPurple.copy(alpha = 0.4f))
                    Icon(Icons.Default.SportsTennis, null, Modifier.size(42.dp), tint = BrandPurple)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Search for a Badminton Court", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text("Enter a postal code in United States, or use\ncurrent location when you're there.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            // Action cards
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column {
                    // Use Current Location row
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp), tint = BrandPurple)
                        Spacer(Modifier.width(12.dp))
                        Text("Use Current Location", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp), tint = Color.Gray.copy(alpha = 0.4f))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 48.dp), color = Color(0xFFF2F2F7))

                    // Popular Courts row
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(12.dp))
                        Text("Popular Courts", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp), tint = Color.Gray.copy(alpha = 0.4f))
                    }

                    // Pill chips
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        examples.forEach { (zip, label) ->
                            Surface(
                                onClick = { onSearch(zip) },
                                shape = RoundedCornerShape(50),
                                color = BrandPurple.copy(alpha = 0.08f),
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - Results (map + court cards like iOS)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ResultsState(
    results: List<CourtResult>,
    onSelect: (name: String, address: String, latitude: Double, longitude: Double) -> Unit,
) {
    val first = results.first()
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(first.latitude, first.longitude), 11f)
    }

    LazyColumn(Modifier.fillMaxSize()) {
        // Map
        item {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
            ) {
                results.forEach {
                    Marker(state = MarkerState(LatLng(it.latitude, it.longitude)), title = it.name)
                }
            }
        }

        // Results header
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = BrandPurple.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.SportsTennis, null, Modifier.size(12.dp), tint = BrandPurple)
                        Text("${results.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                    }
                }
                Text("court${if (results.size == 1) "" else "s"} found — tap to select", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // Court cards
        itemsIndexed(results, key = { i, c -> "$i-${c.name}" }) { _, court ->
            Surface(
                onClick = { onSelect(court.name, court.address, court.latitude, court.longitude) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.dp,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(court.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (court.address.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.7f))
                            Text(court.address, fontSize = 12.sp, color = Color.Gray, maxLines = 2, lineHeight = 16.sp)
                        }
                    }
                    court.distanceMeters?.let {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.NearMe, null, Modifier.size(12.dp), tint = BrandPurple)
                            Text(CourtSearchService.formatDistance(it), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MARK: - No Results
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun EmptyResultsState(examples: List<Pair<String, String>>, onSearch: (String) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text("No Venues Found Yet", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Search your area to see\navailable courts.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Text("Examples to try:", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { (zip, _) ->
                    Surface(
                        onClick = { onSearch(zip) },
                        shape = RoundedCornerShape(50),
                        color = BrandPurple.copy(alpha = 0.08f),
                    ) {
                        Text(zip, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}
