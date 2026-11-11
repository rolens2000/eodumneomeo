package com.eodumneomeo.core

import kotlin.math.abs

enum class Result { ANNOUNCE, SKIP_BRAKE, SKIP_CONDITION, SKIP_SPEED, SKIP_CLUSTERED }

data class Decision(val intersectionId: String, val result: Result, val distanceM: Double, val timeMs: Long)

sealed interface SpeechCommand {
    val key: String
    data class Speak(override val key: String, val text: String, val intersectionIds: List<String>) : SpeechCommand
    data class Cancel(override val key: String) : SpeechCommand
}

data class EngineOutput(
    val decisions: List<Decision>,
    val speech: List<SpeechCommand>,
    val nearestAhead: Intersection?,
    val nearestAheadDistanceM: Double?,
)

object Ahead {
    /** 교차로가 자차 진행 방향 앞(원뿔 안, 탐색 거리 안)에 있는지 */
    fun isAhead(s: VehicleState, x: Intersection, cfg: GuidanceConfig): Boolean {
        val d = Geo.distanceM(s.position, x.position)
        if (d > cfg.lookaheadM) return false
        val diff = Geo.normalizeDeg(Geo.bearingDeg(s.position, x.position) - s.headingDeg)
        return abs(diff) <= cfg.aheadConeDeg
    }

    fun isPassed(s: VehicleState, x: Intersection, cfg: GuidanceConfig): Boolean {
        val diff = Geo.normalizeDeg(Geo.bearingDeg(s.position, x.position) - s.headingDeg)
        return abs(diff) > cfg.passedAngleDeg
    }
}

/**
 * 1단계 안내 엔진. 교차로까지 거리가 안내 거리를 처음 넘어서는 순간 한 번 판정한다.
 * 판정 순서: 이미 처리됨 → 브레이크 → 전조등·와이퍼·사고다발 → 속도(방향지시등이면 면제).
 * 신호 교차로는 DB에 있어도 안내하지 않는다.
 */
class GuidanceEngine(db: List<Intersection>, private val cfg: GuidanceConfig = GuidanceConfig()) {
    private val targets = db.filter { !it.signalized }
    private val handled = HashSet<String>()
    private val lastDistance = HashMap<String, Double>()
    private val activeSpeech = LinkedHashMap<String, Intersection>()
    private var lastGear: Gear? = null

    /** 새 주행 시작. 같은 교차로 1회 안내 기록을 지운다 */
    fun startTrip() {
        handled.clear(); lastDistance.clear(); activeSpeech.clear()
    }

    fun update(s: VehicleState): EngineOutput {
        if (s.gear == Gear.P && lastGear != null && lastGear != Gear.P) startTrip()
        lastGear = s.gear

        val decisions = ArrayList<Decision>()
        val speech = ArrayList<SpeechCommand>()

        // 이미 지나친 안내는 취소
        val passed = activeSpeech.filterValues { Ahead.isPassed(s, it, cfg) }.keys.toList()
        passed.forEach { activeSpeech.remove(it); speech += SpeechCommand.Cancel(it) }

        val ahead = targets.filter { Ahead.isAhead(s, it, cfg) }
            .map { it to Geo.distanceM(s.position, it.position) }
            .sortedBy { it.second }

        for ((x, d) in ahead) {
            val prev = lastDistance[x.id]
            lastDistance[x.id] = d
            val crossed = prev != null && prev > cfg.announceDistanceM && d <= cfg.announceDistanceM
            if (!crossed || x.id in handled) continue
            handled += x.id
            val result = judge(s, x)
            decisions += Decision(x.id, result, d, s.timeMs)
            if (result == Result.ANNOUNCE) {
                val cluster = ahead.map { it.first }
                    .filter { it.id != x.id && Geo.distanceM(x.position, it.position) <= cfg.clusterRadiusM }
                cluster.forEach {
                    if (it.id !in handled) {
                        handled += it.id
                        decisions += Decision(it.id, Result.SKIP_CLUSTERED, Geo.distanceM(s.position, it.position), s.timeMs)
                    }
                }
                activeSpeech[x.id] = x
                speech += SpeechCommand.Speak(x.id, cfg.announceText, listOf(x.id) + cluster.map { it.id })
            }
        }
        val nearest = ahead.firstOrNull()
        return EngineOutput(decisions, speech, nearest?.first, nearest?.second)
    }

    /** 처음 넘어선 순간의 규칙 판정 */
    fun judge(s: VehicleState, x: Intersection): Result {
        if (s.brakePressed) return Result.SKIP_BRAKE
        val conditionMet = s.headlightOn || s.wiperOn || x.risk == Risk.HOTSPOT
        if (!conditionMet) return Result.SKIP_CONDITION
        val speedExempt = s.turnSignal != TurnSignal.NONE
        if (!speedExempt && s.speedKph < cfg.minSpeedKph) return Result.SKIP_SPEED
        return Result.ANNOUNCE
    }
}
