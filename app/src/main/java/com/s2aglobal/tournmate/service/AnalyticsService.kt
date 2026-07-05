package com.s2aglobal.tournmate.service

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Analytics — thin wrapper around Firebase Analytics.
// Centralises all event names and param keys (mirrors iOS AnalyticsService.swift).
// Usage:
//   @Inject lateinit var analytics: AnalyticsService
//   analytics.screenView(ScreenName.PLAY_TAB)
//   analytics.log(EventName.SESSION_JOINED, Param.SESSION_ID to sessionId)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Singleton
class AnalyticsService @Inject constructor(
    private val firebase: FirebaseAnalytics,
) {

    fun screenView(name: ScreenName, extra: Map<String, Any> = emptyMap()) {
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, name.value)
            extra.forEach { (k, v) ->
                when (v) {
                    is String -> param(k, v)
                    is Long   -> param(k, v)
                    is Double -> param(k, v)
                    is Int    -> param(k, v.toLong())
                    else      -> param(k, v.toString())
                }
            }
        }
    }

    fun log(event: EventName, vararg params: Pair<String, Any>) {
        firebase.logEvent(event.value) {
            params.forEach { (k, v) ->
                when (v) {
                    is String -> param(k, v)
                    is Long   -> param(k, v)
                    is Double -> param(k, v)
                    is Int    -> param(k, v.toLong())
                    else      -> param(k, v.toString())
                }
            }
        }
    }

    fun setUserId(id: String?) {
        firebase.setUserId(id)
    }

    fun setUserProperty(name: String, value: String?) {
        firebase.setUserProperty(name, value)
    }

    // ── Screen Names (matches iOS ScreenName enum) ──────────────────────────

    enum class ScreenName(val value: String) {
        SPLASH("splash"),
        WELCOME("welcome"),
        LOGIN("login"),
        PROFILE_SETUP("profile_setup"),
        ONBOARDING("onboarding"),
        PLAY_TAB("play_tab"),
        TOURNAMENT_LIST("tournament_list"),
        TOURNAMENT_DETAIL("tournament_detail"),
        OPEN_PLAY_LIST("open_play_list"),
        OPEN_PLAY_DETAIL("open_play_detail"),
        COURT_FINDER("court_finder"),
        DISCOVER("discover"),
        PROFILE_TAB("profile_tab"),
        PLAYER_PROFILE("player_profile"),
        PLAYERS_LIST("players_list"),
        VENUE_PICKER("venue_picker"),
        RULES("rules"),
    }

    // ── Event Names (matches iOS EventName enum) ─────────────────────────────

    enum class EventName(val value: String) {
        // Auth
        LOGIN_GOOGLE("login_google"),
        LOGIN_EMAIL("login_email"),
        SIGNUP_EMAIL("signup_email"),
        LOGIN_SUCCESS("login_success"),
        LOGIN_FAILED("login_failed"),
        SIGNUP_SUCCESS("signup_success"),
        SIGNUP_FAILED("signup_failed"),
        PASSWORD_RESET("password_reset"),
        CONTINUE_AS_GUEST("continue_as_guest"),
        SIGN_OUT("sign_out"),
        ACCOUNT_DELETED("account_deleted"),
        EMAIL_VERIFICATION_SENT("email_verification_sent"),
        EMAIL_VERIFIED("email_verified"),

        // Profile
        PROFILE_SETUP_COMPLETE("profile_setup_complete"),
        ONBOARDING_COMPLETE("onboarding_complete"),
        PROFILE_UPDATED("profile_updated"),

        // Tournaments
        TOURNAMENT_CREATED("tournament_created"),
        TOURNAMENT_CREATE_FAILED("tournament_create_failed"),
        TOURNAMENT_UPDATED("tournament_updated"),
        TOURNAMENT_CANCELLED("tournament_cancelled"),
        TOURNAMENT_DELETED("tournament_deleted"),
        TOURNAMENT_VIEWED("tournament_viewed"),
        TOURNAMENT_FILTER_CHANGED("tournament_filter_changed"),

        // Registration
        TOURNAMENT_REGISTERED("tournament_registered"),
        TOURNAMENT_REG_FAILED("tournament_reg_failed"),
        TOURNAMENT_WITHDRAWN("tournament_withdrawn"),

        // Matches
        MATCHES_GENERATED("matches_generated"),
        BRACKET_GENERATED("bracket_generated"),
        PAIRINGS_GENERATED("pairings_generated"),
        SCORE_ENTRY_OPENED("score_entry_opened"),
        SCORE_SUBMITTED("score_submitted"),
        SCORE_CONFIRMED("score_confirmed"),
        SCORE_DISPUTED("score_disputed"),
        DISPUTE_RESOLVED("dispute_resolved"),
        ROUND_ADVANCED("round_advanced"),
        MATCHES_RESET("matches_reset"),

        // Open Play
        SESSION_CREATED("session_created"),
        SESSION_CREATE_FAILED("session_create_failed"),
        SESSION_UPDATED("session_updated"),
        SESSION_CANCELLED("session_cancelled"),
        SESSION_FINISHED("session_finished"),
        SESSION_JOINED("session_joined"),
        SESSION_LEFT("session_left"),
        SESSION_VIEWED("session_viewed"),
        SESSION_SHARED("session_shared"),
        OPEN_PLAY_FILTER_CHANGED("open_play_filter_changed"),

        // Courts
        COURT_SEARCH_ZIP("court_search_zip"),
        COURT_SEARCH_LOCATION("court_search_location"),
        COURT_SEARCH_RESULTS("court_search_results"),

        // Discover
        DISCOVER_LINK_OPENED("discover_link_opened"),
        DISCOVER_RULES_OPENED("discover_rules_opened"),

        // Ratings
        PLAYER_RATED("player_rated"),

        // Sharing
        TOURNAMENT_SHARED("tournament_shared"),

        // Deep Links
        DEEP_LINK_OPENED("deep_link_opened"),
    }

    // ── Param Keys (matches iOS Param enum) ─────────────────────────────────

    object Param {
        const val METHOD          = "method"
        const val FORMAT          = "format"
        const val MATCH_FORMAT    = "match_format"
        const val FILTER          = "filter"
        const val COUNT           = "count"
        const val TOURNAMENT_ID   = "tournament_id"
        const val SESSION_ID      = "session_id"
        const val MATCH_ID        = "match_id"
        const val SUCCESS         = "success"
        const val ERROR           = "error"
        const val SOURCE          = "source"
        const val STARS           = "stars"
        const val RESULT_COUNT    = "result_count"
        const val LINK_TITLE      = "link_title"
        const val AGE_GROUP       = "age_group"
        const val GAME_TYPE       = "game_type"
        const val SKILL_LEVEL     = "skill_level"
        const val DEEP_LINK_TYPE  = "deep_link_type"
    }
}

// ── Hilt Module ──────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}
