package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.Tournament
import java.util.UUID

interface RegistrationRepository {
    suspend fun listRegistrations(tournament: Tournament): List<Registration>
    suspend fun findOpenRegistration(player: Player, tournament: Tournament): Registration?
    suspend fun setPartner(partner: Player, registration: Registration)
    suspend fun clearPartner(registration: Registration)
    suspend fun createRegistration(player: Player, partner: Player?, tournament: Tournament)
    suspend fun deleteRegistration(registration: Registration)
    suspend fun tournamentIds(forPlayerID: UUID): Set<UUID>
}
