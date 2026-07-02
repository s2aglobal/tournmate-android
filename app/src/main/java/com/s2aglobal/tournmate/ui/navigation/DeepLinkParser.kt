package com.s2aglobal.tournmate.ui.navigation

import android.net.Uri

/**
 * Parses incoming deep link URIs into a navigation target.
 *
 * Supported formats (matching iOS + AndroidManifest intent filters):
 *   tournmate://tournament/{id}
 *   tournmate://session/{id}
 *   https://www.tournmate.com/tournament/{id}
 *   https://www.tournmate.com/session/{id}
 */
object DeepLinkParser {

    enum class Type { TOURNAMENT, SESSION }

    data class Target(val type: Type, val id: String)

    fun parse(uri: Uri?): Target? {
        uri ?: return null

        val (typeStr, id) = when (uri.scheme) {
            "tournmate" -> {
                val type = uri.host ?: return null
                val id = uri.pathSegments.firstOrNull() ?: return null
                type to id
            }
            "http", "https" -> {
                val segs = uri.pathSegments
                if (segs.size < 2) return null
                segs[0] to segs[1]
            }
            else -> return null
        }

        val type = when (typeStr.lowercase()) {
            "tournament" -> Type.TOURNAMENT
            "session" -> Type.SESSION
            else -> return null
        }

        if (id.isBlank()) return null
        return Target(type, id.uppercase())
    }
}
