package com.eodumneomeo.core

import java.io.File
import kotlin.math.cos
import kotlin.math.sin

object Fx {
    const val M_PER_DEG_LAT = 111_194.9
    val cfg = GuidanceConfig()

    fun x(id: String = "X", lat: Double = 36.7, lon: Double = 126.5, risk: Risk = Risk.HIGH, signalized: Boolean = false) =
        Intersection(id, lat, lon, listOf(Approach("S", 180.0), Approach("W", 270.0), Approach("E", 90.0)), signalized, risk, false, "TEST")

    /** x에서 bearing 방향으로 m만큼 떨어진 지점 */
    fun offset(from: LatLon, bearingDeg: Double, m: Double): LatLon {
        val r = Math.toRadians(bearingDeg)
        return LatLon(from.lat + m * cos(r) / M_PER_DEG_LAT, from.lon + m * sin(r) / (M_PER_DEG_LAT * cos(Math.toRadians(from.lat))))
    }

    fun southOf(x: Intersection, m: Double) = offset(x.position, 180.0, m)
    fun northOf(x: Intersection, m: Double) = offset(x.position, 0.0, m)

    fun state(
        p: LatLon, t: Long = 0, heading: Double = 0.0, speed: Double = 30.0, gear: Gear = Gear.D,
        light: Boolean = true, wiper: Boolean = false, brake: Boolean = false, turn: TurnSignal = TurnSignal.NONE,
    ) = VehicleState(t, p, heading, speed, gear, light, wiper, brake, turn, synthetic = true)

    /** 남쪽 60m → 49m로 북진시켜 50m 지점을 넘는 순간의 출력을 돌려준다 */
    fun cross(
        e: GuidanceEngine, x: Intersection, t0: Long = 0,
        make: (LatLon, Long) -> VehicleState = { p, t -> state(p, t) },
    ): EngineOutput {
        e.update(make(southOf(x, 60.0), t0))
        return e.update(make(southOf(x, 49.0), t0 + 1000))
    }

    fun dataFile(name: String) = File(System.getProperty("dataDir") ?: "../data", name)
}
