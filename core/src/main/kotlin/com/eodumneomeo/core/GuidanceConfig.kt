package com.eodumneomeo.core

import java.io.StringReader
import java.util.Properties

/** 1단계 안내 규칙의 목표값. data/guidance.properties에서 읽는다 */
data class GuidanceConfig(
    val announceDistanceM: Double = 50.0,
    val minSpeedKph: Double = 20.0,
    val clusterRadiusM: Double = 150.0,
    val aheadConeDeg: Double = 30.0,
    val lookaheadM: Double = 200.0,
    val passedAngleDeg: Double = 90.0,
    val announceText: String = "전방에 시야가 제한된 비신호 교차로가 있습니다. 서행하세요.",
) {
    companion object {
        fun fromProperties(text: String): GuidanceConfig {
            val p = Properties().apply { load(StringReader(text)) }
            val d = GuidanceConfig()
            fun num(k: String, def: Double) = p.getProperty(k)?.trim()?.toDouble() ?: def
            return GuidanceConfig(
                announceDistanceM = num("announce.distanceM", d.announceDistanceM),
                minSpeedKph = num("announce.minSpeedKph", d.minSpeedKph),
                clusterRadiusM = num("cluster.radiusM", d.clusterRadiusM),
                aheadConeDeg = num("ahead.coneDeg", d.aheadConeDeg),
                lookaheadM = num("ahead.lookaheadM", d.lookaheadM),
                passedAngleDeg = num("passed.angleDeg", d.passedAngleDeg),
                announceText = p.getProperty("announce.text")?.trim() ?: d.announceText,
            )
        }
    }
}
