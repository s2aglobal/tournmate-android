package com.s2aglobal.tournmate.ui.navigation

object Routes {
    const val AUTH_GATE = "auth_gate"
    const val WELCOME = "welcome"
    const val LOGIN = "login/{createMode}"
    fun login(createMode: Boolean = false) = "login/$createMode"
    const val PROFILE_SETUP = "profile_setup"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"

    const val TOURNAMENT_DETAIL = "tournament_detail/{tournamentId}"
    const val OPEN_PLAY_DETAIL = "open_play_detail/{sessionId}"
    const val PLAYER_PROFILE = "player_profile/{playerId}"

    fun tournamentDetail(tournamentId: String) = "tournament_detail/$tournamentId"
    fun openPlayDetail(sessionId: String) = "open_play_detail/$sessionId"
    fun playerProfile(playerId: String) = "player_profile/$playerId"
}
