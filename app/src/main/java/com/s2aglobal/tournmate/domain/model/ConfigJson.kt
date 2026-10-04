package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Shared JSON for `formatConfigData` / `scoringConfigData`: lenient on read, complete on write. */
val ConfigJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Parses a stored config object, dropping keys whose value is JSON `null` so
 * they decode as missing and take the property's default — as iOS
 * `decodeIfPresent` does (e.g. `"gamesPerMatch": null` falls back to 3).
 * Throws if [json] isn't a JSON object.
 */
fun parseConfigObject(json: String): JsonObject =
    JsonObject(ConfigJson.parseToJsonElement(json).jsonObject.filterValues { it !is JsonNull })
