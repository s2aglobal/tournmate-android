package com.s2aglobal.tournmate.service.region

object PostalCodeRules {

    val supportedCountryCodes: Set<String> = setOf(
        "AE", "AU", "BR", "CA", "CH", "CN", "DE", "ES", "FR", "GB", "HK", "ID", "IN", "IT",
        "JP", "KR", "MX", "MY", "NL", "NZ", "PH", "SG", "TH", "TW", "US", "VN",
    )

    fun normalize(postalRaw: String, countryCode: String): String? {
        val cc = countryCode.uppercase()
        if (cc !in supportedCountryCodes) return null
        val raw = postalRaw.trim()
        if (raw.isEmpty()) return null

        return when (cc) {
            "US" -> normalizeUS(raw)
            "IN" -> normalizeIN(raw)
            "GB" -> normalizeGB(raw)
            "CA" -> normalizeCA(raw)
            "NL" -> normalizeNL(raw)
            "JP" -> normalizeJP(raw)
            "BR" -> normalizeBR(raw)
            "AE" -> normalizeAE(raw)
            "TW" -> normalizeTW(raw)
            "AU", "CH", "NZ", "PH" -> normalizeFixedDigits(raw, 4)
            "CN", "SG", "HK", "VN" -> normalizeFixedDigits(raw, 6)
            "DE", "ES", "FR", "IT", "KR", "MX", "MY", "TH", "ID" -> normalizeFixedDigits(raw, 5)
            else -> null
        }
    }

    fun validationError(postalRaw: String, countryCode: String): String? {
        val trimmed = postalRaw.trim()
        if (trimmed.isEmpty()) return null

        val cc = countryCode.uppercase()
        if (cc !in supportedCountryCodes) {
            return "This country isn't supported for home region. Choose another country."
        }

        return if (normalize(trimmed, cc) != null) null else formatHint(cc)
    }

    private fun formatHint(cc: String): String = when (cc) {
        "US" -> "Enter a 5-digit ZIP or 9-digit ZIP+4 (numbers only)."
        "IN" -> "Enter a 6-digit PIN (numbers only)."
        "GB" -> "Enter a valid UK postcode (e.g. SW1A 1AA or GIR 0AA)."
        "CA" -> "Enter a Canadian postal code (e.g. K1A 0B1)."
        "NL" -> "Enter a Dutch postcode (e.g. 1012 AB)."
        "JP" -> "Enter a 7-digit postal code (numbers only; you may type 123-4567)."
        "BR" -> "Enter a Brazilian CEP (8 digits, e.g. 01310-100)."
        "AE" -> "Enter a 3-5 digit UAE postal code."
        "TW" -> "Enter a Taiwan postal code (5 or 6 digits)."
        "AU", "NZ", "PH" -> "Enter a 4-digit postcode."
        "CH" -> "Enter a 4-digit Swiss postcode."
        "CN" -> "Enter a 6-digit postal code."
        "DE", "ES", "FR", "IT", "KR", "MX", "MY", "TH", "ID" -> "Enter a 5-digit postal code."
        "SG", "HK", "VN" -> "Enter a 6-digit postal code."
        else -> "Enter a valid postal code for this country."
    }

    private fun asciiDigits(raw: String): String = raw.filter { it in '0'..'9' }

    private fun normalizeFixedDigits(raw: String, length: Int): String? {
        val d = asciiDigits(raw)
        return if (d.length == length) d else null
    }

    private fun normalizeUS(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length == 5 || d.length == 9) d else null
    }

    private fun normalizeIN(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length == 6) d else null
    }

    private fun normalizeGB(raw: String): String? {
        val compact = raw.uppercase().replace(" ", "").replace("-", "")
        if (compact == "GIR0AA") return "GIR 0AA"
        if (compact.length !in 5..7) return null
        val inward = compact.takeLast(3)
        val outward = compact.dropLast(3)
        val candidate = "$outward $inward"
        val pattern = Regex("^[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2}$")
        return if (pattern.matches(candidate)) candidate else null
    }

    private val caLettersAny = "ABCEGHJKLMNPRSTVWXYZ".filter { it !in "DFIOQU" }.toSet()
    private val caLettersFirst = caLettersAny.filter { it !in "WZ" }.toSet()

    private fun normalizeCA(raw: String): String? {
        val u = raw.uppercase().replace(" ", "").replace("-", "")
        if (u.length != 6) return null
        val chars = u.toList()
        if (chars[0] !in caLettersFirst) return null
        if (chars[1] !in '0'..'9') return null
        if (chars[2] !in caLettersAny) return null
        if (chars[3] !in '0'..'9') return null
        if (chars[4] !in caLettersAny) return null
        if (chars[5] !in '0'..'9') return null
        return "${chars[0]}${chars[1]}${chars[2]} ${chars[3]}${chars[4]}${chars[5]}"
    }

    private fun normalizeNL(raw: String): String? {
        val u = raw.uppercase().replace(" ", "").replace("-", "")
        if (u.length != 6) return null
        val digits = u.take(4)
        val letters = u.takeLast(2)
        if (!digits.all { it.isDigit() }) return null
        if (!letters.all { it.isLetter() }) return null
        val spaced = "$digits $letters"
        val pattern = Regex("^[0-9]{4} [A-Z]{2}$")
        return if (pattern.matches(spaced)) spaced else null
    }

    private fun normalizeJP(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length == 7) d else null
    }

    private fun normalizeBR(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length == 8) "${d.take(5)}-${d.takeLast(3)}" else null
    }

    private fun normalizeAE(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length in 3..5) d else null
    }

    private fun normalizeTW(raw: String): String? {
        val d = asciiDigits(raw)
        return if (d.length == 5 || d.length == 6) d else null
    }
}
