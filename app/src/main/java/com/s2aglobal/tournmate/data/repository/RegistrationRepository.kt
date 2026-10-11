package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.Team
import com.s2aglobal.tournmate.domain.model.TeamRoles
import com.s2aglobal.tournmate.domain.model.Tournament
import java.util.UUID

interface RegistrationRepository {
    suspend fun listRegistrations(tournament: Tournament): List<Registration>
    suspend fun findOpenRegistration(player: Player, tournament: Tournament): Registration?
    suspend fun setPartner(partner: Player, registration: Registration)
    suspend fun clearPartner(registration: Registration)
    suspend fun createRegistration(player: Player, partner: Player?, tournament: Tournament)
    /**
     * Registers a team (team sports). Whoever registers is the registration's
     * `playerId` (they needn't play); [rosterIds] and [guestNames] are who
     * plays in this event and [roles] names the captain and the rest.
     * Throws [RegistrationAlreadyEnteredException] if they already entered a team.
     */
    suspend fun createTeamRegistration(
        team: Team, registrant: Player, rosterIds: List<String>, guestNames: List<String>,
        roles: TeamRoles, tournament: Tournament,
    )
    /** Changes who plays for a team entry (captain, or a roster player leaving). */
    suspend fun updateRoster(registration: Registration, rosterIds: List<String>, guestNames: List<String>)
    suspend fun deleteRegistration(registration: Registration)
    suspend fun tournamentIds(forPlayerID: UUID): Set<UUID>
}

/** This person already entered a team in the tournament (one entry per person). */
class RegistrationAlreadyEnteredException : Exception("You've already registered a team for this tournament.")
