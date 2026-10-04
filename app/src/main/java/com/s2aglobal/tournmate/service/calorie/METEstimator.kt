package com.s2aglobal.tournmate.service.calorie

import com.s2aglobal.tournmate.domain.model.SportType

/*
 * Pure-function calorie estimator using MET (Metabolic Equivalent of Task)
 * values per sport — mirrors iOS METEstimator.swift.
 *
 * Formula: Calories = MET x weight(kg) x duration(hours)
 *
 * Sources:
 * - Compendium of Physical Activities (Ainsworth et al.; 2024 update):
 *   badminton social 5.5 / competitive 7.0, tennis doubles 6.0 / singles 8.0,
 *   table tennis 4.0, volleyball non-competitive 3.0 / competitive 6.0.
 * - Pickleball is not in the Compendium yet. Doubles 4.1 METs was measured
 *   by Smith et al. (2018), Int. J. Research in Exercise Physiology. Singles
 *   scales that by tennis's singles/doubles ratio (8.0 / 6.0) ≈ 5.5.
 */

/** How hard the session was played. */
enum class PlayIntensity {
    /** Social / recreational / doubles. */
    CASUAL,
    /** Singles / intense play. */
    COMPETITIVE,
}

object METEstimator {

    private const val DEFAULT_WEIGHT_KG = 70.0

    /** MET value for a sport at a given intensity. */
    fun metValue(intensity: PlayIntensity, sport: SportType = SportType.BADMINTON): Double = when (sport) {
        SportType.PICKLEBALL -> if (intensity == PlayIntensity.CASUAL) 4.1 else 5.5
        SportType.TENNIS -> if (intensity == PlayIntensity.CASUAL) 6.0 else 8.0
        SportType.TABLE_TENNIS -> 4.0
        SportType.VOLLEYBALL -> if (intensity == PlayIntensity.CASUAL) 3.0 else 6.0
        SportType.DISC_GOLF -> 3.5
        SportType.ROUNDNET -> 6.0
        // Badminton social / competitive (default).
        else -> if (intensity == PlayIntensity.CASUAL) 5.5 else 7.0
    }

    /**
     * Estimates calories burned.
     *
     * @param weightKg Player body weight (defaults to 70 kg if unknown).
     * @param sport    The sport played (defaults to badminton for legacy callers).
     */
    fun estimate(
        durationMinutes: Int,
        weightKg: Double = DEFAULT_WEIGHT_KG,
        intensity: PlayIntensity = PlayIntensity.CASUAL,
        sport: SportType = SportType.BADMINTON,
    ): Double {
        val durationHours = durationMinutes / 60.0
        val weight = if (weightKg > 0) weightKg else DEFAULT_WEIGHT_KG
        return metValue(intensity, sport) * weight * durationHours
    }
}
