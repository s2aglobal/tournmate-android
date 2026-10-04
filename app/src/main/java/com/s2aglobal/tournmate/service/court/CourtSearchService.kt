package com.s2aglobal.tournmate.service.court

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import com.s2aglobal.tournmate.BuildConfig
import com.s2aglobal.tournmate.domain.model.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

data class CourtResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double? = null,
    val placeId: String? = null,
    val phone: String? = null,
)

class CourtSearchService(private val context: Context) {

    private val apiKey: String = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    } catch (_: Exception) { "" }

    private val json = Json { ignoreUnknownKeys = true }

    // Logs carry coordinates, ZIPs and request URLs: debug builds only, never the API key.
    private fun logD(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private fun logE(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(TAG, message, error)
    }

    private fun redactKey(url: URL): String = url.toString().replace(Regex("key=[^&]*"), "key=REDACTED")

    suspend fun searchCourts(query: String, sport: SportType, includePhone: Boolean = false): List<CourtResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val isLikelyZip = trimmed.length <= 10 && trimmed.all { it.isDigit() || it == '-' || it == ' ' }

        val results = if (isLikelyZip) {
            searchByZipCode(trimmed, sport.inlineName)
        } else {
            searchByName(trimmed)
        }
        if (includePhone) withPhoneNumbers(results) else results
    }

    /** Searches for courts for [sport] near an explicit coordinate (e.g. the user's current location). */
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        sport: SportType,
        radiusMeters: Int = 25_000,
        includePhone: Boolean = false,
    ): List<CourtResult> = withContext(Dispatchers.IO) {
        val queries = sportQueries(sport.inlineName)
        for (q in queries) {
            val results = nearbySearch(q, latitude, longitude, radiusMeters)
            if (results.isNotEmpty()) return@withContext if (includePhone) withPhoneNumbers(results) else results
        }
        emptyList()
    }

    /** Places search responses carry no phone, so fill it from Place Details (iOS gets it from MKMapItem). */
    private suspend fun withPhoneNumbers(results: List<CourtResult>): List<CourtResult> = coroutineScope {
        if (apiKey.isBlank()) return@coroutineScope results
        results.map { court ->
            async {
                val id = court.placeId ?: return@async court
                court.copy(phone = fetchPhone(id))
            }
        }.awaitAll()
    }

    private fun fetchPhone(placeId: String): String? {
        val encoded = URLEncoder.encode(placeId, "UTF-8")
        val url = URL(
            "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=$encoded&fields=formatted_phone_number,international_phone_number&key=$apiKey"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            if (conn.responseCode !in 200..299) return null
            val response = json.decodeFromString<PlaceDetailsResponse>(conn.inputStream.bufferedReader().readText())
            (response.result?.formatted_phone_number ?: response.result?.international_phone_number)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Reverse-geocodes a coordinate to a 2-letter ISO country code.
     * Used to keep "use current location" search aligned with the player's home country.
     */
    @Suppress("DEPRECATION")
    suspend fun reverseGeocodeCountryCode(
        latitude: Double,
        longitude: Double,
    ): String? = withContext(Dispatchers.IO) {
        // Try device Geocoder first
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.countryCode?.let { return@withContext it.uppercase() }
        } catch (_: Exception) {
        }

        // Fallback: Google Geocoding HTTP API
        if (apiKey.isBlank()) return@withContext null
        try {
            val geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&result_type=country&key=$apiKey"
            val conn = URL(geoUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream.bufferedReader().readText()
            conn.disconnect()
            val response = json.decodeFromString<ReverseGeocodingResponse>(responseText)
            response.results.firstOrNull()
                ?.address_components
                ?.firstOrNull { "country" in it.types }
                ?.short_name
                ?.uppercase()
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun sportQueries(sport: String) = listOf("$sport court", sport, "sports recreation center")

    private fun searchByZipCode(zipCode: String, sport: String): List<CourtResult> {
        // Step 1: Geocode zip to coordinates (like iOS CLGeocoder)
        var lat: Double? = null
        var lng: Double? = null

        // Try device Geocoder first
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(zipCode, 1)
            addresses?.firstOrNull()?.let { addr ->
                lat = addr.latitude
                lng = addr.longitude
                logD("Geocoder success: $lat, $lng")
            }
            if (lat == null) logD("Geocoder returned no results for '$zipCode'")
        } catch (e: Exception) {
            logE("Geocoder failed: ${e.message}")
        }

        // Fallback: use Google Geocoding HTTP API
        if (lat == null && apiKey.isNotBlank()) {
            try {
                val encoded = URLEncoder.encode(zipCode, "UTF-8")
                val geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&key=$apiKey"
                logD("Trying Geocoding API for '$zipCode'")
                val url = URL(geoUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream.bufferedReader().readText()
                conn.disconnect()
                logD("Geocoding response ($code): ${responseText.take(300)}")
                val response = json.decodeFromString<GeocodingResponse>(responseText)
                response.results.firstOrNull()?.let { result ->
                    lat = result.geometry.location.lat
                    lng = result.geometry.location.lng
                    logD("Geocoding API success: $lat, $lng")
                }
            } catch (e: Exception) {
                logE("Geocoding API failed: ${e.message}")
            }
        }

        val finalLat = lat ?: return textSearchFallback(zipCode, sport)
        val finalLng = lng ?: return textSearchFallback(zipCode, sport)

        // Step 2: Search nearby for sports venues (like iOS MKLocalSearch)
        logD("Searching nearby at $finalLat, $finalLng")
        val queries = sportQueries(sport)
        for (q in queries) {
            logD("Trying nearby search: '$q'")
            val results = nearbySearch(q, finalLat, finalLng, 25000)
            logD("Nearby '$q' returned ${results.size} results")
            if (results.isNotEmpty()) return results
        }

        // Step 3: Fallback — text search
        logD("No nearby results, trying text search fallback")
        return textSearchFallback(zipCode, sport)
    }

    private fun textSearchFallback(zipCode: String, sport: String): List<CourtResult> {
        if (apiKey.isBlank()) return emptyList()
        val results = textSearch("$sport court near $zipCode")
        if (results.isNotEmpty()) return results
        val fallback = textSearch("recreation center near $zipCode")
        if (fallback.isNotEmpty()) return fallback
        return emptyList()
    }

    private fun searchByName(name: String): List<CourtResult> {
        // Try text search first
        if (apiKey.isNotBlank()) {
            val results = textSearch(name)
            if (results.isNotEmpty()) return results
        }

        // Fallback to geocoder
        return geocoderSearch(name)
    }

    private fun nearbySearch(keyword: String, lat: Double, lng: Double, radiusMeters: Int): List<CourtResult> {
        if (apiKey.isBlank()) { logD("No API key"); return emptyList() }

        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val url = URL(
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
            "?location=$lat,$lng&radius=$radiusMeters&keyword=$encodedKeyword&key=$apiKey"
        )

        return try {
            executePlacesRequest(url, lat, lng)
        } catch (e: Exception) {
            logE("nearbySearch exception: ${e.message}", e)
            emptyList()
        }
    }

    private fun textSearch(query: String): List<CourtResult> {
        if (apiKey.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encoded&key=$apiKey")

        return executePlacesRequest(url)
    }

    private fun executePlacesRequest(url: URL, centerLat: Double? = null, centerLng: Double? = null): List<CourtResult> {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream.bufferedReader().readText()
            logD("URL: ${redactKey(url)}")
            logD("Response ($code): ${responseText.take(500)}")
            val response = json.decodeFromString<PlacesResponse>(responseText)

            if (response.status != "OK" && response.status != "ZERO_RESULTS") {
                logE("API error: status=${response.status}, msg=${response.error_message}")
                return emptyList()
            }

            response.results.map { place ->
                val distance = if (centerLat != null && centerLng != null) {
                    haversineDistance(centerLat, centerLng, place.geometry.location.lat, place.geometry.location.lng)
                } else null

                CourtResult(
                    name = place.name,
                    address = place.formatted_address ?: place.vicinity ?: "",
                    latitude = place.geometry.location.lat,
                    longitude = place.geometry.location.lng,
                    distanceMeters = distance,
                    placeId = place.place_id,
                )
            }.let { results ->
                if (centerLat != null) results.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
                else results
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun geocoderSearch(query: String): List<CourtResult> {
        val geocoder = Geocoder(context, Locale.getDefault())
        val queries = listOf(query, "$query recreation center")
        val results = mutableListOf<CourtResult>()
        val seen = mutableSetOf<String>()

        for (q in queries) {
            val addresses = try { geocoder.getFromLocationName(q, 5) ?: emptyList() } catch (_: Exception) { emptyList() }
            for (addr in addresses) {
                val name = addr.featureName ?: addr.locality ?: continue
                val key = "${(addr.latitude * 1000).roundToInt()}_${(addr.longitude * 1000).roundToInt()}"
                if (key in seen) continue
                seen.add(key)
                results.add(CourtResult(name = name, address = buildAddress(addr), latitude = addr.latitude, longitude = addr.longitude))
            }
            if (results.size >= 5) break
        }
        return results
    }

    private fun buildAddress(addr: android.location.Address): String {
        val parts = buildList {
            addr.thoroughfare?.let { t -> addr.subThoroughfare?.let { add("$it $t") } ?: add(t) }
            addr.locality?.let { if (it != addr.featureName) add(it) }
            addr.adminArea?.let { add(it) }
            addr.postalCode?.let { add(it) }
            addr.countryCode?.let { add(it) }
        }
        return if (parts.isEmpty()) "Address unavailable" else parts.joinToString(", ")
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    companion object {
        private const val TAG = "CourtSearch"
        fun formatDistance(meters: Double): String {
            val miles = meters / 1_609.344
            val km = meters / 1_000
            return when {
                meters < 160 -> "${meters.roundToInt()} m"
                miles < 100 -> "%.1f mi (%.1f km)".format(miles, km)
                else -> "%.0f mi".format(miles)
            }
        }
    }
}

@Serializable
private data class PlacesResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String = "",
    val error_message: String? = null,
)

@Serializable
private data class PlaceResult(
    val place_id: String? = null,
    val name: String = "",
    val formatted_address: String? = null,
    val vicinity: String? = null,
    val geometry: PlaceGeometry = PlaceGeometry(),
)

@Serializable
private data class PlaceGeometry(
    val location: PlaceLocation = PlaceLocation(),
)

@Serializable
private data class PlaceLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

@Serializable
private data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
    val status: String = "",
)

@Serializable
private data class GeocodingResult(
    val geometry: PlaceGeometry = PlaceGeometry(),
)

@Serializable
private data class ReverseGeocodingResponse(
    val results: List<ReverseGeocodingResult> = emptyList(),
    val status: String = "",
)

@Serializable
private data class ReverseGeocodingResult(
    val address_components: List<AddressComponent> = emptyList(),
)

@Serializable
private data class AddressComponent(
    val short_name: String = "",
    val long_name: String = "",
    val types: List<String> = emptyList(),
)

@Serializable
private data class PlaceDetailsResponse(
    val result: PlaceDetails? = null,
    val status: String = "",
)

@Serializable
private data class PlaceDetails(
    val formatted_phone_number: String? = null,
    val international_phone_number: String? = null,
)
