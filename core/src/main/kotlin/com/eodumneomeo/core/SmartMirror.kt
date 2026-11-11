package com.eodumneomeo.core

import kotlin.math.abs

// 2단계: 스마트 반사경 키트 연동. 이번 저장소에는 이벤트 형식, 판정, 합성 생성기만 둔다.

enum class ApproachState { APPROACHING, RECEDING, NONE }
enum class SensorState { OK, FAULT }
enum class Side { LEFT, RIGHT }

/** 키트가 보내는 접근 이벤트. 필드는 docs/event-format.md 참고 */
data class SmartMirrorEvent(
    val intersectionId: String,
    val approachRoadId: String,
    val approachState: ApproachState,
    val detectedAtMs: Long,
    val sensorState: SensorState,
)

/** 키트 이벤트 수신. 실제 통신 구현은 이 저장소 범위 밖이다 */
interface SmartMirrorEventSource {
    fun setListener(listener: (SmartMirrorEvent) -> Unit)
    fun start()
    fun stop()
}

object DirectionJudge {
    /** 각도차 = 접근도로 방위 − 자차 진행 방위, -180 초과 180 이하 */
    fun angleDiff(roadBearingDeg: Double, headingDeg: Double) = Geo.normalizeDeg(roadBearingDeg - headingDeg)

    /** −135~−45°는 왼쪽, 45~135°는 오른쪽, 경계 포함. 나머지는 판정하지 않는다 */
    fun side(diffDeg: Double): Side? = when {
        diffDeg >= -135.0 && diffDeg <= -45.0 -> Side.LEFT
        diffDeg >= 45.0 && diffDeg <= 135.0 -> Side.RIGHT
        else -> null
    }

    /** 자차가 들어오는 도로: 진행 방위의 반대쪽에 가장 가까운 진입도로 */
    fun egoApproachRoadId(x: Intersection, headingDeg: Double): String? =
        x.approaches.minByOrNull { abs(Geo.normalizeDeg(it.bearingDeg - (headingDeg + 180.0))) }?.roadId

    /** 이벤트를 운전자 기준 좌우로 바꾼다. 같은 도로, 접근 중이 아님, 센서 이상이면 null */
    fun judge(x: Intersection, headingDeg: Double, e: SmartMirrorEvent): Side? {
        if (e.intersectionId != x.id) return null
        if (e.sensorState != SensorState.OK || e.approachState != ApproachState.APPROACHING) return null
        val road = x.approaches.firstOrNull { it.roadId == e.approachRoadId } ?: return null
        if (road.roadId == egoApproachRoadId(x, headingDeg)) return null
        return side(angleDiff(road.bearingDeg, headingDeg))
    }
}

/** 시험용 합성 이벤트 생성기. 실제 키트 신호가 아니다 */
object SyntheticEventGenerator {
    fun approaching(intersectionId: String, roadId: String, atMs: Long) =
        SmartMirrorEvent(intersectionId, roadId, ApproachState.APPROACHING, atMs, SensorState.OK)

    fun receding(intersectionId: String, roadId: String, atMs: Long) =
        SmartMirrorEvent(intersectionId, roadId, ApproachState.RECEDING, atMs, SensorState.OK)

    fun fault(intersectionId: String, atMs: Long) =
        SmartMirrorEvent(intersectionId, "", ApproachState.NONE, atMs, SensorState.FAULT)
}
