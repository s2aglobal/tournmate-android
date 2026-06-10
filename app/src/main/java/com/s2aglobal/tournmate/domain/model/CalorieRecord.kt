package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class CalorieRecord(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: UUID = UUID.randomUUID(),
    val playerId: UUID = UUID.randomUUID(),
    val calories: Double = 0.0,
    val source: CalorieSource = CalorieSource.ESTIMATED,
    val weightUsedKg: Double = 70.0,
    val durationMinutes: Int = 0,
    val date: Date = Date(),
    val sessionTitle: String? = null,
    val activityType: CalorieActivityType = CalorieActivityType.OPEN_PLAY,
) {
    val formattedCalories: String
        get() = "${calories.toInt()} cal"
}
