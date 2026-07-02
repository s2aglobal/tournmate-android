package com.s2aglobal.tournmate.domain.model

import androidx.compose.ui.graphics.Color
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import com.s2aglobal.tournmate.ui.theme.PrizeGold
import com.s2aglobal.tournmate.ui.theme.SuccessGreen

data class SeedTier(
    val title: String,
    val badgeColor: Color,
) {
    val displayName: String get() = title
    val color: Color get() = badgeColor

    companion object {
        fun fromElo(elo: Double): SeedTier = SeedTierRules.tier(elo)
    }
}

object SeedTierRules {
    fun tier(elo: Double): SeedTier = when {
        elo >= 1600 -> SeedTier("Seed 1", PrizeGold)
        elo >= 1400 -> SeedTier("Seed 2", BrandPurple)
        elo >= 1200 -> SeedTier("Seed 3", SuccessGreen)
        else -> SeedTier("Seed 4", Color.Gray)
    }
}
