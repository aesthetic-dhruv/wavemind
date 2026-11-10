package com.raj.wavemind

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
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
 *
 * Music Mixer: the background sound is a REAL recording from
 * res/raw/ (see [BackgroundSound]), played looped by a MediaPlayer
 * under the binaural tones. OFF by default (null = pure beat only).
 * The recording plays identically in both ears, so it never
 * disturbs the left/right beat.
 */
class BinauralEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var audioTrack: AudioTrack? = null
    private var volume = 0.45f

    // Music Mixer: OFF by default (null = pure binaural beat only).
    @Volatile
    private var background: BackgroundSound? = null
    private var backgroundVolume = 0.30f
    private var bgPlayer: MediaPlayer? = null
    @Volatile
    private var bgFadeCancel = false

    var onFinished: (() -> Unit)? = null

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    fun getVolume(): Float = volume

    fun setBackground(sound: BackgroundSound?) {
        background = sound
        if (running) startBackgroundPlayer() else stopBackgroundPlayer()
    }

    fun getBackground(): BackgroundSound? = background

    fun setBackgroundVolume(v: Float) {
        backgroundVolume = v.coerceIn(0f, 1f)
        bgPlayer?.setVolume(backgroundVolume, backgroundVolume)
    }

    fun getBackgroundVolume(): Float = backgroundVolume

    fun isRunning(): Boolean = running

    /** (Re)starts the looped background recording, if one is selected. */
    private fun startBackgroundPlayer() {
        stopBackgroundPlayer()
        val sound = background ?: return
        val player = try {
            MediaPlayer.create(context.applicationContext, sound.rawRes)
        } catch (_: Exception) {
            null
        } ?: return
        bgPlayer = player
        player.isLooping = true
        player.setVolume(0f, 0f)
        try {
            player.start()
        } catch (_: Exception) {
            stopBackgroundPlayer()
            return
        }
        // Gentle 2-second fade-in so the recording never starts abruptly.
        bgFadeCancel = false
        val target = backgroundVolume
        Thread({
            for (step in 1..20) {
                if (bgFadeCancel || !running) break
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                    break
                }
                val p = bgPlayer ?: break
                try {
                    val v = target * step / 20f
                    p.setVolume(v, v)
                } catch (_: Exception) {
                    break
                }
            }
        }, "WaveMind-BgFade").also { it.start() }
    }

    private fun stopBackgroundPlayer() {
        bgFadeCancel = true
        val player = bgPlayer
        bgPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {
            }
            try {
                player.release()
            } catch (_: Exception) {
            }
        }
    }

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
            startBackgroundPlayer()

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
                    val left = (sin(phaseL) * 0.5 * fade).coerceIn(-1.0, 1.0)
                    val right = (sin(phaseR) * 0.5 * fade).coerceIn(-1.0, 1.0)
                    buffer[i * 2] = (left * 32767).toInt().toShort()
                    buffer[i * 2 + 1] = (right * 32767).toInt().toShort()
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
        stopBackgroundPlayer()
        try {
            thread?.join(1000)
        } catch (_: InterruptedException) {
        }
        thread = null
    }
}
