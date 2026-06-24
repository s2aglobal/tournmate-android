package com.s2aglobal.tournmate.service.court

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
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
)

class CourtSearchService(private val context: Context) {

    private val apiKey: String = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    } catch (_: Exception) { "" }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchCourts(query: String): List<CourtResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val isLikelyZip = trimmed.length <= 10 && trimmed.all { it.isDigit() || it == '-' || it == ' ' }

        if (isLikelyZip) {
            searchByZipCode(trimmed)
        } else {
            searchByName(trimmed)
        }
    }

    @Suppress("DEPRECATION")
    private fun searchByZipCode(zipCode: String): List<CourtResult> {
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
                android.util.Log.d("CourtSearch", "Geocoder success: $lat, $lng")
            }
            if (lat == null) android.util.Log.d("CourtSearch", "Geocoder returned no results for '$zipCode'")
        } catch (e: Exception) {
            android.util.Log.e("CourtSearch", "Geocoder failed: ${e.message}")
        }

        // Fallback: use Google Geocoding HTTP API
        if (lat == null && apiKey.isNotBlank()) {
            try {
                val encoded = URLEncoder.encode(zipCode, "UTF-8")
                val geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&key=$apiKey"
                android.util.Log.d("CourtSearch", "Trying Geocoding API: $geoUrl")
                val url = URL(geoUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream.bufferedReader().readText()
                conn.disconnect()
                android.util.Log.d("CourtSearch", "Geocoding response ($code): ${responseText.take(300)}")
                val response = json.decodeFromString<GeocodingResponse>(responseText)
                response.results.firstOrNull()?.let { result ->
                    lat = result.geometry.location.lat
                    lng = result.geometry.location.lng
                    android.util.Log.d("CourtSearch", "Geocoding API success: $lat, $lng")
                }
            } catch (e: Exception) {
                android.util.Log.e("CourtSearch", "Geocoding API failed: ${e.message}")
            }
        }

        val finalLat = lat ?: return textSearchFallback(zipCode)
        val finalLng = lng ?: return textSearchFallback(zipCode)

        // Step 2: Search nearby for sports venues (like iOS MKLocalSearch)
        android.util.Log.d("CourtSearch", "Searching nearby at $finalLat, $finalLng with apiKey=${apiKey.take(10)}...")
        val queries = listOf("badminton court", "badminton", "sports recreation center")
        for (q in queries) {
            android.util.Log.d("CourtSearch", "Trying nearby search: '$q'")
            val results = nearbySearch(q, finalLat, finalLng, 25000)
            android.util.Log.d("CourtSearch", "Nearby '$q' returned ${results.size} results")
            if (results.isNotEmpty()) return results
        }

        // Step 3: Fallback — text search
        android.util.Log.d("CourtSearch", "No nearby results, trying text search fallback")
        return textSearchFallback(zipCode)
    }

    private fun textSearchFallback(zipCode: String): List<CourtResult> {
        if (apiKey.isBlank()) return emptyList()
        val results = textSearch("badminton court near $zipCode")
        if (results.isNotEmpty()) return results
        val fallback = textSearch("recreation center near $zipCode")
        if (fallback.isNotEmpty()) return fallback
        return emptyList()
    }

    private fun searchByName(name: String): List<CourtResult> {
        // Try text search first
        if (apiKey.isNotBlank()) {
            val results = textSearch("$name badminton")
            if (results.isNotEmpty()) return results
            val fallback = textSearch(name)
            if (fallback.isNotEmpty()) return fallback
        }

        // Fallback to geocoder
        return geocoderSearch(name)
    }

    private fun nearbySearch(keyword: String, lat: Double, lng: Double, radiusMeters: Int): List<CourtResult> {
        if (apiKey.isBlank()) { android.util.Log.d("CourtSearch", "No API key"); return emptyList() }

        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val url = URL(
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
            "?location=$lat,$lng&radius=$radiusMeters&keyword=$encodedKeyword&key=$apiKey"
        )

        return try {
            executePlacesRequest(url, lat, lng)
        } catch (e: Exception) {
            android.util.Log.e("CourtSearch", "nearbySearch exception: ${e.message}", e)
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
            android.util.Log.d("CourtSearch", "URL: $url")
            android.util.Log.d("CourtSearch", "Response ($code): ${responseText.take(500)}")
            val response = json.decodeFromString<PlacesResponse>(responseText)

            if (response.status != "OK" && response.status != "ZERO_RESULTS") {
                android.util.Log.e("CourtSearch", "API error: status=${response.status}, msg=${response.error_message}")
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
        val queries = listOf("$query badminton", "$query recreation center", query)
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
