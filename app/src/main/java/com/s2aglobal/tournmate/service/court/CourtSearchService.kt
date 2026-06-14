package com.s2aglobal.tournmate.service.court

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

data class CourtResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double? = null,
)

class CourtSearchService(context: Context) {

    private val apiKey: String = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    } catch (_: Exception) { "" }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchCourts(query: String): List<CourtResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2 || apiKey.isBlank()) return@withContext emptyList()

        val isLikelyZip = trimmed.length <= 10 && trimmed.all { it.isDigit() || it == '-' || it == ' ' }

        val searchQuery = if (isLikelyZip) {
            "badminton court near $trimmed"
        } else {
            "$trimmed badminton"
        }

        val results = textSearch(searchQuery)
        if (results.isNotEmpty()) return@withContext results

        val fallbackQuery = if (isLikelyZip) "recreation center near $trimmed" else trimmed
        textSearch(fallbackQuery)
    }

    private fun textSearch(query: String): List<CourtResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encoded&key=$apiKey")

        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        return try {
            val responseText = conn.inputStream.bufferedReader().readText()
            val response = json.decodeFromString<PlacesTextSearchResponse>(responseText)

            response.results.map { place ->
                CourtResult(
                    name = place.name,
                    address = place.formatted_address ?: "",
                    latitude = place.geometry.location.lat,
                    longitude = place.geometry.location.lng,
                )
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
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
private data class PlacesTextSearchResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String = "",
)

@Serializable
private data class PlaceResult(
    val name: String = "",
    val formatted_address: String? = null,
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
