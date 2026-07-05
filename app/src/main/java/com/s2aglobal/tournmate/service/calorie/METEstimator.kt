package com.s2aglobal.tournmate.service.calorie

/**
 * Metabolic Equivalent of Task (MET) calorie estimator — mirrors iOS METEstimator.swift.
 *
 * Formula: Calories = MET × weight(kg) × duration(hours)
 *
 * Badminton MET values:
 *  - Casual play:      4.5  (recreational, general)
 *  - Competitive play: 7.0  (singles, tournament-level)
 */
enum class BadmintonIntensity(val met: Double) {
    CASUAL(4.5),
    COMPETITIVE(7.0),
}

object METEstimator {

    private const val DEFAULT_WEIGHT_KG = 70.0

    /**
     * Estimates calories burned for a badminton session.
     *
     * @param durationMinutes Session duration.
     * @param weightKg        Player body weight (defaults to 70 kg if unknown).
     * @param intensity       [BadmintonIntensity.CASUAL] for doubles/recreational,
     *                        [BadmintonIntensity.COMPETITIVE] for singles/competitive.
     * @return Estimated kilocalories burned.
     */
    fun estimate(
        durationMinutes: Int,
        weightKg: Double = DEFAULT_WEIGHT_KG,
        intensity: BadmintonIntensity = BadmintonIntensity.CASUAL,
    ): Double {
        val durationHours = durationMinutes / 60.0
        val weight = if (weightKg > 0) weightKg else DEFAULT_WEIGHT_KG
        return intensity.met * weight * durationHours
    }
}
