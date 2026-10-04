package com.s2aglobal.tournmate.service.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists received push notifications locally so they appear in the
 * notification inbox even without a Firestore document.
 *
 * Broadcast topic notifications (session/tournament created) don't
 * write server-side inbox docs for receivers — this fills that gap.
 * Mirrors iOS LocalNotificationStore.swift.
 * Uses built-in org.json — no extra dependency needed.
 */
data class LocalNotificationEntry(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val tournamentId: String?,
    val sessionId: String?,
    val createdBy: String?,
    val createdAt: Long,       // epoch millis
    var read: Boolean = false,
)

@Singleton
class LocalNotificationStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("local_notifications", Context.MODE_PRIVATE)
    }
    private val storageKey = "entries"
    private val maxStored = 50

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save(
        type: String,
        title: String,
        body: String,
        tournamentId: String? = null,
        sessionId: String? = null,
        createdBy: String? = null,
    ) {
        val entry = LocalNotificationEntry(
            id           = UUID.randomUUID().toString(),
            type         = type,
            title        = title,
            body         = body,
            tournamentId = tournamentId,
            sessionId    = sessionId,
            createdBy    = createdBy,
            createdAt    = Date().time,
            read         = false,
        )
        val existing = loadAll().toMutableList()
        existing.add(0, entry)
        persist(existing.take(maxStored))
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadAll(): List<LocalNotificationEntry> {
        val json = prefs.getString(storageKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LocalNotificationEntry(
                    id           = o.getString("id"),
                    type         = o.optString("type", ""),
                    title        = o.getString("title"),
                    body         = o.optString("body", ""),
                    tournamentId = o.optString("tournamentId").takeIf { it.isNotEmpty() },
                    sessionId    = o.optString("sessionId").takeIf { it.isNotEmpty() },
                    createdBy    = o.optString("createdBy").takeIf { it.isNotEmpty() },
                    createdAt    = o.getLong("createdAt"),
                    read         = o.optBoolean("read", false),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Mark read ─────────────────────────────────────────────────────────────

    fun markAsRead(id: String) {
        persist(loadAll().map { if (it.id == id) it.copy(read = true) else it })
    }

    fun markAllAsRead() {
        persist(loadAll().map { it.copy(read = true) })
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun delete(id: String) {
        persist(loadAll().filter { it.id != id })
    }

    /** Removes every stored entry (sign-out / account deletion). */
    fun clear() {
        prefs.edit().remove(storageKey).apply()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun persist(entries: List<LocalNotificationEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id",           e.id)
                put("type",         e.type)
                put("title",        e.title)
                put("body",         e.body)
                put("tournamentId", e.tournamentId ?: "")
                put("sessionId",    e.sessionId ?: "")
                put("createdBy",    e.createdBy ?: "")
                put("createdAt",    e.createdAt)
                put("read",         e.read)
            })
        }
        prefs.edit().putString(storageKey, arr.toString()).apply()
    }
}
