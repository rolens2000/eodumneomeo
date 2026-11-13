package com.eodumneomeo.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import com.eodumneomeo.core.EngineOutput
import com.eodumneomeo.core.GuidanceConfig
import com.eodumneomeo.core.GuidanceEngine
import com.eodumneomeo.core.Intersection
import com.eodumneomeo.core.IntersectionDb
import com.eodumneomeo.core.Result
import com.eodumneomeo.core.Scenario
import com.eodumneomeo.core.SpeechPlayer
import com.eodumneomeo.core.SpeechQueue
import com.eodumneomeo.core.TurnSignal
import com.eodumneomeo.core.VehicleState

/**
 * 시나리오 파일의 합성 입력으로 1단계 안내를 재현하는 화면.
 * 음성은 Pleos TextToSpeech SDK(ON_DEVICE)로 출력하고, 초기화나 재생에 실패하면 화면으로만 안내한다.
 * 위치·차량 신호는 아직 합성 입력이며 core의 LocationSource·VehicleSignalSource 구현으로 교체한다.
 */
class MainActivity : Activity(), SpeechPlayer {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var tts: TextToSpeech
    private val queue = SpeechQueue(this)
    private lateinit var db: List<Intersection>
    private lateinit var cfg: GuidanceConfig
    private var engine: GuidanceEngine? = null
    private var scenario: Scenario? = null
    private var startedAt = 0L

    private lateinit var status: TextView
    private lateinit var signals: TextView
    private lateinit var log: TextView
    private lateinit var voice: TextView
    private var ttsReady = false
    private var speakingKey: String? = null

    private val ttsListener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() { finishSpeech() }
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) {
            ui.post { voice.text = "Pleos 음성 오류($errorType)" }
            finishSpeech()
        }
    }

    private fun finishSpeech() {
        ui.post { speakingKey?.let { k -> speakingKey = null; queue.onDone(k) } }
    }

    private val tick = object : Runnable {
        override fun run() {
            val sc = scenario ?: return
            val e = engine ?: return
            val t = SystemClock.elapsedRealtime() - startedAt
            val s = sc.stateAt(t)
            if (s == null) { status.text = "시나리오 종료"; return }
            render(s, e.update(s))
            ui.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = IntersectionDb.parse(asset("intersections.sample.json"))
        cfg = GuidanceConfig.fromProperties(asset("guidance.properties"))
        setContentView(buildLayout())
        // Pleos TextToSpeech SDK. ON_DEVICE 모드는 네트워크와 앱 등록 없이 동작한다.
        // SDK는 initialize() 다음에 리스너를 등록해야 한다. 실패하면 화면으로만 안내한다.
        try {
            tts = TextToSpeech(applicationContext, Mode.ON_DEVICE)
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    ui.post {
                        try {
                            tts.addEventListener(ttsListener)
                            ttsReady = true
                            voice.text = "Pleos TextToSpeech SDK 연결됨 (ON_DEVICE)"
                        } catch (e: Exception) {
                            voice.text = "Pleos TextToSpeech SDK 리스너 등록 실패 · 화면으로만 안내"
                        }
                    }
                }
                override fun onFailure(errorType: ErrorType) {
                    ui.post { voice.text = "Pleos TextToSpeech SDK 초기화 실패($errorType) · 화면으로만 안내" }
                }
            })
        } catch (e: Exception) {
            voice.text = "Pleos TextToSpeech SDK 사용 불가 · 화면으로만 안내"
        }
    }

    private fun asset(name: String) = assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun text(size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        textSize = size; setTextColor(color); setPadding(32, 12, 32, 12)
    }

    private fun buildLayout() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(16, 24, 43))
        addView(text(22f, Color.BLACK).apply {
            text = "합성 입력 · 실제 차량·센서 신호 아님"
            setBackgroundColor(Color.rgb(255, 184, 28)); gravity = Gravity.CENTER
        })
        addView(text(30f).apply { text = "어둠너머" })
        status = text(40f).apply { text = "시나리오를 선택하세요" }
        signals = text(20f, Color.rgb(169, 180, 200))
        log = text(20f)
        voice = text(22f, Color.rgb(255, 184, 28)).apply { text = "음성 엔진 연결 중" }
        addView(status); addView(signals); addView(voice); addView(log)
        val row = LinearLayout(this@MainActivity)
        assets.list("scenarios").orEmpty().sorted().forEach { f ->
            row.addView(Button(this@MainActivity).apply { text = f.removeSuffix(".csv"); setOnClickListener { start(f) } })
        }
        addView(HorizontalScrollView(this@MainActivity).apply { addView(row) })
    }

    private fun start(file: String) {
        ui.removeCallbacks(tick)
        queue.clear()
        scenario = Scenario.parseCsv(file, asset("scenarios/$file"))
        engine = GuidanceEngine(db, cfg)
        log.text = "시나리오: ${file.removeSuffix(".csv")}"
        startedAt = SystemClock.elapsedRealtime()
        ui.post(tick)
    }

    private fun render(s: VehicleState, out: EngineOutput) {
        out.speech.forEach(queue::submit)
        out.decisions.forEach { d ->
            log.append("\n${d.intersectionId}: ${label(d.result)} (${d.distanceM.toInt()}m)")
        }
        status.text = out.nearestAhead?.let { "${it.id} 앞 ${out.nearestAheadDistanceM!!.toInt()}m" } ?: "앞쪽 등록 교차로 없음"
        signals.text = "시속 ${s.speedKph.toInt()}km · 기어 ${s.gear} · 전조등 ${onOff(s.headlightOn)} · 와이퍼 ${onOff(s.wiperOn)}" +
            " · 브레이크 ${onOff(s.brakePressed)} · 방향지시등 ${turn(s.turnSignal)}"
    }

    private fun label(r: Result) = when (r) {
        Result.ANNOUNCE -> "안내"
        Result.SKIP_BRAKE -> "생략(브레이크)"
        Result.SKIP_CONDITION -> "생략(주간·맑음)"
        Result.SKIP_SPEED -> "생략(저속)"
        Result.SKIP_CLUSTERED -> "묶음 안내에 포함"
    }

    private fun onOff(b: Boolean) = if (b) "켜짐" else "꺼짐"
    private fun turn(t: TurnSignal) = when (t) { TurnSignal.NONE -> "꺼짐"; TurnSignal.LEFT -> "왼쪽"; TurnSignal.RIGHT -> "오른쪽" }

    override fun play(key: String, text: String) {
        val spoken = ttsReady && try { tts.speak(text); true } catch (e: Exception) { false }
        if (spoken) {
            speakingKey = key
            voice.text = "Pleos TTS 음성 안내: $text"
        } else {
            voice.text = "화면 안내(음성 출력 실패): $text"
            ui.post { queue.onDone(key) }
        }
    }

    override fun stop() {
        if (speakingKey != null) { speakingKey = null; try { tts.stop() } catch (e: Exception) {} }
    }

    override fun onDestroy() { ui.removeCallbacks(tick); try { if (ttsReady) tts.removeEventListener(ttsListener); tts.release() } catch (e: Exception) {}; super.onDestroy() }
}
