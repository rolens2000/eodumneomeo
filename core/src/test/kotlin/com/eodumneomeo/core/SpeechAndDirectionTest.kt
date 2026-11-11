package com.eodumneomeo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechQueueTest {
    private class FakePlayer : SpeechPlayer {
        val log = ArrayList<String>()
        override fun play(key: String, text: String) { log += "play:$key" }
        override fun stop() { log += "stop" }
    }

    private fun speak(k: String) = SpeechCommand.Speak(k, "문장 $k", listOf(k))

    @Test fun `한 번에 한 문장만 재생한다`() {
        val p = FakePlayer(); val q = SpeechQueue(p)
        q.submit(speak("A")); q.submit(speak("B"))
        assertEquals(listOf("play:A"), p.log)
        q.onDone("A")
        assertEquals(listOf("play:A", "play:B"), p.log)
    }

    @Test fun `재생 중인 안내를 취소하면 멈추고 다음으로 넘어간다`() {
        val p = FakePlayer(); val q = SpeechQueue(p)
        q.submit(speak("A")); q.submit(speak("B")); q.submit(SpeechCommand.Cancel("A"))
        assertEquals(listOf("play:A", "stop", "play:B"), p.log)
    }

    @Test fun `대기 중인 안내를 취소하면 재생하지 않는다`() {
        val p = FakePlayer(); val q = SpeechQueue(p)
        q.submit(speak("A")); q.submit(speak("B")); q.submit(SpeechCommand.Cancel("B")); q.onDone("A")
        assertEquals(listOf("play:A"), p.log)
    }

    @Test fun `다른 키의 완료 알림은 무시한다`() {
        val p = FakePlayer(); val q = SpeechQueue(p)
        q.submit(speak("A")); q.submit(speak("B")); q.onDone("B")
        assertEquals(listOf("play:A"), p.log)
    }
}

class DirectionJudgeTest {
    private val x = Fx.x() // 진입도로 S(180), W(270), E(90)
    private fun ev(road: String) = SyntheticEventGenerator.approaching("X", road, 0)

    @Test fun `경계값은 포함한다`() {
        assertEquals(Side.LEFT, DirectionJudge.side(-45.0))
        assertEquals(Side.LEFT, DirectionJudge.side(-135.0))
        assertEquals(Side.RIGHT, DirectionJudge.side(45.0))
        assertEquals(Side.RIGHT, DirectionJudge.side(135.0))
    }

    @Test fun `경계 밖은 판정하지 않는다`() {
        listOf(-44.9, -135.1, 44.9, 135.1, 0.0, 180.0).forEach { assertNull("$it", DirectionJudge.side(it)) }
    }

    @Test fun `각도차는 정규화한다`() {
        assertEquals(-90.0, DirectionJudge.angleDiff(270.0, 0.0), 1e-9)
        assertEquals(20.0, DirectionJudge.angleDiff(10.0, 350.0), 1e-9)
        assertEquals(-20.0, DirectionJudge.angleDiff(350.0, 10.0), 1e-9)
    }

    @Test fun `남쪽에서 북진하면 서쪽은 왼쪽, 동쪽은 오른쪽`() {
        assertEquals(Side.LEFT, DirectionJudge.judge(x, 0.0, ev("W")))
        assertEquals(Side.RIGHT, DirectionJudge.judge(x, 0.0, ev("E")))
    }

    @Test fun `같은 도로의 이벤트는 제외한다`() {
        assertEquals("S", DirectionJudge.egoApproachRoadId(x, 0.0))
        assertNull(DirectionJudge.judge(x, 0.0, ev("S")))
    }

    @Test fun `서쪽에서 동진하면 남쪽은 오른쪽, 정면 동쪽은 판정하지 않는다`() {
        assertEquals("W", DirectionJudge.egoApproachRoadId(x, 90.0))
        assertEquals(Side.RIGHT, DirectionJudge.judge(x, 90.0, ev("S")))
        assertNull(DirectionJudge.judge(x, 90.0, ev("E")))
        assertNull(DirectionJudge.judge(x, 90.0, ev("W")))
    }

    @Test fun `멀어짐·센서 이상·다른 교차로·없는 도로는 판정하지 않는다`() {
        assertNull(DirectionJudge.judge(x, 0.0, SyntheticEventGenerator.receding("X", "W", 0)))
        assertNull(DirectionJudge.judge(x, 0.0, SyntheticEventGenerator.fault("X", 0)))
        assertNull(DirectionJudge.judge(x, 0.0, SyntheticEventGenerator.approaching("Y", "W", 0)))
        assertNull(DirectionJudge.judge(x, 0.0, ev("N")))
    }
}
