package com.s2aglobal.tournmate.util

import android.content.Context
import android.content.Intent
import com.s2aglobal.tournmate.domain.model.PlaySession
import com.s2aglobal.tournmate.domain.model.Tournament
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Centralised share helper — mirrors iOS ShareService.swift.
 *
 * Generates deep-link URLs and human-readable share text for tournaments
 * and open-play sessions, then launches the system share sheet.
 *
 * Usage:
 *   ShareUtil.shareTournament(context, tournament)
 *   ShareUtil.shareSession(context, session)
 */
object ShareUtil {

    private const val BASE_URL = "https://www.tournmate.com"

    fun tournamentUrl(tournamentId: String): String =
        "$BASE_URL/tournament/${tournamentId.uppercase()}"

    fun sessionUrl(sessionId: String): String =
        "$BASE_URL/session/${sessionId.uppercase()}"

    fun shareTournament(context: Context, tournament: Tournament) {
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val url = tournamentUrl(tournament.id.toString())
        val text = buildString {
            append("🏸 ${tournament.title}\n")
            append("📅 ${dateFmt.format(tournament.date)} • ${tournament.format.displayName}\n")
            if (tournament.location.isNotEmpty()) append("📍 ${tournament.location}\n")
            append("\n$url")
        }
        launchShareSheet(context, text, "Share Tournament")
    }

    fun shareSession(context: Context, session: PlaySession) {
        val dateFmt = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        val url = sessionUrl(session.id.toString())
        val text = buildString {
            append("🏸 Open Play: ${session.title}\n")
            append("📅 ${dateFmt.format(session.date)}")
            if (session.venue.isNotEmpty()) append("\n📍 ${session.venue}")
            append("\n\n$url")
        }
        launchShareSheet(context, text, "Share Session")
    }

    private fun launchShareSheet(context: Context, text: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
