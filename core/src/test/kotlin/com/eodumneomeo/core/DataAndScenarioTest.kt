package com.eodumneomeo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTest {
    private val db = IntersectionDb.parse(Fx.dataFile("intersections.sample.json").readText(Charsets.UTF_8))

    @Test fun `샘플 DB는 10개이고 모두 샘플 좌표로 표시된다`() {
        assertEquals(10, db.size)
        assertEquals(10, db.map { it.id }.toSet().size)
        assertTrue(db.all { it.source == "SAMPLE_COORDINATE" })
        assertTrue(db.all { it.approaches.size >= 3 })
        assertTrue(db.any { it.risk == Risk.HOTSPOT } && db.any { it.signalized } && db.any { it.kitInstalled })
    }

    @Test fun `설정 파일을 읽는다`() {
        val c = GuidanceConfig.fromProperties(Fx.dataFile("guidance.properties").readText(Charsets.UTF_8))
        assertEquals(50.0, c.announceDistanceM, 0.0)
        assertEquals(20.0, c.minSpeedKph, 0.0)
        assertEquals(150.0, c.clusterRadiusM, 0.0)
        assertEquals("전방에 시야가 제한된 비신호 교차로가 있습니다. 서행하세요.", c.announceText)
    }

    @Test fun `JSON 파서는 이스케이프와 숫자를 처리한다`() {
        @Suppress("UNCHECKED_CAST")
        val m = MiniJson("""{"a":"x\"yA","b":[1,2.5,-3e2],"c":true,"d":null}""").parse() as Map<String, Any?>
        assertEquals("x\"yA", m["a"])
        assertEquals(listOf(1.0, 2.5, -300.0), m["b"])
        assertEquals(true, m["c"])
        assertEquals(null, m["d"])
    }
}

class ScenarioReplayTest {
    private val db = IntersectionDb.parse(Fx.dataFile("intersections.sample.json").readText(Charsets.UTF_8))
    private val cfg = GuidanceConfig.fromProperties(Fx.dataFile("guidance.properties").readText(Charsets.UTF_8))

    private fun replay(file: String): Pair<List<Decision>, List<SpeechCommand>> {
        val sc = Scenario.parseCsv(file, Fx.dataFile("scenarios/$file").readText(Charsets.UTF_8))
        val e = GuidanceEngine(db, cfg)
        val d = ArrayList<Decision>(); val s = ArrayList<SpeechCommand>()
        var t = 0L
        while (t <= sc.durationMs) {
            val out = e.update(sc.stateAt(t)!!)
            d += out.decisions; s += out.speech; t += 100
        }
        return d to s
    }

    private fun speaks(s: List<SpeechCommand>) = s.filterIsInstance<SpeechCommand.Speak>()

    @Test fun `s01 120m에서 출발해 50m에서 한 번 안내하고 통과 후 취소한다`() {
        val (d, s) = replay("s01_night_basic.csv")
        val one = d.single()
        assertEquals("SAMPLE-SS-01" to Result.ANNOUNCE, one.intersectionId to one.result)
        assertTrue("안내 거리 ${one.distanceM}", one.distanceM in 49.0..50.0)
        assertEquals(1, speaks(s).size)
        assertTrue(s.contains(SpeechCommand.Cancel("SAMPLE-SS-01")))
    }

    @Test fun `s02 주간에는 안내하지 않는다`() {
        val (d, s) = replay("s02_day_no_light.csv")
        assertEquals(Result.SKIP_CONDITION, d.single().result)
        assertTrue(speaks(s).isEmpty())
    }

    @Test fun `s03 사고다발 교차로는 주간에도 안내`() =
        assertEquals("SAMPLE-SS-02" to Result.ANNOUNCE, replay("s03_hotspot_day.csv").first.single().let { it.intersectionId to it.result })

    @Test fun `s04 저속이어도 방향지시등이면 안내`() =
        assertEquals(Result.ANNOUNCE, replay("s04_slow_turn_signal.csv").first.single().result)

    @Test fun `s05 브레이크 중이면 생략`() =
        assertEquals(Result.SKIP_BRAKE, replay("s05_brake.csv").first.single().result)

    @Test fun `s06 100m 간격 교차로 2곳은 한 번만 안내`() {
        val (d, s) = replay("s06_cluster.csv")
        assertEquals(listOf("SAMPLE-SS-05" to Result.ANNOUNCE, "SAMPLE-SS-06" to Result.SKIP_CLUSTERED), d.map { it.intersectionId to it.result })
        assertEquals(listOf("SAMPLE-SS-05", "SAMPLE-SS-06"), speaks(s).single().intersectionIds)
    }
}
