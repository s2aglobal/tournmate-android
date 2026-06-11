package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.CalorieRecord
import java.util.UUID

interface CalorieRecordRepository {
    suspend fun save(record: CalorieRecord)
    suspend fun findRecord(sessionId: UUID, playerId: UUID): CalorieRecord?
    suspend fun listRecords(playerId: UUID): List<CalorieRecord>
}
