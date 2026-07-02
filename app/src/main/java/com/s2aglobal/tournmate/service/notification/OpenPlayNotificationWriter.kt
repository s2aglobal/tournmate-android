package com.s2aglobal.tournmate.service.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.domain.model.PlaySession
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenPlayNotificationWriter @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private val collection get() = db.collection("notifications")

    suspend fun writeSessionCreated(session: PlaySession) {
        val countryCode = session.countryCode ?: return
        val postalCode = session.postalCode ?: return
        val topic = "region_${countryCode}_$postalCode"

        val data = mapOf(
            "topic" to topic,
            "title" to "New Open Play Session",
            "body" to "${session.hostName ?: "Someone"} is hosting: ${session.title}",
            "type" to "openPlayCreated",
            "sessionId" to session.id.toString().uppercase(),
            "sent" to false,
            "read" to false,
            "createdAt" to Timestamp(Date()),
        )
        collection.add(data).await()
    }

    suspend fun writeSessionCancelled(session: PlaySession) {
        for (attendeeId in session.attendeeIds) {
            val data = mapOf(
                "recipientId" to attendeeId,
                "title" to "Session Cancelled",
                "body" to "\"${session.title}\" has been cancelled by the host.",
                "type" to "openPlayCancelled",
                "sessionId" to session.id.toString().uppercase(),
                "sent" to false,
                "read" to false,
                "createdAt" to Timestamp(Date()),
            )
            collection.add(data).await()
        }
    }

    suspend fun writePlayerJoined(session: PlaySession, playerName: String) {
        val hostId = session.hostId ?: return
        val data = mapOf(
            "recipientId" to hostId,
            "title" to "New Attendee",
            "body" to "$playerName joined your session: ${session.title}",
            "type" to "openPlayJoined",
            "sessionId" to session.id.toString().uppercase(),
            "sent" to false,
            "read" to false,
            "createdAt" to Timestamp(Date()),
        )
        collection.add(data).await()
    }
}
