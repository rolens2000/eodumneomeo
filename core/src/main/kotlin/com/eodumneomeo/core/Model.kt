package com.eodumneomeo.core

/** 진입도로: roadId와 교차로 중심에서 그 도로가 뻗은 방위 */
data class Approach(val roadId: String, val bearingDeg: Double)

enum class Risk { LOW, MEDIUM, HIGH, HOTSPOT }

/** 시야 제한 비신호 교차로 DB의 한 항목. HOTSPOT은 사고다발 교차로 */
data class Intersection(
    val id: String,
    val lat: Double,
    val lon: Double,
    val approaches: List<Approach>,
    val signalized: Boolean,
    val risk: Risk,
    val kitInstalled: Boolean,
    val source: String,
) {
    val position get() = LatLon(lat, lon)
}

enum class Gear { P, R, N, D }
enum class TurnSignal { NONE, LEFT, RIGHT }

/** 한 시점의 자차 상태. 위치와 차량 신호를 합친 판단 입력 */
data class VehicleState(
    val timeMs: Long,
    val position: LatLon,
    val headingDeg: Double,
    val speedKph: Double,
    val gear: Gear,
    val headlightOn: Boolean,
    val wiperOn: Boolean,
    val brakePressed: Boolean,
    val turnSignal: TurnSignal,
    val synthetic: Boolean,
)

/** 위치 입력. 실제 구현(FusedLocation)과 합성 구현을 바꿔 끼운다 */
interface LocationSource {
    fun position(timeMs: Long): Pair<LatLon, Double>? // 위치, 진행 방위
}

/** 차량 신호 입력. 실제 구현(Vehicle SDK)과 합성 구현을 바꿔 끼운다 */
interface VehicleSignalSource {
    fun signals(timeMs: Long): VehicleSignals?
}

data class VehicleSignals(
    val speedKph: Double,
    val gear: Gear,
    val headlightOn: Boolean,
    val wiperOn: Boolean,
    val brakePressed: Boolean,
    val turnSignal: TurnSignal,
)

/** 두 입력을 합쳐 판단 입력을 만든다 */
fun combine(timeMs: Long, loc: LocationSource, veh: VehicleSignalSource, synthetic: Boolean): VehicleState? {
    val (p, h) = loc.position(timeMs) ?: return null
    val s = veh.signals(timeMs) ?: return null
    return VehicleState(timeMs, p, h, s.speedKph, s.gear, s.headlightOn, s.wiperOn, s.brakePressed, s.turnSignal, synthetic)
}
