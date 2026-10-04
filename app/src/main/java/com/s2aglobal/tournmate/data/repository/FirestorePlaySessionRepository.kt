package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.toPlaySession
import com.s2aglobal.tournmate.domain.model.PlaySession
import com.s2aglobal.tournmate.domain.model.PlaySessionStatus
import com.s2aglobal.tournmate.domain.model.storedRawValue
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirestorePlaySessionRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : PlaySessionRepository {

    private val collection get() = db.collection("playSessions")

    override suspend fun find(id: String): PlaySession? {
        val upper = id.uppercase()
        val doc = collection.document(upper).get().await()
        if (doc.exists()) return doc.toPlaySession()
        // Fallback for legacy lowercase document IDs
        val lower = id.lowercase()
        if (lower == upper) return null
        val fallback = collection.document(lower).get().await()
        return fallback.toPlaySession()
    }

    override suspend fun listUpcoming(): List<PlaySession> {
        val now = Date()
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.toPlaySession() }
            .filter { !it.isPast && it.status == PlaySessionStatus.ACTIVE }
            .sortedBy { it.date }
    }

    override suspend fun listPast(): List<PlaySession> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.toPlaySession() }
            .filter { it.isPast || it.status == PlaySessionStatus.COMPLETED }
            .sortedByDescending { it.date }
            .take(20)
    }

    override suspend fun create(session: PlaySession) {
        val data = session.toFirestoreMap()
        collection.document(session.id.toString().uppercase()).set(data).await()
    }

    override suspend fun update(session: PlaySession) {
        val data = session.toFirestoreMap()
        collection.document(session.id.toString().uppercase()).update(data).await()
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }

    override suspend fun join(sessionId: String, playerId: String) {
        collection.document(sessionId).update(
            "attendeeIds", FieldValue.arrayUnion(playerId)
        ).await()
    }

    override suspend fun leave(sessionId: String, playerId: String) {
        collection.document(sessionId).update(
            "attendeeIds", FieldValue.arrayRemove(playerId)
        ).await()
    }

    override suspend fun cancel(id: String) {
        collection.document(id).update("status", PlaySessionStatus.CANCELLED.rawValue).await()
    }
}

private fun PlaySession.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("title", title)
    put("venue", venue)
    put("venueAddress", venueAddress)
    venueLatitude?.let { put("venueLatitude", it) }
    venueLongitude?.let { put("venueLongitude", it) }
    countryCode?.let { put("countryCode", it) }
    postalCode?.let { put("postalCode", it) }
    put("date", Timestamp(date))
    durationMinutes?.let { put("durationMinutes", it) }
    put("skillLevel", skillLevelRaw)
    put("gameType", gameTypeRaw)
    costPerPerson?.let { put("costPerPerson", it) }
    put("currency", currency)
    notes?.let { put("notes", it) }
    put("preferredAgeGroup", preferredAgeGroupRaw)
    put("status", statusRaw)
    hostId?.let { put("hostId", it) }
    hostName?.let { put("hostName", it) }
    hostAvatarId?.let { put("hostAvatarId", it) }
    put("createdAt", Timestamp(createdAt))
    timeZone?.let { put("timeZone", it) }
    put("sportType", sportType.storedRawValue(sportTypeRaw))
    put("attendeeIds", attendeeIds)
}
