package com.s2aglobal.tournmate.service.config

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.domain.model.SportType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** One picker section ("Racket & Paddle"). [sports] holds only ids this build knows. */
data class SportCategory(val id: String, val title: String, val sports: List<SportType>)

/**
 * The resolved sport catalog (Firestore `config/sports`, schemaVersion 1).
 * Unknown ids are already dropped, and so are categories left empty.
 */
data class SportCatalogState(
    val categories: List<SportCategory>,
    /** Live sports, in `live` order. Known ids only; never [SportType.GENERIC] or [SportType.FOOTBALL]. */
    val liveSports: List<SportType>,
) {
    /** Every catalog sport, in section order (drives the "Search N sports" count). */
    val allSports: List<SportType> = categories.flatMap { it.sports }

    /** Selectable: in `live` and known to this build (iOS `isLive`). */
    fun isLive(sport: SportType): Boolean = sport in liveSports

    /** Create flows only accept live sports: [preferred] when live, else the first live sport (iOS parity). */
    fun defaultCreationSport(preferred: SportType): SportType =
        if (isLive(preferred)) preferred else liveSports.firstOrNull() ?: SportType.BADMINTON

    companion object {
        /** Bundled default, identical to the launch `config/sports`. */
        val DEFAULT: SportCatalogState = SportCatalogParser.parse(SportCatalogParser.DEFAULT_RAW)!!
    }
}

/**
 * Strings-only copy of a `config/sports` document, before dropping unknown ids, so a cached copy
 * still works after an app update that learns more sports. Bad category entries are already skipped.
 */
internal data class RawSportCatalog(
    val schemaVersion: Int?,
    val live: List<String>,
    val categories: List<RawCategory>,
) {
    data class RawCategory(val id: String, val title: String?, val sports: List<String>)

    fun toJson(): String = JSONObject().apply {
        schemaVersion?.let { put("schemaVersion", it) }
        put("live", JSONArray(live))
        put("categories", JSONArray(categories.map { c ->
            JSONObject().put("id", c.id).apply { c.title?.let { put("title", it) } }.put("sports", JSONArray(c.sports))
        }))
    }.toString()
}

internal object SportCatalogParser {

    val DEFAULT_RAW = RawSportCatalog(
        schemaVersion = 1,
        live = listOf("pickleball", "badminton", "tennis"),
        categories = listOf(
            RawSportCatalog.RawCategory(
                "racket_paddle", "Racket & Paddle",
                listOf("pickleball", "badminton", "tennis", "padel", "table_tennis", "squash"),
            ),
            RawSportCatalog.RawCategory(
                "court_field", "Court & Field",
                listOf("volleyball", "beach_volleyball", "basketball", "soccer", "cricket", "roundnet"),
            ),
            RawSportCatalog.RawCategory("target_more", "Target & More", listOf("golf", "disc_golf", "bowling", "darts")),
        ),
    )

    /** Fallback titles if a category omits `title`. */
    private val KNOWN_CATEGORY_TITLES = mapOf(
        "racket_paddle" to "Racket & Paddle",
        "court_field" to "Court & Field",
        "target_more" to "Target & More",
    )

    /**
     * Reads document data (Firestore map or decoded cache JSON). Null when malformed: a bad
     * `schemaVersion`, or `live` / `categories` of the wrong type. Bad category entries are skipped.
     */
    fun rawFromMap(data: Map<String, Any?>?): RawSportCatalog? {
        data ?: return null
        val version = data["schemaVersion"]
        if (version != null && ((version as? Number)?.toInt() ?: return null) < 1) return null
        val live = (data["live"] as? List<*>)?.filterIsInstance<String>() ?: return null
        val categories = (data["categories"] as? List<*>)?.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val id = (map["id"] as? String)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val sports = (map["sports"] as? List<*>)?.filterIsInstance<String>() ?: return@mapNotNull null
            RawSportCatalog.RawCategory(id, (map["title"] as? String)?.takeIf { it.isNotBlank() }, sports)
        } ?: return null
        return RawSportCatalog((version as? Number)?.toInt(), live, categories)
    }

    fun rawFromJson(json: String?): RawSportCatalog? = runCatching {
        @Suppress("UNCHECKED_CAST")
        rawFromMap(jsonToKotlin(JSONObject(json ?: return null)) as Map<String, Any?>)
    }.getOrNull()

    private fun jsonToKotlin(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { jsonToKotlin(value.opt(it)) }
        is JSONArray -> (0 until value.length()).map { jsonToKotlin(value.opt(it)) }
        JSONObject.NULL -> null
        else -> value
    }

    /** A catalog id this build can show: known, and not a legacy id (`generic`, `football`). */
    private fun catalogSport(id: String): SportType? =
        SportType.fromKnownRawValue(id)?.takeIf { it != SportType.GENERIC && it != SportType.FOOTBALL }

    /**
     * Applies the app rules (iOS `SportCatalogConfig.parse`): unknown/legacy ids and duplicates
     * skipped, empty categories dropped, missing titles filled from the known ids.
     * Null when no category is left (treated as malformed).
     */
    fun parse(raw: RawSportCatalog): SportCatalogState? {
        val seen = mutableSetOf<SportType>()
        val categories = raw.categories.mapNotNull { c ->
            val sports = c.sports.mapNotNull(::catalogSport).filter(seen::add)
            if (sports.isEmpty()) return@mapNotNull null
            SportCategory(c.id, c.title ?: KNOWN_CATEGORY_TITLES[c.id] ?: c.id, sports)
        }
        if (categories.isEmpty()) return null
        val live = raw.live.mapNotNull(::catalogSport).distinct()
        return SportCatalogState(categories, live)
    }
}

/**
 * Server-driven sport catalog backed by Firestore `config/sports` (shared with iOS and the server).
 *
 * Starts on the bundled default, swaps to the last cached config as soon as it's read from disk,
 * then refreshes from Firestore once per launch ([refresh]). A missing or malformed doc falls back
 * to the bundled default; an unreachable or slow (> 5s) fetch keeps the cached config.
 */
@Singleton
class SportCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: FirebaseFirestore,
) {
    private val _state = MutableStateFlow(SportCatalogState.DEFAULT)
    val state: StateFlow<SportCatalogState> = _state.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private var refreshed = false

    /** Loads the cache, then fetches `config/sports`. Network work runs once per process. */
    suspend fun refresh() {
        loadCache()
        if (refreshed) return
        refreshed = true
        val result = fetch() ?: return // unreachable: keep cache/default
        when (result) {
            FetchResult.Missing -> {
                _state.value = SportCatalogState.DEFAULT
                withContext(Dispatchers.IO) { prefs.edit().remove(KEY_CONFIG).apply() }
            }
            FetchResult.Malformed -> {
                // Spec: a malformed doc falls back to the bundled default. The last good
                // config stays cached in case the doc is fixed and later unreachable.
                _state.value = SportCatalogState.DEFAULT
            }
            is FetchResult.Found -> {
                val parsed = SportCatalogParser.parse(result.raw)
                if (parsed == null) {
                    _state.value = SportCatalogState.DEFAULT
                    return
                }
                _state.value = parsed
                withContext(Dispatchers.IO) { prefs.edit().putString(KEY_CONFIG, result.raw.toJson()).apply() }
            }
        }
    }

    private var cacheLoaded = false

    private suspend fun loadCache() {
        if (cacheLoaded) return
        cacheLoaded = true
        val json = withContext(Dispatchers.IO) { prefs.getString(KEY_CONFIG, null) }
        val cached = SportCatalogParser.rawFromJson(json)?.let(SportCatalogParser::parse) ?: return
        _state.value = cached
    }

    private sealed interface FetchResult {
        data object Missing : FetchResult
        data object Malformed : FetchResult
        data class Found(val raw: RawSportCatalog) : FetchResult
    }

    private suspend fun fetch(): FetchResult? = try {
        withTimeoutOrNull(TIMEOUT_MS) {
            val doc = db.collection("config").document("sports").get().await()
            if (!doc.exists()) {
                // Only trust "missing" from the server; Firestore's local cache may just be empty.
                return@withTimeoutOrNull if (doc.metadata.isFromCache) null else FetchResult.Missing
            }
            SportCatalogParser.rawFromMap(doc.data)?.let { FetchResult.Found(it) } ?: FetchResult.Malformed
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
        private const val PREFS_NAME = "sport_catalog"
        /** Same name as the iOS UserDefaults key. */
        private const val KEY_CONFIG = "sportCatalogConfigCache"
    }
}
