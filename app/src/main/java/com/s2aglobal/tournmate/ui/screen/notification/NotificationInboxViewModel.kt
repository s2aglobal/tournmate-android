package com.s2aglobal.tournmate.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.service.notification.LocalNotificationStore
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val localStore: LocalNotificationStore,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationInboxUiState())
    val uiState: StateFlow<NotificationInboxUiState> = _uiState.asStateFlow()

    val unreadCount: Int get() = _uiState.value.notifications.count { !it.read }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.notifications.isEmpty())
            val firebaseUid = currentUserStore.firebaseUid()
            val combined = mutableListOf<NotificationItem>()

            // 1. Firestore per-user inbox docs (direct recipient notifications)
            if (firebaseUid != null) {
                try {
                    val snapshot = db.collection("notifications")
                        .whereEqualTo("recipientId", firebaseUid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .await()

                    snapshot.documents.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val title = data["title"] as? String ?: return@forEach
                        val body  = data["body"] as? String ?: ""
                        val ts = (data["createdAt"] as? com.google.firebase.Timestamp)
                            ?.toDate() ?: Date()
                        combined += NotificationItem(
                            id           = doc.id,
                            type         = data["type"] as? String ?: "",
                            title        = title,
                            body         = body,
                            tournamentId = data["tournamentId"] as? String,
                            sessionId    = data["sessionId"] as? String,
                            read         = data["read"] as? Boolean ?: false,
                            createdAt    = ts,
                        )
                    }
                } catch (_: Exception) {}
            }

            // 2. Local push notifications (broadcast topics not written to Firestore)
            val localEntries = localStore.loadAll()
            val firestoreSessionIds    = combined.mapNotNull { it.sessionId }.toSet()
            val firestoreTournamentIds = combined.mapNotNull { it.tournamentId }.toSet()

            for (entry in localEntries) {
                val isDuplicate = entry.sessionId    != null && entry.sessionId in firestoreSessionIds ||
                                  entry.tournamentId != null && entry.tournamentId in firestoreTournamentIds
                if (isDuplicate) continue
                combined += NotificationItem(
                    id           = "local_${entry.id}",
                    type         = entry.type,
                    title        = entry.title,
                    body         = entry.body,
                    tournamentId = entry.tournamentId,
                    sessionId    = entry.sessionId,
                    read         = entry.read,
                    createdAt    = Date(entry.createdAt),
                )
            }

            // 3. Sort by date descending
            _uiState.value = NotificationInboxUiState(
                notifications = combined.sortedByDescending { it.createdAt },
                isLoading     = false,
            )
        }
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
                notifications = _uiState.value.notifications.map { it.copy(read = true) }
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
                notifications = _uiState.value.notifications.filter { it.id != item.id }
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
