package com.eodumneomeo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidanceRulesTest {
    private fun result(x: Intersection = Fx.x(), make: (LatLon, Long) -> VehicleState): Result? =
        Fx.cross(GuidanceEngine(listOf(x)), x, make = make).decisions.firstOrNull()?.result

    @Test fun `야간 전조등 켜짐이면 안내`() = assertEquals(Result.ANNOUNCE, result { p, t -> Fx.state(p, t) })

    @Test fun `주간 전조등·와이퍼 꺼짐이면 생략`() =
        assertEquals(Result.SKIP_CONDITION, result { p, t -> Fx.state(p, t, light = false) })

    @Test fun `와이퍼 작동이면 안내`() =
        assertEquals(Result.ANNOUNCE, result { p, t -> Fx.state(p, t, light = false, wiper = true) })

    @Test fun `사고다발 교차로는 주간에도 안내`() =
        assertEquals(Result.ANNOUNCE, result(Fx.x(risk = Risk.HOTSPOT)) { p, t -> Fx.state(p, t, light = false) })

    @Test fun `시속 20km 미만이면 생략, 20km는 안내`() {
        assertEquals(Result.SKIP_SPEED, result { p, t -> Fx.state(p, t, speed = 19.9) })
        assertEquals(Result.ANNOUNCE, result { p, t -> Fx.state(p, t, speed = 20.0) })
    }

    @Test fun `방향지시등이 켜지면 속도와 관계없이 안내`() {
        assertEquals(Result.ANNOUNCE, result { p, t -> Fx.state(p, t, speed = 10.0, turn = TurnSignal.LEFT) })
        assertEquals(Result.ANNOUNCE, result { p, t -> Fx.state(p, t, speed = 5.0, turn = TurnSignal.RIGHT) })
    }

    @Test fun `브레이크를 밟고 있으면 방향지시등이 있어도 생략`() {
        assertEquals(Result.SKIP_BRAKE, result { p, t -> Fx.state(p, t, brake = true) })
        assertEquals(Result.SKIP_BRAKE, result { p, t -> Fx.state(p, t, brake = true, turn = TurnSignal.LEFT) })
    }

    @Test fun `50m를 넘기 전에는 판정하지 않는다`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        e.update(Fx.state(Fx.southOf(x, 60.0)))
        assertTrue(e.update(Fx.state(Fx.southOf(x, 55.0), 1000)).decisions.isEmpty())
    }

    @Test fun `처음 받은 위치가 이미 50m 안이면 판정하지 않는다`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        assertTrue(e.update(Fx.state(Fx.southOf(x, 40.0))).decisions.isEmpty())
        assertTrue(e.update(Fx.state(Fx.southOf(x, 35.0), 1000)).decisions.isEmpty())
    }

    @Test fun `같은 교차로는 한 주행에 1회만`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        assertEquals(1, Fx.cross(e, x).speech.size)
        val again = Fx.cross(e, x, t0 = 5000)
        assertTrue(again.decisions.isEmpty() && again.speech.isEmpty())
    }

    @Test fun `기어 P 후 새 주행이면 다시 안내`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        Fx.cross(e, x)
        e.update(Fx.state(Fx.southOf(x, 49.0), 3000, gear = Gear.P, speed = 0.0))
        assertEquals(Result.ANNOUNCE, Fx.cross(e, x, t0 = 5000).decisions.single().result)
    }

    @Test fun `진행 방향 뒤의 교차로는 안내하지 않는다`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        e.update(Fx.state(Fx.southOf(x, 60.0), heading = 180.0))
        assertTrue(e.update(Fx.state(Fx.southOf(x, 49.0), 1000, heading = 180.0)).decisions.isEmpty())
    }

    @Test fun `신호 교차로는 안내하지 않는다`() =
        assertEquals(null, result(Fx.x(signalized = true)) { p, t -> Fx.state(p, t) })

    @Test fun `150m 안의 교차로는 묶어서 한 번만`() {
        val a = Fx.x("A"); val b = Fx.x("B", lat = Fx.northOf(a, 100.0).lat)
        val e = GuidanceEngine(listOf(a, b))
        val out = Fx.cross(e, a)
        val speak = out.speech.filterIsInstance<SpeechCommand.Speak>().single()
        assertEquals(listOf("A", "B"), speak.intersectionIds)
        assertEquals(Result.SKIP_CLUSTERED, out.decisions.first { it.intersectionId == "B" }.result)
        // B 앞 50m를 지날 때 다시 안내하지 않는다
        e.update(Fx.state(Fx.southOf(b, 60.0), 9000))
        assertTrue(e.update(Fx.state(Fx.southOf(b, 49.0), 10000)).speech.none { it is SpeechCommand.Speak })
    }

    @Test fun `150m보다 멀면 각각 안내`() {
        val a = Fx.x("A"); val b = Fx.x("B", lat = Fx.northOf(a, 200.0).lat)
        val e = GuidanceEngine(listOf(a, b))
        val first = Fx.cross(e, a).speech.filterIsInstance<SpeechCommand.Speak>().single()
        assertEquals(listOf("A"), first.intersectionIds)
        e.update(Fx.state(Fx.northOf(a, 10.0), 3000))
        assertEquals(Result.ANNOUNCE, Fx.cross(e, b, t0 = 9000).decisions.single().result)
    }

    @Test fun `지나친 안내는 취소한다`() {
        val x = Fx.x(); val e = GuidanceEngine(listOf(x))
        Fx.cross(e, x)
        assertTrue(e.update(Fx.state(Fx.northOf(x, 5.0), 3000)).speech.contains(SpeechCommand.Cancel("X")))
    }
}
