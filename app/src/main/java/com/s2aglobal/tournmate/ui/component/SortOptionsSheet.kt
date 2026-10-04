package com.s2aglobal.tournmate.ui.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.s2aglobal.tournmate.domain.model.PlaySession
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.ui.theme.AppAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

// ── Option ───────────────────────────────────────────────────────────────

/** Sort options shared by the Tournaments and Open Play lists (mirrors iOS ListSortOption). */
enum class ListSortOption(val storedValue: String, val label: String) {
    SOONEST("soonest", "Soonest first"),
    LATEST("latest", "Latest first"),
    NEAREST("nearest", "Nearest to me"),
    PRICE("price", "Lowest price"),
    FILLING("filling", "Filling fast");

    companion object {
        val DEFAULT = SOONEST

        /** Unknown or missing stored values fall back to Soonest first. */
        fun fromStored(value: String?): ListSortOption =
            entries.firstOrNull { it.storedValue == value } ?: DEFAULT
    }
}

// ── Persistence (per device, per list) ───────────────────────────────────

object ListSortStore {
    const val KEY_TOURNAMENTS = "sortOption.tournaments"
    const val KEY_OPEN_PLAY = "sortOption.openPlay"
    private const val PREFS = "list_sort_options"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context, key: String): ListSortOption =
        ListSortOption.fromStored(prefs(context).getString(key, null))

    fun save(context: Context, key: String, option: ListSortOption) {
        prefs(context).edit().putString(key, option.storedValue).apply()
    }
}

/** Persisted sort option for one list; writes through to SharedPreferences on change. */
@Composable
fun rememberListSortOption(key: String): Pair<ListSortOption, (ListSortOption) -> Unit> {
    val context = LocalContext.current
    var option by remember(key) { mutableStateOf(ListSortStore.load(context, key)) }
    return option to { new ->
        option = new
        ListSortStore.save(context, key, new)
    }
}

// ── Comparators ──────────────────────────────────────────────────────────

/** One coarse location fix as (latitude, longitude). */
data class SortLocation(val latitude: Double, val longitude: Double)

private class SortKeys<T>(
    val date: (T) -> Date,
    val title: (T) -> String,
    val latitude: (T) -> Double?,
    val longitude: (T) -> Double?,
    val price: (T) -> Double,
    val filling: (T) -> Double,
    /** Filling fast sorts lower tiers first (tournaments: 0 = has a max, 1 = no max). */
    val fillingTier: (T) -> Int = { 0 },
)

private fun distanceMeters(from: SortLocation, lat: Double?, lng: Double?): Float? {
    if (lat == null || lng == null) return null
    val out = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, lat, lng, out)
    return out[0]
}

private fun <T> sortWith(
    items: List<T>,
    option: ListSortOption,
    userLocation: SortLocation?,
    keys: SortKeys<T>,
    isPast: Boolean,
): List<T> {
    // Stable tie-break for every option: soonest date, then title.
    val tieBreak = compareBy<T> { keys.date(it) }.thenBy(String.CASE_INSENSITIVE_ORDER) { keys.title(it) }
    // Past / Completed sections ignore the chosen sort and always show most recent first.
    val effective = when {
        isPast -> ListSortOption.LATEST
        option == ListSortOption.NEAREST && userLocation == null -> ListSortOption.SOONEST
        else -> option
    }
    val comparator: Comparator<T> = when (effective) {
        ListSortOption.SOONEST -> tieBreak
        ListSortOption.LATEST -> compareByDescending<T> { keys.date(it) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { keys.title(it) }
        ListSortOption.NEAREST -> {
            val loc = userLocation!!
            // Items without coordinates sort last.
            compareBy<T, Float?>(nullsLast()) { distanceMeters(loc, keys.latitude(it), keys.longitude(it)) }
                .then(tieBreak)
        }
        ListSortOption.PRICE -> compareBy<T> { keys.price(it) }.then(tieBreak)
        ListSortOption.FILLING -> compareBy<T> { keys.fillingTier(it) }
            .thenByDescending { keys.filling(it) }
            .then(tieBreak)
    }
    return items.sortedWith(comparator)
}

private val tournamentKeys = SortKeys<Tournament>(
    date = { it.date },
    title = { it.title },
    latitude = { it.locationLatitude },
    longitude = { it.locationLongitude },
    price = { (it.entryFee ?: 0.0).coerceAtLeast(0.0) },
    filling = { t ->
        val count = (t.registrationCount ?: 0).toDouble()
        val max = t.formatConfig.maxParticipants ?: 0
        if (max > 0) count / max else count
    },
    fillingTier = { t -> if ((t.formatConfig.maxParticipants ?: 0) > 0) 0 else 1 },
)

private val sessionKeys = SortKeys<PlaySession>(
    date = { it.date },
    title = { it.title },
    latitude = { it.venueLatitude },
    longitude = { it.venueLongitude },
    price = { (it.costPerPerson ?: 0.0).coerceAtLeast(0.0) },
    filling = { it.attendeeCount.toDouble() },
)

@JvmName("sortedTournamentsForList")
fun List<Tournament>.sortedForList(option: ListSortOption, userLocation: SortLocation?, isPast: Boolean = false): List<Tournament> =
    sortWith(this, option, userLocation, tournamentKeys, isPast)

@JvmName("sortedSessionsForList")
fun List<PlaySession>.sortedForList(option: ListSortOption, userLocation: SortLocation?, isPast: Boolean = false): List<PlaySession> =
    sortWith(this, option, userLocation, sessionKeys, isPast)

// ── Location for "Nearest to me" ─────────────────────────────────────────

/** One coarse fix per app session; no continuous updates. */
private object SortLocationCache {
    var location: SortLocation? = null
}

class SortLocationState internal constructor(
    val userLocation: SortLocation?,
    /** Permission denied or no fix within ~5s: show the "Turn on location" note. */
    val unavailable: Boolean,
    /** Call when the user picks an option; asks for permission if Nearest needs it. */
    val onOptionChosen: (ListSortOption) -> Unit,
)

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private suspend fun fetchCoarseFix(context: Context): SortLocation? = runCatching {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val last = client.lastLocation.await()
    val fix = last ?: run {
        val cts = CancellationTokenSource()
        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
        } finally {
            cts.cancel()
        }
    }
    fix?.let { SortLocation(it.latitude, it.longitude) }
}.getOrNull()

/** Same approach as Courts: FusedLocationProviderClient with a when-in-use runtime permission. */
@Composable
fun rememberSortLocationState(option: ListSortOption): SortLocationState {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf(SortLocationCache.location) }
    var unavailable by remember { mutableStateOf(false) }
    var awaitingPermission by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        awaitingPermission = false
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) attempt++ else unavailable = true
    }

    // Back from Settings (or any foreground return) while the note shows: retry once.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (option == ListSortOption.NEAREST && unavailable && !awaitingPermission && SortLocationCache.location == null) {
            attempt++
        }
    }

    LaunchedEffect(option, attempt, awaitingPermission) {
        if (option != ListSortOption.NEAREST || awaitingPermission) return@LaunchedEffect
        SortLocationCache.location?.let {
            userLocation = it
            unavailable = false
            return@LaunchedEffect
        }
        if (!hasLocationPermission(context)) {
            unavailable = true
            return@LaunchedEffect
        }
        unavailable = false
        val fix = withTimeoutOrNull(5_000) { fetchCoarseFix(context) }
        if (fix != null) {
            SortLocationCache.location = fix
            userLocation = fix
        } else {
            unavailable = true
        }
    }

    return SortLocationState(
        userLocation = if (option == ListSortOption.NEAREST) userLocation else null,
        unavailable = option == ListSortOption.NEAREST && unavailable && userLocation == null,
        onOptionChosen = { chosen ->
            if (chosen == ListSortOption.NEAREST && SortLocationCache.location == null && !hasLocationPermission(context)) {
                awaitingPermission = true
                launcher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        },
    )
}

/** One-line fallback note shown at the top of the list when distance sorting can't run. */
@Composable
fun LocationSortNote(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        text = "Turn on location to sort by distance",
        fontSize = 13.sp,
        color = SortSheetColors.Secondary,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            .padding(vertical = 4.dp),
    )
}

// ── Filter icon ──────────────────────────────────────────────────────────

/**
 * Today's sort icon in a 32x32 tap target: secondary grey for Soonest first; accent colour
 * plus a 6dp accent dot (offset +4, -3 from the icon's top-right) for any other option.
 */
@Composable
fun SortFilterIcon(option: ListSortOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val active = option != ListSortOption.DEFAULT
    Box(
        modifier = modifier
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Icon(
                Icons.Default.FilterList,
                contentDescription = if (active) "Sort, ${option.label}" else "Sort",
                tint = if (active) AppAccent else SortSheetColors.Secondary,
                modifier = Modifier.size(20.dp),
            )
            if (active) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .size(6.dp)
                        .background(AppAccent, CircleShape),
                )
            }
        }
    }
}

// ── Sheet ────────────────────────────────────────────────────────────────

private object SortSheetColors {
    val Background = Color(0xFFF2F2F7)
    val Secondary = Color(0xFF8E8E93)
    val Divider = Color(0xFFE5E5EA)
    val Selected = Color(0xFF0F172A)
    val UnselectedStroke = Color(0xFFC7C7CC)
    val Grabber = Color(0x4D3C3C43)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsSheet(
    selected: ListSortOption,
    onSelect: (ListSortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun close(afterMillis: Long) {
        if (closing) return
        closing = true
        scope.launch {
            if (afterMillis > 0) delay(afterMillis)
            sheetState.hide()
        }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SortSheetColors.Background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // iOS-style grabber inside the 28dp top padding (5dp from the edge, 36x5).
        dragHandle = {
            Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .background(SortSheetColors.Grabber, CircleShape),
                )
            }
        },
    ) {
        // Content fits without scrolling, so no sheetScrollLikeIos().
        // Padding: 16 sides, 28 top (the grabber slot), 16 bottom; header + label inset to 20.
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort & filter", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.weight(1f))
                val resetEnabled = selected != ListSortOption.DEFAULT
                Text(
                    "Reset",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Dark when there's something to reset, grey when already on the default (iOS).
                    color = if (resetEnabled) SortSheetColors.Selected else SortSheetColors.Secondary,
                    modifier = Modifier
                        .clickable(enabled = resetEnabled, role = Role.Button) {
                            onSelect(ListSortOption.DEFAULT)
                        }
                        .padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "SORT BY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = SortSheetColors.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .selectableGroup(),
            ) {
                ListSortOption.entries.forEachIndexed { index, option ->
                    val isSelected = option == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                if (isSelected) {
                                    close(0)
                                } else {
                                    onSelect(option)
                                    close(200)
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            option.label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = Color.Black,
                            modifier = Modifier.weight(1f),
                        )
                        SelectionIndicator(isSelected)
                    }
                    if (index < ListSortOption.entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = SortSheetColors.Divider,
                        )
                    }
                }
            }
            // ModalBottomSheet already insets for the navigation bar; 16dp below the card.
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    if (selected) {
        Box(
            Modifier.size(22.dp).background(SortSheetColors.Selected, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Bold 11dp checkmark (iOS: checkmark, 11pt bold, white).
            Canvas(Modifier.size(11.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.08f, h * 0.55f)
                    lineTo(w * 0.38f, h * 0.84f)
                    lineTo(w * 0.94f, h * 0.18f)
                }
                drawPath(
                    path,
                    Color.White,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    } else {
        Box(
            Modifier.size(22.dp).border(1.5.dp, SortSheetColors.UnselectedStroke, CircleShape),
        )
    }
}
