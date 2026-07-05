package com.s2aglobal.tournmate.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.service.notification.LocalNotificationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

// ── Domain model ──────────────────────────────────────────────────────────────

data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val tournamentId: String?,
    val sessionId: String?,
    var read: Boolean,
    val createdAt: Date,
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class NotificationInboxUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localStore: LocalNotificationStore,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationInboxUiState())
    val uiState: StateFlow<NotificationInboxUiState> = _uiState.asStateFlow()

    val unreadCount: Int get() = _uiState.value.notifications.count { !it.read }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.notifications.isEmpty())

            val firebaseUid = resolveFirebaseUid()
            if (firebaseUid == null) {
                _uiState.value = NotificationInboxUiState(
                    notifications = mergeNotifications(emptyList()),
                    isLoading = false,
                )
                return@launch
            }

            val firestoreItems = loadFirestoreNotifications(firebaseUid)

            _uiState.value = NotificationInboxUiState(
                notifications = mergeNotifications(firestoreItems),
                isLoading = false,
            )
        }
    }

    /** Prefer live Firebase Auth UID — must match Firestore security rules. */
    private suspend fun resolveFirebaseUid(): String? {
        repeat(8) { attempt ->
            auth.currentUser?.uid?.let { return it }
            if (attempt == 0) {
                currentUserStore.firebaseUid()?.let { return it }
            }
            delay(150)
        }
        return currentUserStore.firebaseUid()
    }

    private fun mergeNotifications(firestoreItems: List<NotificationItem>): List<NotificationItem> {
        val combined = firestoreItems.toMutableList()
        val firestoreSessionIds = combined.mapNotNull { it.sessionId?.uppercase() }.toSet()
        val firestoreTournamentIds = combined.mapNotNull { it.tournamentId?.uppercase() }.toSet()
        val firestoreIds = combined.map { it.id }.toSet()

        for (entry in localStore.loadAll()) {
            val localId = "local_${entry.id}"
            if (localId in firestoreIds) continue

            val sessionId = entry.sessionId?.uppercase()
            val tournamentId = entry.tournamentId?.uppercase()
            val isDuplicate = when {
                sessionId != null -> sessionId in firestoreSessionIds
                tournamentId != null -> tournamentId in firestoreTournamentIds
                else -> false
            }
            if (isDuplicate) continue

            combined += NotificationItem(
                id = localId,
                type = entry.type,
                title = entry.title,
                body = entry.body,
                tournamentId = entry.tournamentId,
                sessionId = entry.sessionId,
                read = entry.read,
                createdAt = Date(entry.createdAt),
            )
        }

        return combined.sortedByDescending { it.createdAt }
    }

    private suspend fun loadFirestoreNotifications(firebaseUid: String): List<NotificationItem> {
        // Try indexed query first (server source to avoid stale cache).
        try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("recipientId", firebaseUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get(Source.SERVER)
                .await()
            return parseSnapshot(snapshot)
        } catch (_: Exception) {
            // Fall through to simpler queries.
        }

        // Fallback without orderBy (missing composite index).
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("recipientId", firebaseUid)
                .limit(50)
                .get(Source.SERVER)
                .await()
            parseSnapshot(snapshot).sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot): List<NotificationItem> =
        snapshot.documents.mapNotNull { doc -> parseNotificationDoc(doc.id, doc.data) }

    private fun parseNotificationDoc(
        docId: String,
        data: Map<String, Any?>?,
    ): NotificationItem? {
        if (data == null) return null
        val title = data["title"] as? String ?: return null
        val body = data["body"] as? String ?: ""
        val ts = when (val createdAt = data["createdAt"]) {
            is com.google.firebase.Timestamp -> createdAt.toDate()
            else -> Date()
        }
        return NotificationItem(
            id = docId,
            type = data["type"] as? String ?: "",
            title = title,
            body = body,
            tournamentId = data["tournamentId"] as? String,
            sessionId = data["sessionId"] as? String,
            read = data["read"] as? Boolean ?: false,
            createdAt = ts,
        )
    }

    fun markAsRead(item: NotificationItem) {
        viewModelScope.launch {
            val updated = _uiState.value.notifications.map {
                if (it.id == item.id) it.copy(read = true) else it
            }
            _uiState.value = _uiState.value.copy(notifications = updated)

            if (item.id.startsWith("local_")) {
                localStore.markAsRead(item.id.removePrefix("local_"))
            } else {
                try {
                    db.collection("notifications").document(item.id)
                        .update("read", true).await()
                } catch (_: Exception) {}
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val unread = _uiState.value.notifications.filter { !it.read }
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map { it.copy(read = true) },
            )
            localStore.markAllAsRead()
            unread.filter { !it.id.startsWith("local_") }.forEach { item ->
                try {
                    db.collection("notifications").document(item.id)
                        .update("read", true).await()
                } catch (_: Exception) {}
            }
        }
    }

    fun delete(item: NotificationItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.filter { it.id != item.id },
            )
            if (item.id.startsWith("local_")) {
                localStore.delete(item.id.removePrefix("local_"))
            } else {
                try {
                    db.collection("notifications").document(item.id).delete().await()
                } catch (_: Exception) {}
            }
        }
    }
}
