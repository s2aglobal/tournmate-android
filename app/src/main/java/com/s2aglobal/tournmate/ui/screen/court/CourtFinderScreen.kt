package com.s2aglobal.tournmate.ui.screen.court

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Apps
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.service.court.CourtResult
import com.s2aglobal.tournmate.service.court.CourtSearchService
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import java.net.URLEncoder

private val GroupedBg = Color(0xFFF2F2F7)
private val CallBlue = Color(0xFF2196F3)

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroupedBg)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                focusManager.clearFocus()
            },
    ) {
        Text(
            "Find Courts",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(GroupedBg)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )

        SearchBar(
            query = uiState.zipCode,
            onQueryChange = viewModel::onZipCodeChange,
            onSearch = {
                focusManager.clearFocus()
                viewModel.searchByZipCode()
            },
            onClear = { viewModel.resetSearch() },
        )

        when {
            uiState.isLoading -> Box(Modifier.weight(1f)) { LoadingState() }
            uiState.errorMessage != null -> Box(Modifier.weight(1f)) {
                ErrorState(uiState.errorMessage!!) { viewModel.dismissError() }
            }
            uiState.courts.isEmpty() && uiState.hasSearched -> Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                NoResultsState(uiState.zipExamples) {
                    viewModel.onZipCodeChange(it)
                    viewModel.searchByZipCode()
                }
            }
            uiState.courts.isEmpty() -> Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                WelcomeHero(
                    state = uiState,
                    onUseCurrentLocation = { onUseCurrentLocation() },
                )
            }
            else -> ResultsList(
                modifier = Modifier.weight(1f),
                courts = uiState.courts,
                zipCode = uiState.zipCode,
            )
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
            border = if (isFocused) BorderStroke(1.5.dp, AppAccent.copy(alpha = 0.5f)) else null,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    null,
                    Modifier.size(18.dp),
                    tint = if (isFocused) AppAccent else Color.Gray,
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
                        cursorBrush = SolidColor(AppAccent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Cancel, "Clear", Modifier.size(18.dp), tint = Color.Gray)
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
            color = if (hasText) AppAccent else Color.Gray.copy(alpha = 0.3f),
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        CourtsHero()
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

        FeatureRow(painterResource(R.drawable.ic_sportscourt_fill), AppAccent, "Courts & Clubs", "${CurrentSport.sport.displayName} courts, clubs, and sports facilities")
        Spacer(Modifier.height(10.dp))
        FeatureRow(rememberVectorPainter(Icons.Filled.NearMe), Color(0xFF2196F3), "Distance Info", "See how far each court is from your area")
        Spacer(Modifier.height(10.dp))
        FeatureRow(rememberVectorPainter(Icons.Filled.Phone), Color(0xFFFF9800), "Quick Actions", "Get directions, call, or visit their website")
        Spacer(Modifier.height(10.dp))
        if (state.usesHomeRegion) {
            FeatureRow(rememberVectorPainter(Icons.Filled.Home), AppAccent, "Region Aligned", "Matches your Profile home country for open play and tournaments")
        } else {
            FeatureRow(rememberVectorPainter(Icons.Filled.Public), AppAccent, "Set Your Region", "Add home country and postal in Profile for regional discovery")
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            onClick = onUseCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = AppAccent,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.NearMe, null, Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Use Current Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.7f)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Lightbulb, null, Modifier.size(12.dp), tint = Color(0xFFFF9800))
                Text(
                    "Or enter a postal code (e.g. ${state.zipExamples.take(2).joinToString(", ")})",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun FeatureRow(icon: Painter, color: Color, title: String, subtitle: String) {
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
                        color = AppAccent.copy(alpha = alpha),
                    ) {}
                }
                Icon(sportIconPainter(CurrentSport.sport), null, Modifier.size(32.dp), tint = AppAccent)
            }
            Spacer(Modifier.height(24.dp))
            Text("Searching for courts…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Finding ${CurrentSport.sport.inlineName} facilities\nnear your location", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(Modifier.size(20.dp), color = AppAccent, strokeWidth = 2.dp)
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
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(180.dp, 130.dp).background(AppAccent.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(4) { Box(Modifier.fillMaxWidth().height(1.dp).background(AppAccent.copy(alpha = 0.06f))) }
                    }
                }
                Icon(sportIconPainter(CurrentSport.sport), null, Modifier.size(48.dp), tint = AppAccent.copy(alpha = 0.6f))
                Icon(
                    Icons.Filled.Warning, null,
                    Modifier.offset(x = 35.dp, y = (-30).dp).size(26.dp),
                    tint = Color(0xFFFFCC00),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Search Error", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(message, fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            Text("Try another ZIP code.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            Surface(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AppAccent,
            ) {
                Text(
                    "OK",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(110.dp).background(AppAccent.copy(alpha = 0.06f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_sportscourt), null, Modifier.size(44.dp), tint = AppAccent.copy(alpha = 0.3f))
            Icon(Icons.Filled.Search, null, Modifier.offset(x = 25.dp, y = (-25).dp).size(22.dp), tint = Color.Gray)
        }
        Spacer(Modifier.height(16.dp))
        Text("No Courts Found", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "We couldn't find ${CurrentSport.sport.inlineName} courts for that area.\nTry a different postal code.",
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
                    color = AppAccent.copy(alpha = 0.08f),
                ) {
                    Text(
                        zip,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Results
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ResultsList(
    modifier: Modifier = Modifier,
    courts: List<CourtResult>,
    zipCode: String,
) {
    val context = LocalContext.current
    val apiKey = remember { mapsApiKey(context) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = AppAccent.copy(alpha = 0.12f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(sportIconPainter(CurrentSport.sport), null, Modifier.size(12.dp), tint = AppAccent)
                    Text("${courts.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                }
            }
            Text("court${if (courts.size == 1) "" else "s"} near", fontSize = 13.sp, color = Color.Gray)
            Text(zipCode.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        courts.forEach { court ->
            CourtCard(court, apiKey)
        }

        Spacer(Modifier.height(80.dp))
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
                        color = Color.White.copy(alpha = 0.8f),
                    ) {
                        Text(
                            CourtSearchService.formatDistance(dist),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(court.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = AppAccent.copy(alpha = 0.1f)) {
                        Text(
                            CurrentSport.sport.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppAccent,
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
                court.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Phone, null, Modifier.size(12.dp), tint = CallBlue.copy(alpha = 0.6f))
                        Text(phone, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GroupedBg)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(rememberVectorPainter(Icons.Filled.Directions), "Directions", AppAccent) { openDirections(context, court) }
                    court.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        ActionPill(rememberVectorPainter(Icons.Filled.Phone), "Call", CallBlue) { callPhone(context, phone) }
                    }
                    ActionPill(painterResource(R.drawable.ic_safari_fill), "Website", AppAccent) { searchWebsite(context, court) }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(icon: Painter, label: String, tint: Color, onClick: () -> Unit) {
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

private fun callPhone(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    } catch (_: Exception) {
    }
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

/** Drawn hero (iOS CourtFinderView): accent rings, court motif, sport icon, floating pin. */
@Composable
private fun CourtsHero() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val p by animateFloatAsState(if (appeared) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 200f), label = "hero")
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(180.dp)
                .graphicsLayer { scaleX = 0.5f + 0.5f * p; scaleY = scaleX; alpha = p }
                .background(Brush.radialGradient(0.44f to AppAccent.copy(alpha = 0.12f), 1f to Color.Transparent), CircleShape),
        )
        Box(
            Modifier
                .size(130.dp)
                .graphicsLayer { scaleX = 0.7f + 0.3f * p; scaleY = scaleX; alpha = p }
                .background(AppAccent.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(70.dp, 50.dp).border(1.5.dp, AppAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp)))
            Box(Modifier.size(1.5.dp, 50.dp).background(AppAccent.copy(alpha = 0.1f)))
            Box(Modifier.size(70.dp, 1.5.dp).background(AppAccent.copy(alpha = 0.1f)))
        }
        Icon(
            sportIconPainter(CurrentSport.sport), null,
            Modifier.size(56.dp).graphicsLayer { scaleX = 0.3f + 0.7f * p; scaleY = scaleX; alpha = p },
            tint = AppAccent,
        )
        Icon(
            Icons.Filled.LocationOn, null,
            Modifier.offset(x = 55.dp, y = (-50).dp).size(26.dp).graphicsLayer { scaleX = p; scaleY = p; alpha = 0.8f * p },
            tint = AppAccent,
        )
        Icon(
            Icons.Filled.Apps, null,
            Modifier.offset(x = (-60).dp, y = 45.dp).size(12.dp).graphicsLayer { scaleX = p; scaleY = p; alpha = 0.6f * p },
            tint = AppAccent.copy(alpha = 0.5f),
        )
    }
}
