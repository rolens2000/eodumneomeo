package com.eodumneomeo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    private val o = LatLon(36.7, 126.5)

    @Test fun `위도 0_001도는 약 111m`() = assertEquals(111.19, Geo.distanceM(o, LatLon(36.701, 126.5)), 0.1)

    @Test fun `경도 거리는 위도에 따라 줄어든다`() = assertEquals(89.17, Geo.distanceM(o, LatLon(36.7, 126.501)), 0.2)

    @Test fun `같은 지점 거리는 0`() = assertEquals(0.0, Geo.distanceM(o, o), 1e-9)

    @Test fun `방위 북동남서`() {
        assertEquals(0.0, Geo.bearingDeg(o, LatLon(36.71, 126.5)), 1e-6)
        assertEquals(90.0, Geo.bearingDeg(o, LatLon(36.7, 126.51)), 0.01)
        assertEquals(180.0, Geo.bearingDeg(o, LatLon(36.69, 126.5)), 1e-6)
        assertEquals(270.0, Geo.bearingDeg(o, LatLon(36.7, 126.49)), 0.01)
    }

    @Test fun `각도 정규화`() {
        assertEquals(-90.0, Geo.normalizeDeg(270.0), 1e-9)
        assertEquals(90.0, Geo.normalizeDeg(-270.0), 1e-9)
        assertEquals(180.0, Geo.normalizeDeg(180.0), 1e-9)
        assertEquals(180.0, Geo.normalizeDeg(-180.0), 1e-9)
        assertEquals(0.0, Geo.normalizeDeg(360.0), 1e-9)
        assertEquals(180.0, Geo.normalizeDeg(540.0), 1e-9)
    }
}

class AheadTest {
    private val x = Fx.x()
    private val cfg = Fx.cfg

    @Test fun `앞 100m는 진행 방향 앞`() = assertTrue(Ahead.isAhead(Fx.state(Fx.southOf(x, 100.0)), x, cfg))

    @Test fun `뒤에 있으면 앞이 아니다`() = assertFalse(Ahead.isAhead(Fx.state(Fx.northOf(x, 100.0)), x, cfg))

    @Test fun `옆 90도는 앞이 아니다`() = assertFalse(Ahead.isAhead(Fx.state(Fx.offset(x.position, 270.0, 100.0)), x, cfg))

    @Test fun `원뿔 30도 안팎`() {
        // 자차가 북진할 때 교차로가 25도/35도 방향에 있도록 자차를 반대쪽에 둔다
        assertTrue(Ahead.isAhead(Fx.state(Fx.offset(x.position, 180.0 + 25.0, 100.0)), x, cfg))
        assertFalse(Ahead.isAhead(Fx.state(Fx.offset(x.position, 180.0 + 35.0, 100.0)), x, cfg))
    }

    @Test fun `탐색 거리 밖은 앞이 아니다`() = assertFalse(Ahead.isAhead(Fx.state(Fx.southOf(x, 250.0)), x, cfg))

    @Test fun `지나친 뒤에는 통과로 본다`() {
        assertTrue(Ahead.isPassed(Fx.state(Fx.northOf(x, 5.0)), x, cfg))
        assertFalse(Ahead.isPassed(Fx.state(Fx.southOf(x, 5.0)), x, cfg))
    }
}
