package com.raj.wavemind

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * REAL binaural generation — no audio files, no recordings, no loops.
 *
 * Generates two pure sine tones live with AudioTrack in stereo:
 * left channel = [mode.leftHz], right channel = [mode.rightHz].
 * The beat the listener perceives is (rightHz - leftHz).
 *
 * Runs on its own thread so playback continues while the UI is idle,
 * and stops cleanly on [stop]. Volume is 0.0–1.0 (default 0.45 —
 * binaural beats should be barely audible, never loud).
 */
class BinauralEngine {

    companion object {
        const val SAMPLE_RATE = 44100
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var audioTrack: AudioTrack? = null
    private var volume = 0.45f

    var onFinished: (() -> Unit)? = null

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    fun getVolume(): Float = volume

    fun isRunning(): Boolean = running

    fun start(mode: Mode) {
        stop()
        running = true
        thread = Thread({
            val minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBuffer * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack = track
            track.setVolume(volume)
            track.play()

            // Interleaved stereo PCM: L, R, L, R ...
            val frameCount = bufferSize / 4 // 2 channels x 2 bytes
            val buffer = ShortArray(frameCount * 2)
            var phaseL = 0.0
            var phaseR = 0.0
            val stepL = 2.0 * PI * mode.leftHz / SAMPLE_RATE
            val stepR = 2.0 * PI * mode.rightHz / SAMPLE_RATE
            // Gentle fade-in over the first ~2 seconds to avoid a click.
            var fade = 0.0
            val fadeStep = 1.0 / (SAMPLE_RATE * 2)

            while (running) {
                for (i in 0 until frameCount) {
                    if (fade < 1.0) fade = (fade + fadeStep).coerceAtMost(1.0)
                    val amp = (0.5 * 32767 * fade).toInt()
                    buffer[i * 2] = (sin(phaseL) * amp).toInt().toShort()
                    buffer[i * 2 + 1] = (sin(phaseR) * amp).toInt().toShort()
                    phaseL += stepL
                    phaseR += stepR
                    if (phaseL > 2.0 * PI) phaseL -= 2.0 * PI
                    if (phaseR > 2.0 * PI) phaseR -= 2.0 * PI
                }
                track.write(buffer, 0, buffer.size)
            }
            track.stop()
            track.release()
            audioTrack = null
        }, "WaveMind-Binaural").also { it.start() }
    }

    fun stop() {
        running = false
        try {
            thread?.join(1000)
        } catch (_: InterruptedException) {
        }
        thread = null
    }
}
