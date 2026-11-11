package com.eodumneomeo.core

/** 외부 라이브러리 없이 DB를 읽기 위한 최소 JSON 파서 */
internal class MiniJson(private val s: String) {
    private var i = 0

    fun parse(): Any? { ws(); val v = value(); ws(); require(i == s.length) { "JSON 끝에 남는 내용: $i" }; return v }

    private fun value(): Any? = when (s[i]) {
        '{' -> obj()
        '[' -> arr()
        '"' -> str()
        't' -> lit("true", true)
        'f' -> lit("false", false)
        'n' -> lit("null", null)
        else -> num()
    }

    private fun lit(word: String, v: Any?): Any? {
        require(s.startsWith(word, i)) { "알 수 없는 값: $i" }; i += word.length; return v
    }

    private fun obj(): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>(); i++; ws()
        if (s[i] == '}') { i++; return m }
        while (true) {
            ws(); val k = str(); ws(); expect(':'); ws(); m[k] = value(); ws()
            if (s[i] == ',') { i++; continue }
            expect('}'); return m
        }
    }

    private fun arr(): List<Any?> {
        val l = ArrayList<Any?>(); i++; ws()
        if (s[i] == ']') { i++; return l }
        while (true) {
            ws(); l += value(); ws()
            if (s[i] == ',') { i++; continue }
            expect(']'); return l
        }
    }

    private fun str(): String {
        expect('"'); val b = StringBuilder()
        while (s[i] != '"') {
            val c = s[i++]
            if (c != '\\') { b.append(c); continue }
            when (val e = s[i++]) {
                'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r')
                'u' -> { b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                else -> b.append(e)
            }
        }
        i++; return b.toString()
    }

    private fun num(): Double {
        val st = i
        while (i < s.length && s[i] in "+-.eE0123456789") i++
        return s.substring(st, i).toDouble()
    }

    private fun expect(c: Char) { require(s[i] == c) { "'$c' 필요: $i" }; i++ }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
}

object IntersectionDb {
    @Suppress("UNCHECKED_CAST")
    fun parse(json: String): List<Intersection> {
        val root = MiniJson(json).parse() as Map<String, Any?>
        return (root["intersections"] as List<Map<String, Any?>>).map { m ->
            Intersection(
                id = m["id"] as String,
                lat = m["lat"] as Double,
                lon = m["lon"] as Double,
                approaches = (m["approaches"] as List<Map<String, Any?>>).map {
                    Approach(it["roadId"] as String, it["bearingDeg"] as Double)
                },
                signalized = m["signalized"] as Boolean,
                risk = Risk.valueOf(m["risk"] as String),
                kitInstalled = m["kitInstalled"] as Boolean,
                source = m["source"] as String,
            )
        }
    }
}

/** 시나리오 CSV 한 줄 = 키프레임. 사이 값은 선형 보간한다 */
data class Keyframe(val tMs: Long, val pos: LatLon, val signals: VehicleSignals)

/** 시나리오 파일을 위치·차량 신호 두 입력으로 나눠 제공하는 합성 입력 */
class Scenario(val name: String, private val frames: List<Keyframe>) : LocationSource, VehicleSignalSource {
    val durationMs get() = frames.last().tMs

    override fun position(timeMs: Long): Pair<LatLon, Double>? {
        val (a, b, u) = segment(timeMs) ?: return null
        val p = LatLon(a.pos.lat + (b.pos.lat - a.pos.lat) * u, a.pos.lon + (b.pos.lon - a.pos.lon) * u)
        val heading = if (a.pos == b.pos) headingNear(a) else Geo.bearingDeg(a.pos, b.pos)
        return p to heading
    }

    override fun signals(timeMs: Long): VehicleSignals? {
        val (a, b, u) = segment(timeMs) ?: return null
        return a.signals.copy(speedKph = a.signals.speedKph + (b.signals.speedKph - a.signals.speedKph) * u)
    }

    fun stateAt(timeMs: Long) = combine(timeMs, this, this, synthetic = true)

    private fun segment(t: Long): Triple<Keyframe, Keyframe, Double>? {
        if (t < frames.first().tMs || t > durationMs) return null
        val i = frames.indexOfLast { it.tMs <= t }.coerceAtMost(frames.size - 2)
        val a = frames[i]; val b = frames[i + 1]
        val u = if (b.tMs == a.tMs) 0.0 else (t - a.tMs).toDouble() / (b.tMs - a.tMs)
        return Triple(a, b, u.coerceIn(0.0, 1.0))
    }

    private fun headingNear(k: Keyframe): Double {
        val i = frames.indexOf(k)
        val other = frames.drop(i + 1).firstOrNull { it.pos != k.pos } ?: frames.take(i).lastOrNull { it.pos != k.pos }
        return when {
            other == null -> 0.0
            other.tMs > k.tMs -> Geo.bearingDeg(k.pos, other.pos)
            else -> Geo.bearingDeg(other.pos, k.pos)
        }
    }

    companion object {
        /** 열: t_ms,lat,lon,speed_kph,gear,headlight,wiper,brake,turn. #으로 시작하는 줄은 주석 */
        fun parseCsv(name: String, text: String): Scenario {
            val rows = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            val body = if (rows.first().startsWith("t_ms")) rows.drop(1) else rows
            val frames = body.map { line ->
                val c = line.split(",").map { it.trim() }
                Keyframe(
                    c[0].toLong(), LatLon(c[1].toDouble(), c[2].toDouble()),
                    VehicleSignals(c[3].toDouble(), Gear.valueOf(c[4]), c[5] == "1", c[6] == "1", c[7] == "1", TurnSignal.valueOf(c[8])),
                )
            }
            require(frames.size >= 2) { "키프레임이 2개 이상 필요합니다: $name" }
            return Scenario(name, frames)
        }
    }
}
