package com.s2aglobal.tournmate.service.config

import com.s2aglobal.tournmate.domain.model.SportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SportCatalogParserTest {

    private fun doc(gated: Any?): Map<String, Any?> = mapOf(
        "schemaVersion" to 1,
        "live" to listOf("pickleball", "badminton", "tennis"),
        "categories" to listOf(
            mapOf("id" to "racket_paddle", "sports" to listOf("pickleball", "badminton", "tennis")),
            mapOf("id" to "court_field", "sports" to listOf("basketball", "cricket")),
        ),
        "liveMinAppVersion" to gated,
    )

    private fun parse(gated: Any?, version: String?) =
        SportCatalogParser.parse(SportCatalogParser.rawFromMap(doc(gated))!!, AppVersion.fromVersionName(version))!!

    private val cricketGate = mapOf("cricket" to mapOf("ios" to "1.8", "android" to "1.1"))

    @Test fun gatedSportLiveAtMinimumVersion() {
        val state = parse(cricketGate, "1.1.0-dev")
        assertTrue(state.isLive(SportType.CRICKET))
        assertEquals(SportType.CRICKET, state.liveSports.last())
    }

    @Test fun gatedSportLiveOnNewerVersion() {
        assertTrue(parse(cricketGate, "1.10").isLive(SportType.CRICKET))
        assertTrue(parse(cricketGate, "2.0.0").isLive(SportType.CRICKET))
    }

    @Test fun gatedSportNotLiveOnOlderVersion() {
        assertFalse(parse(cricketGate, "1.0.0").isLive(SportType.CRICKET))
        assertFalse(parse(cricketGate, "1.0.9").isLive(SportType.CRICKET))
    }

    @Test fun iosOnlyGateIsIgnored() =
        assertFalse(parse(mapOf("cricket" to mapOf("ios" to "1.8")), "9.9").isLive(SportType.CRICKET))

    @Test fun missingOrMalformedGateKeepsDoc() {
        assertEquals(3, parse(null, "1.1").liveSports.size)
        assertEquals(3, parse("oops", "1.1").liveSports.size)
        assertEquals(3, parse(mapOf("cricket" to "1.1"), "1.1").liveSports.size)
        assertFalse(parse(mapOf("cricket" to mapOf("android" to "x.y")), "1.1").isLive(SportType.CRICKET))
    }

    @Test fun unknownGatedIdsSkipped() {
        val state = parse(mapOf("quidditch" to mapOf("android" to "1.0")), "1.1")
        assertEquals(listOf(SportType.PICKLEBALL, SportType.BADMINTON, SportType.TENNIS), state.liveSports)
    }

    @Test fun cricketNotRequiredInLive() {
        val state = parse(cricketGate, "1.1")
        assertEquals(listOf(SportType.PICKLEBALL, SportType.BADMINTON, SportType.TENNIS, SportType.CRICKET), state.liveSports)
    }
}
