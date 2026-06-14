package com.s2aglobal.tournmate.service.validation

import android.content.Context
import android.content.SharedPreferences
import com.s2aglobal.tournmate.domain.model.Player
import java.util.Calendar
import kotlin.math.ceil

enum class EventType(val key: String) {
    TOURNAMENT("tournament"),
    OPEN_PLAY("openPlay"),
}

object EventRateLimiter {

    private const val COOLDOWN_SECONDS = 120L
    private const val MAX_TOURNAMENTS_PER_DAY = 5
    private const val MAX_OPEN_PLAYS_PER_DAY = 8

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("event_rate_limiter", Context.MODE_PRIVATE)
    }

    fun canCreate(type: EventType): ValidationResult {
        val lastCreate = prefs?.getLong("lastCreateTime_${type.key}", 0L) ?: 0L
        if (lastCreate == 0L) return ValidationResult.Valid

        val elapsed = System.currentTimeMillis() / 1000 - lastCreate
        if (elapsed < COOLDOWN_SECONDS) {
            val remaining = (COOLDOWN_SECONDS - elapsed).toInt()
            val minutes = remaining / 60
            val seconds = remaining % 60
            val timeStr = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
            val label = if (type == EventType.TOURNAMENT) "tournament" else "session"
            return ValidationResult.Invalid("Please wait $timeStr before creating another $label.")
        }

        return ValidationResult.Valid
    }

    fun checkDailyLimit(type: EventType): ValidationResult {
        val maxPerDay = if (type == EventType.TOURNAMENT) MAX_TOURNAMENTS_PER_DAY else MAX_OPEN_PLAYS_PER_DAY
        val todayCount = todayCreationCount(type)

        if (todayCount >= maxPerDay) {
            val label = if (type == EventType.TOURNAMENT) "tournaments" else "open play sessions"
            return ValidationResult.Invalid("You can create up to $maxPerDay $label per day. Try again tomorrow.")
        }
        return ValidationResult.Valid
    }

    fun recordCreation(type: EventType) {
        val editor = prefs?.edit() ?: return
        editor.putLong("lastCreateTime_${type.key}", System.currentTimeMillis() / 1000)

        val todayMs = todayStartMs()
        val storedDate = prefs?.getLong("dailyCreateDate_${type.key}", 0L) ?: 0L

        if (storedDate == todayMs) {
            val current = prefs?.getInt("dailyCreateCount_${type.key}", 0) ?: 0
            editor.putInt("dailyCreateCount_${type.key}", current + 1)
        } else {
            editor.putLong("dailyCreateDate_${type.key}", todayMs)
            editor.putInt("dailyCreateCount_${type.key}", 1)
        }
        editor.apply()
    }

    fun hasCompleteProfile(player: Player?): ValidationResult {
        if (player == null) {
            return ValidationResult.Invalid("Please complete your profile before hosting events.")
        }
        if (player.name.trim().isEmpty()) {
            return ValidationResult.Invalid("Please set your name in your profile before hosting events.")
        }
        if (player.homeCountryCode == null) {
            return ValidationResult.Invalid("Please set your home country in your profile before hosting events.")
        }
        return ValidationResult.Valid
    }

    private fun todayCreationCount(type: EventType): Int {
        val todayMs = todayStartMs()
        val storedDate = prefs?.getLong("dailyCreateDate_${type.key}", 0L) ?: 0L
        return if (storedDate == todayMs) prefs?.getInt("dailyCreateCount_${type.key}", 0) ?: 0 else 0
    }

    private fun todayStartMs(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
