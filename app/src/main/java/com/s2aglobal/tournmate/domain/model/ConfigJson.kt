package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.json.Json

/** Shared JSON for `formatConfigData` / `scoringConfigData`: lenient on read, complete on write. */
val ConfigJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
