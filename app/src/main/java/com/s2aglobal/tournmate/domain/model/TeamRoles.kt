package com.s2aglobal.tournmate.domain.model

/**
 * Who leads a team in one tournament (captain, vice-captain, wicketkeeper)
 * and what each player does (batter, bowler, …), plus the Player of the
 * Match award. Stored on the registration / match, so the same team can have
 * a different captain in each tournament. Whoever registers the team doesn't
 * have to be the captain, or even play.
 *
 * Members are keyed by player id (compared in upper case) or `guest:<name>`
 * for guests. Same rules as iOS `TeamRoles.swift`.
 */

/** What a player does in the side. Optional for every player. */
enum class PlayingRole(val rawValue: String, val title: String) {
    BATTER("batter", "Batter"),
    BOWLER("bowler", "Bowler"),
    ALL_ROUNDER("allRounder", "All-rounder"),
    WICKETKEEPER_BATTER("wicketkeeperBatter", "Wicketkeeper-batter");

    companion object {
        fun fromRawValue(raw: String?): PlayingRole? = entries.firstOrNull { it.rawValue == raw }
    }
}

/** One person playing for a team in an event: a TournMate player or a guest. */
class RosterMember private constructor(
    /** [TeamRoles] key: upper-cased player id, or `guest:<name>`. */
    val key: String,
    val name: String,
    val player: Player?,
) {
    val isGuest: Boolean get() = player == null

    override fun equals(other: Any?): Boolean = other is RosterMember && other.key == key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = "RosterMember($key, $name)"

    companion object {
        fun of(player: Player): RosterMember = RosterMember(TeamRoles.key(player.id.toString()), player.name, player)
        fun guest(name: String): RosterMember = RosterMember(TeamRoles.guestKey(name), name, null)
    }
}

data class TeamRoles(
    val captain: String? = null,
    val viceCaptain: String? = null,
    val keeper: String? = null,
    val playing: Map<String, PlayingRole> = emptyMap(),
) {
    fun isCaptain(key: String): Boolean = captain?.let(::key) == key(key)
    fun isViceCaptain(key: String): Boolean = viceCaptain?.let(::key) == key(key)
    fun isKeeper(key: String): Boolean = keeper?.let(::key) == key(key)
    fun playingRole(key: String): PlayingRole? = playing[key(key)]

    /**
     * Makes [key] the captain (there is always exactly one). A vice-captain who
     * becomes captain stops being vice-captain. Returns the updated roles.
     */
    fun setCaptain(key: String): TeamRoles {
        val k = key(key)
        return copy(captain = k, viceCaptain = if (isViceCaptain(k)) null else viceCaptain)
    }

    /** Vice-captain on/off. The captain can't also be vice-captain. Returns the updated roles. */
    fun toggleViceCaptain(key: String): TeamRoles {
        val k = key(key)
        return when {
            isViceCaptain(k) -> copy(viceCaptain = null)
            !isCaptain(k) -> copy(viceCaptain = k)
            else -> this
        }
    }

    /** Wicketkeeper on/off. The captain may keep wicket. Returns the updated roles. */
    fun toggleKeeper(key: String): TeamRoles {
        val k = key(key)
        return copy(keeper = if (isKeeper(k)) null else k)
    }

    /** Sets (or clears, with `null`) a player's playing role. Returns the updated roles. */
    fun setPlayingRole(role: PlayingRole?, key: String): TeamRoles {
        val k = key(key)
        return copy(playing = if (role == null) playing - k else playing + (k to role))
    }

    /**
     * Roles for the side as it stands: anyone no longer playing loses their
     * role, and there is always a captain while anyone plays (the preferred
     * member if they play, otherwise the first one).
     */
    fun normalized(roster: List<String>, preferredCaptain: String?): TeamRoles {
        val keys = roster.map(::key)
        val inSide = keys.toSet()
        val first = keys.firstOrNull() ?: return TeamRoles()
        fun keep(k: String?): String? = k?.let(::key)?.takeIf { it in inSide }
        val newCaptain = keep(captain) ?: keep(preferredCaptain) ?: first
        val newPlaying = mutableMapOf<String, PlayingRole>()
        for ((k, role) in playing) if (key(k) in inSide) newPlaying[key(k)] = role
        return TeamRoles(
            captain = newCaptain,
            viceCaptain = keep(viceCaptain)?.takeIf { it != newCaptain },
            keeper = keep(keeper),
            playing = newPlaying,
        )
    }

    /** Short badges for a member, in display order: "C", "VC", "WK". */
    fun badges(key: String): List<String> = buildList {
        if (isCaptain(key)) add("C")
        if (isViceCaptain(key)) add("VC")
        if (isKeeper(key)) add("WK")
    }

    /** Fields written on the registration. Empty roles write nothing. */
    val firestoreFields: Map<String, Any>
        get() = buildMap {
            captain?.let { put("captainId", it) }
            viceCaptain?.let { put("viceCaptainId", it) }
            keeper?.let { put("keeperId", it) }
            if (playing.isNotEmpty()) put("playerRoles", playing.mapValues { it.value.rawValue })
        }

    companion object {
        const val GUEST_PREFIX = "guest:"

        fun guestKey(name: String): String = GUEST_PREFIX + name

        /** Normalized key: player ids in upper case, guest keys unchanged. */
        fun key(raw: String): String = if (raw.startsWith(GUEST_PREFIX)) raw else raw.uppercase()

        /** Reads roles from a registration document (keys normalized; unknown roles skipped). */
        fun fromFirestore(data: Map<String, Any?>): TeamRoles {
            val playing = mutableMapOf<String, PlayingRole>()
            (data["playerRoles"] as? Map<*, *>)?.forEach { (k, raw) ->
                val role = PlayingRole.fromRawValue(raw as? String)
                if (k is String && role != null) playing[key(k)] = role
            }
            return TeamRoles(
                captain = (data["captainId"] as? String)?.let(::key),
                viceCaptain = (data["viceCaptainId"] as? String)?.let(::key),
                keeper = (data["keeperId"] as? String)?.let(::key),
                playing = playing,
            )
        }
    }
}

/** The Player of the Match award on a cricket result. Optional; never for a no result. */
data class PlayerOfMatch(
    /** [TeamRoles] key of the player (or `guest:<name>`). */
    val key: String,
    val name: String,
    /** Registration id of the team they played for. */
    val teamRegistrationId: String,
)

/** "Player of the tournament": who won Player of the Match most often. */
data class PlayerAwardTally(
    val key: String,
    val name: String,
    val teamRegistrationId: String,
    val awards: Int,
) {
    val id: String get() = "$teamRegistrationId|$key"

    companion object {
        /** Leaders by awards (most first, then name), over finished matches only. */
        fun tally(matches: List<Match>): List<PlayerAwardTally> {
            val byId = linkedMapOf<String, PlayerAwardTally>()
            for (match in matches) {
                if (match.status != MatchStatus.FINISHED) continue
                val pom = match.playerOfMatch ?: continue
                val entry = PlayerAwardTally(TeamRoles.key(pom.key), pom.name, pom.teamRegistrationId, 0)
                val current = byId[entry.id] ?: entry
                byId[entry.id] = current.copy(awards = current.awards + 1)
            }
            return byId.values.sortedWith(
                compareByDescending<PlayerAwardTally> { it.awards }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    .thenBy { it.id },
            )
        }
    }
}
