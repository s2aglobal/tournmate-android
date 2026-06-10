package com.s2aglobal.tournmate.domain.model

enum class PlayerAvatar(val id: String, val displayName: String) {
    AVATAR_01("avatar01", "Ace"),
    AVATAR_02("avatar02", "Blaze"),
    AVATAR_03("avatar03", "Chase"),
    AVATAR_04("avatar04", "Dash"),
    AVATAR_05("avatar05", "Echo"),
    AVATAR_06("avatar06", "Flare"),
    AVATAR_07("avatar07", "Grit"),
    AVATAR_08("avatar08", "Hawk"),
    AVATAR_09("avatar09", "Iron"),
    AVATAR_10("avatar10", "Jade"),
    AVATAR_11("avatar11", "Knox"),
    AVATAR_12("avatar12", "Luna"),
    AVATAR_13("avatar13", "Maverick"),
    AVATAR_14("avatar14", "Nova"),
    AVATAR_15("avatar15", "Onyx"),
    AVATAR_16("avatar16", "Phoenix"),
    AVATAR_17("avatar17", "Raven"),
    AVATAR_18("avatar18", "Storm"),
    AVATAR_19("avatar19", "Titan"),
    AVATAR_20("avatar20", "Viper"),
    AVATAR_21("avatar21", "Wolf"),
    AVATAR_22("avatar22", "Zenith"),
    AVATAR_23("avatar23", "Arrow"),
    AVATAR_24("avatar24", "Bolt"),
    AVATAR_25("avatar25", "Comet"),
    DEFAULT("defaultAvatar", "Default");

    fun avatarUrl(size: Int = 128): String =
        "https://api.dicebear.com/9.x/adventurer/png?seed=$id&size=$size"

    companion object {
        val selectable: List<PlayerAvatar> = entries.filter { it != DEFAULT }

        fun fromId(id: String): PlayerAvatar =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
