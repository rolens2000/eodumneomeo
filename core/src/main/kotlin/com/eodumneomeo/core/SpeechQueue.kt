package com.eodumneomeo.core

/** 실제 음성 출력. 앱에서 TextToSpeech로 구현한다 */
interface SpeechPlayer {
    fun play(key: String, text: String)
    fun stop()
}

/** 한 번에 한 문장만 재생한다. 취소된 안내는 재생 중이면 멈추고, 대기 중이면 지운다 */
class SpeechQueue(private val player: SpeechPlayer) {
    var current: SpeechCommand.Speak? = null
        private set
    private val waiting = ArrayDeque<SpeechCommand.Speak>()

    fun submit(cmd: SpeechCommand) {
        when (cmd) {
            is SpeechCommand.Speak -> if (current == null) play(cmd) else waiting.addLast(cmd)
            is SpeechCommand.Cancel -> {
                waiting.removeAll { it.key == cmd.key }
                if (current?.key == cmd.key) {
                    player.stop(); current = null; next()
                }
            }
        }
    }

    /** 재생이 끝났을 때 앱이 호출한다 */
    fun onDone(key: String) {
        if (current?.key != key) return
        current = null
        next()
    }

    fun clear() { waiting.clear(); if (current != null) { player.stop(); current = null } }

    private fun next() { waiting.removeFirstOrNull()?.let(::play) }
    private fun play(s: SpeechCommand.Speak) { current = s; player.play(s.key, s.text) }
}
