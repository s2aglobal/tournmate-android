package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.toCalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCalorieRecordRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : CalorieRecordRepository {

    private val collection get() = db.collection("calorieRecords")

    override suspend fun save(record: CalorieRecord) {
        val data = mapOf(
            "sessionId" to record.sessionId.toString().uppercase(),
            "playerId" to record.playerId.toString().uppercase(),
            "calories" to record.calories,
            "source" to record.source.rawValue,
            "weightUsedKg" to record.weightUsedKg,
            "durationMinutes" to record.durationMinutes,
            "date" to Timestamp(record.date),
            "sessionTitle" to record.sessionTitle,
            "activityType" to record.activityType.rawValue,
        )
        collection.document(record.id).set(data).await()
    }

    override suspend fun findRecord(sessionId: UUID, playerId: UUID): CalorieRecord? =
        collection
            .whereEqualTo("sessionId", sessionId.toString())
            .whereEqualTo("playerId", playerId.toString())
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toCalorieRecord()

    override suspend fun listRecords(playerId: UUID): List<CalorieRecord> {
        // Query both uppercase and lowercase to handle iOS/Android UUID format differences
        val uppercaseResults = collection
            .whereEqualTo("playerId", playerId.toString().uppercase())
            .get().await().documents.mapNotNull { it.toCalorieRecord() }

        val lowercaseResults = if (playerId.toString().uppercase() != playerId.toString().lowercase()) {
            collection
                .whereEqualTo("playerId", playerId.toString().lowercase())
                .get().await().documents.mapNotNull { it.toCalorieRecord() }
        } else emptyList()

        return (uppercaseResults + lowercaseResults)
            .distinctBy { it.id }
            .sortedByDescending { it.date }
    }
}
