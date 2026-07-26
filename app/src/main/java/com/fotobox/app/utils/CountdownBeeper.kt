package com.fotobox.app.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object CountdownBeeper {

    fun beep() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    fun beepFinal() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 400)
    }

    private fun playTone(tone: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(tone, durationMs)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, (durationMs + 100).toLong())
        } catch (_: Exception) {}
    }
}
