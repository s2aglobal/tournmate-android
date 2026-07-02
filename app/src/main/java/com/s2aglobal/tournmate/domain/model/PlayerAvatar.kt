package com.s2aglobal.tournmate.domain.model

/**
 * Pre-designed avatar styles a player can choose from.
 * The [id] is what Firestore stores (matches iOS PlayerAvatar rawValue for cross-platform parity).
 * The [seed] is the DiceBear seed used to generate the image URL (matches iOS seed mapping).
 * The [displayName] is the human-readable label shown in the picker (= the seed name).
 *
 * DiceBear "adventurer" style (CC BY 4.0). Art by Lisa Wischofsky.
 */
enum class PlayerAvatar(
    val id: String,
    private val seed: String,
    val displayName: String,
) {
    // Row 1
    SHUTTLECOCK("shuttlecock", "Aiden",     "Aiden"),
    RACKET(     "racket",      "Riley",     "Riley"),
    CHAMPION(   "champion",    "Valentina", "Valentina"),
    FLAME(      "flame",       "Zara",      "Zara"),

    // Row 2
    BOLT(       "bolt",        "Leo",       "Leo"),
    ACE(        "ace",         "Mika",      "Mika"),
    SMASH(      "smash",       "Jaden",     "Jaden"),
    RALLY(      "rally",       "Nora",      "Nora"),

    // Row 3
    DROP(       "drop",        "Felix",     "Felix"),
    SERVE(      "serve",       "Luna",      "Luna"),
    LEAF(       "leaf",        "Eliza",     "Eliza"),
    STAR(       "star",        "Mason",     "Mason"),

    // Row 4
    HEART(      "heart",       "Sadie",     "Sadie"),
    MOON(       "moon",        "Kai",       "Kai"),
    SUN(        "sun",         "Harper",    "Harper"),
    PHOENIX(    "phoenix",     "Roman",     "Roman"),

    // Row 5
    EAGLE(      "eagle",       "Skyler",    "Skyler"),
    TIGER(      "tiger",       "Dante",     "Dante"),
    PANDA(      "panda",       "Chloe",     "Chloe"),
    NINJA(      "ninja",       "Raven",     "Raven"),

    // Row 6
    RUNNER(     "runner",      "Olive",     "Olive"),
    WAVE(       "wave",        "Miles",     "Miles"),
    CROWN(      "crown",       "Aria",      "Aria"),
    SHIELD(     "shield",      "Brian",     "Brian"),

    // Default (not shown in picker)
    DEFAULT("defaultAvatar", "Default", "Default");

    fun avatarUrl(size: Int = 128): String =
        "https://api.dicebear.com/9.x/adventurer/png?seed=$seed&size=$size"

    companion object {
        val selectable: List<PlayerAvatar> = entries.filter { it != DEFAULT }

        fun fromId(id: String): PlayerAvatar =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
