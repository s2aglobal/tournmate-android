package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.PlaySession

interface PlaySessionRepository {
    suspend fun find(id: String): PlaySession?
    suspend fun listUpcoming(): List<PlaySession>
    suspend fun listPast(): List<PlaySession>
    suspend fun create(session: PlaySession)
    suspend fun update(session: PlaySession)
    suspend fun delete(id: String)
    suspend fun join(sessionId: String, playerId: String)
    suspend fun leave(sessionId: String, playerId: String)
    suspend fun cancel(id: String)
}
