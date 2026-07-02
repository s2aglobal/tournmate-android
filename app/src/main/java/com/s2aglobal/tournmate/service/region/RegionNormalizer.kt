package com.s2aglobal.tournmate.service.region

import com.s2aglobal.tournmate.domain.model.Tournament

object RegionNormalizer {

    fun normalizeCountryCode(raw: String?): String? {
        val c = raw?.trim()?.uppercase() ?: return null
        return if (c.length == 2) c else null
    }

    fun normalizePostal(raw: String?, countryCode: String?): String? {
        val r = raw?.trim() ?: return null
        if (r.isEmpty()) return null
        val cc = (countryCode ?: "").uppercase()

        if (cc in PostalCodeRules.supportedCountryCodes) {
            return PostalCodeRules.normalize(r, cc)
        }

        return r.uppercase().replace(" ", "").replace("-", "")
    }

    fun validateHomePostalForCountry(countryCode: String?, postalRaw: String): String? {
        val trimmed = postalRaw.trim()
        if (trimmed.isEmpty()) return "Enter your postal or ZIP code."
        val cc = normalizeCountryCode(countryCode) ?: return "Choose a country."
        return PostalCodeRules.validationError(trimmed, cc)
    }

    fun tournamentMatchesPlayerRegion(
        tournament: Tournament,
        resolvedCountryCode: String? = null,
        playerCountry: String?,
        playerPostal: String?,
    ): Boolean = geographicEntityMatchesPlayerRegion(
        countryCode = tournament.countryCode,
        resolvedCountryCode = resolvedCountryCode,
        playerCountry = playerCountry,
        playerPostal = playerPostal,
    )

    fun sessionMatchesPlayerRegion(
        sessionCountry: String?,
        sessionPostal: String?,
        playerCountry: String?,
        playerPostal: String?,
    ): Boolean = geographicEntityMatchesPlayerRegion(
        countryCode = sessionCountry,
        resolvedCountryCode = null,
        playerCountry = playerCountry,
        playerPostal = playerPostal,
    )

    fun geographicEntityMatchesPlayerRegion(
        countryCode: String?,
        resolvedCountryCode: String?,
        playerCountry: String?,
        playerPostal: String?,
    ): Boolean {
        val pCountry = normalizeCountryCode(playerCountry) ?: return true
        val tCountry = normalizeCountryCode(countryCode) ?: normalizeCountryCode(resolvedCountryCode) ?: return true
        return pCountry == tCountry
    }
}
