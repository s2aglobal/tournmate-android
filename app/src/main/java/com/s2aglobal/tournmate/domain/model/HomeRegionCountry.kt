package com.s2aglobal.tournmate.domain.model

data class HomeRegionCountry(
    val code: String,
    val name: String,
) {
    companion object {
        val fallback = HomeRegionCountry("US", "United States")

        val pickerOptions = listOf(
            HomeRegionCountry("AE", "United Arab Emirates"),
            HomeRegionCountry("AU", "Australia"),
            HomeRegionCountry("BR", "Brazil"),
            HomeRegionCountry("CA", "Canada"),
            HomeRegionCountry("CH", "Switzerland"),
            HomeRegionCountry("CN", "China"),
            HomeRegionCountry("DE", "Germany"),
            HomeRegionCountry("ES", "Spain"),
            HomeRegionCountry("FR", "France"),
            HomeRegionCountry("GB", "United Kingdom"),
            HomeRegionCountry("HK", "Hong Kong"),
            HomeRegionCountry("ID", "Indonesia"),
            HomeRegionCountry("IN", "India"),
            HomeRegionCountry("IT", "Italy"),
            HomeRegionCountry("JP", "Japan"),
            HomeRegionCountry("KR", "South Korea"),
            HomeRegionCountry("MX", "Mexico"),
            HomeRegionCountry("MY", "Malaysia"),
            HomeRegionCountry("NL", "Netherlands"),
            HomeRegionCountry("NZ", "New Zealand"),
            HomeRegionCountry("PH", "Philippines"),
            HomeRegionCountry("SG", "Singapore"),
            HomeRegionCountry("TH", "Thailand"),
            HomeRegionCountry("TW", "Taiwan"),
            HomeRegionCountry("US", "United States"),
            HomeRegionCountry("VN", "Vietnam"),
        )

        fun matching(code: String): HomeRegionCountry? =
            pickerOptions.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
