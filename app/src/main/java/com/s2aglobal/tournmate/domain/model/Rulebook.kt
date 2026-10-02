package com.s2aglobal.tournmate.domain.model

/** A sport's quick-reference rulebook shown in Discover. */
data class Rulebook(
    val title: String,
    val source: String,
    val sections: List<RuleSection>,
)

/** Quick-reference rules for this sport, or `null` when we don't have them yet. */
val SportType.rulebook: Rulebook?
    get() = when (this) {
        SportType.BADMINTON -> Rulebook(
            title = "Official BWF Laws of Badminton",
            source = "Badminton World Federation",
            sections = BadmintonRules.sections,
        )
        SportType.PICKLEBALL -> Rulebook(
            title = "Official Rules of Pickleball",
            source = "USA Pickleball Official Rulebook",
            sections = PickleballRules.sections,
        )
        else -> null
    }
