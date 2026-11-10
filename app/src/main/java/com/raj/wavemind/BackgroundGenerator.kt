package com.raj.wavemind

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Live generator for one background (mixer) sound. One sample at a
 * time, mono — the same sample goes to both ears, so the binaural
 * beat (left vs right tone) is never disturbed.
 *
 * All of this is approximation, documented honestly in
 * [BackgroundSound]: coloured noise is filtered random noise, nature
 * is modulated/filtered noise with occasional generated events, and
 * instruments are sine-harmonic arpeggios, not recordings.
 */
class BackgroundGenerator {

    private var kind: GenKind? = null
    private val random = Random.Default
    private var sampleCount: Long = 0L

    // --- coloured-noise filter state ---
    private var pinkB0 = 0.0; private var pinkB1 = 0.0; private var pinkB2 = 0.0
    private var pinkB3 = 0.0; private var pinkB4 = 0.0; private var pinkB5 = 0.0
    private var pinkB6 = 0.0
    private var brownOut = 0.0
    private var greenLp1 = 0.0; private var greenLp2 = 0.0
    private var greyLp = 0.0
    private var rainLp = 0.0
    private var oceanLp = 0.0
    private var forestLp = 0.0
    private var windLp = 0.0; private var windMod = 0.5; private var windTarget = 0.5
    private var windTargetAt: Long = 0L
    private var fireLp = 0.0

    // --- one-shot events (droplets / chirps / crackles) ---
    private var eventAt: Long = 0L
    private var eventFreq = 1000.0
    private var eventPhase = 0.0
    private var eventAmp = 0.0
    private var eventDecay = 0.999

    // --- music arpeggio state ---
    // A minor pentatonic, slow and calm.
    private val scale = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.0, 440.0, 523.25)
    private val pattern = intArrayOf(0, 2, 4, 6, 4, 2, 1, 3)
    private var noteIndex = 0
    private var noteStart: Long = 0L
    private var noteFreq = 220.0
    private var notePhase = 0.0

    fun setKind(newKind: GenKind?) {
        if (kind == newKind) return
        kind = newKind
        sampleCount = 0L
        pinkB0 = 0.0; pinkB1 = 0.0; pinkB2 = 0.0; pinkB3 = 0.0
        pinkB4 = 0.0; pinkB5 = 0.0; pinkB6 = 0.0
        brownOut = 0.0; greenLp1 = 0.0; greenLp2 = 0.0; greyLp = 0.0
        rainLp = 0.0; oceanLp = 0.0; forestLp = 0.0
        windLp = 0.0; windMod = 0.5; windTarget = 0.5; windTargetAt = 0L
        fireLp = 0.0
        eventAt = 0L; eventAmp = 0.0
        noteIndex = 0; noteStart = 0L; noteFreq = scale[0]; notePhase = 0.0
    }

    /** One mono sample in [-1, 1]. Call once per frame, in order. */
    fun nextSample(sampleRate: Int): Double {
        val k = kind ?: return 0.0
        val t = sampleCount.toDouble() / sampleRate
        sampleCount++
        val w = random.nextDouble() * 2.0 - 1.0 // white noise, raw
        return when (k) {
            GenKind.WHITE -> w * 0.5
            GenKind.PINK -> pink(w)
            GenKind.BROWN -> brown(w)
            GenKind.GREEN -> green(w)
            GenKind.GREY -> grey(w)
            GenKind.RAIN -> rain(w, sampleRate)
            GenKind.THUNDER -> thunder(w, t)
            GenKind.OCEAN -> ocean(w, t, sampleRate)
            GenKind.FOREST -> forest(w, sampleRate)
            GenKind.WIND -> wind(w, sampleRate)
            GenKind.FIREPLACE -> fireplace(w)
            GenKind.GUITAR, GenKind.PIANO, GenKind.FLUTE,
            GenKind.VIOLIN, GenKind.HARP -> instrument(k, t, sampleRate)
        }.coerceIn(-1.0, 1.0)
    }

    // ---------- coloured noise ----------

    // Paul Kellet 7-pole pink-noise approximation.
    private fun pink(w: Double): Double {
        pinkB0 = 0.99886 * pinkB0 + w * 0.0555179
        pinkB1 = 0.99332 * pinkB1 + w * 0.0750759
        pinkB2 = 0.96900 * pinkB2 + w * 0.1538520
        pinkB3 = 0.86650 * pinkB3 + w * 0.3104856
        pinkB4 = 0.55000 * pinkB4 + w * 0.5329522
        pinkB5 = -0.7616 * pinkB5 - w * 0.0168980
        val out = pinkB0 + pinkB1 + pinkB2 + pinkB3 + pinkB4 + pinkB5 + pinkB6 + w * 0.5362
        pinkB6 = w * 0.115926
        return out * 0.11
    }

    private fun brown(w: Double): Double {
        brownOut = (brownOut + 0.02 * w) / 1.02
        return (brownOut * 3.5).coerceIn(-1.0, 1.0)
    }

    // Green noise ~ mid-band: low-pass minus a deeper low-pass.
    private fun green(w: Double): Double {
        greenLp1 += 0.20 * (w - greenLp1)
        greenLp2 += 0.02 * (w - greenLp2)
        return ((greenLp1 - greenLp2) * 2.5).coerceIn(-1.0, 1.0)
    }

    // Grey noise ~ balanced: soft low-pass plus a little raw hiss.
    private fun grey(w: Double): Double {
        greyLp += 0.05 * (w - greyLp)
        return (greyLp * 0.7 + w * 0.3) * 0.6
    }

    // ---------- nature (filtered / modulated noise + events) ----------

    private fun rain(w: Double, sampleRate: Int): Double {
        rainLp += 0.02 * (w - rainLp)
        val hiss = (w - rainLp) * 0.6 // high-passed noise
        return hiss + droplet(sampleRate, 800.0, 3000.0, 0.25, 2000L, 12000L, 0.9995)
    }

    private fun thunder(w: Double, t: Double): Double {
        // Deep rumble, swelling slowly like distant thunder.
        val lfo = 0.5 + 0.5 * sin(2.0 * PI * 0.05 * t)
        return brown(w) * (0.3 + 0.7 * lfo) * 0.9
    }

    private fun ocean(w: Double, t: Double, sampleRate: Int): Double {
        oceanLp += 0.05 * (w - oceanLp)
        val swell = 0.5 + 0.5 * sin(2.0 * PI * 0.08 * t)
        return oceanLp * 2.0 * swell + (w - oceanLp) * 0.12 * swell
    }

    private fun forest(w: Double, sampleRate: Int): Double {
        forestLp += 0.01 * (w - forestLp)
        // Occasional distant bird chirp: short high sine burst.
        return forestLp * 2.0 + droplet(sampleRate, 2500.0, 4200.0, 0.12, 88200L, 264600L, 0.9992)
    }

    private fun wind(w: Double, sampleRate: Int): Double {
        windLp += 0.005 * (w - windLp)
        if (sampleCount - windTargetAt > sampleRate) {
            windTarget = 0.2 + random.nextDouble() * 0.8
            windTargetAt = sampleCount
        }
        windMod += (windTarget - windMod) * 0.001
        return windLp * 3.0 * (0.2 + 0.8 * windMod)
    }

    private fun fireplace(w: Double): Double {
        fireLp += 0.08 * (w - fireLp)
        // Crackle: random short pops on top of a warm low rumble.
        if (eventAmp < 0.01 && random.nextDouble() < 0.0015) {
            eventAmp = 0.4 + random.nextDouble() * 0.3
            eventPhase = 0.0
            eventFreq = 1200.0 + random.nextDouble() * 2000.0
            eventDecay = 0.995
        }
        var pop = 0.0
        if (eventAmp > 0.01) {
            pop = sin(eventPhase) * eventAmp
            eventPhase += 2.0 * PI * eventFreq / BinauralEngine.SAMPLE_RATE
            eventAmp *= eventDecay
        }
        return fireLp * 0.8 + pop
    }

    // Short decaying sine burst, triggered at random intervals.
    private fun droplet(
        sampleRate: Int, minFreq: Double, maxFreq: Double,
        amp: Double, minGap: Long, maxGap: Long, decay: Double
    ): Double {
        if (sampleCount >= eventAt) {
            eventAt = sampleCount + minGap + (random.nextLong(maxGap - minGap))
            eventFreq = minFreq + random.nextDouble() * (maxFreq - minFreq)
            eventPhase = 0.0
            eventAmp = amp
            eventDecay = decay
        }
        if (eventAmp < 0.005) return 0.0
        val out = sin(eventPhase) * eventAmp
        eventPhase += 2.0 * PI * eventFreq / sampleRate
        eventAmp *= eventDecay
        return out
    }

    // ---------- instruments (sine-harmonic arpeggio) ----------

    private fun instrument(kind: GenKind, t: Double, sampleRate: Int): Double {
        val noteDurSec = when (kind) {
            GenKind.HARP -> 0.4
            GenKind.GUITAR -> 0.5
            GenKind.PIANO -> 0.7
            GenKind.FLUTE, GenKind.VIOLIN -> 0.9
            else -> 0.6
        }
        val noteDurSamples = (noteDurSec * sampleRate).toLong()
        if (sampleCount - noteStart >= noteDurSamples) {
            noteIndex++
            noteFreq = scale[pattern[noteIndex % pattern.size]]
            noteStart = sampleCount
            notePhase = 0.0
        }
        val noteT = (sampleCount - noteStart).toDouble() / sampleRate
        val progress = (sampleCount - noteStart).toDouble() / noteDurSamples

        // Vibrato only for flute / violin.
        val vibrato = when (kind) {
            GenKind.FLUTE, GenKind.VIOLIN ->
                1.0 + 0.005 * sin(2.0 * PI * 5.0 * t)
            else -> 1.0
        }
        notePhase += 2.0 * PI * noteFreq * vibrato / sampleRate

        // Harmonic mix per instrument: (harmonic number, strength).
        val harmonics = when (kind) {
            GenKind.GUITAR -> doubleArrayOf(1.0, 0.5, 0.25)
            GenKind.PIANO -> doubleArrayOf(1.0, 0.6, 0.3, 0.15)
            GenKind.FLUTE -> doubleArrayOf(1.0, 0.1)
            GenKind.VIOLIN -> doubleArrayOf(1.0, 0.7, 0.5, 0.3)
            GenKind.HARP -> doubleArrayOf(1.0, 0.4, 0.2)
            else -> doubleArrayOf(1.0)
        }
        var tone = 0.0
        for (h in harmonics.indices) {
            tone += sin(notePhase * (h + 1)) * harmonics[h]
        }
        tone /= harmonics.sum()

        val envelope = when (kind) {
            GenKind.GUITAR -> exp(-6.0 * noteT) * (1.0 - exp(-80.0 * noteT))
            GenKind.PIANO -> exp(-3.5 * noteT)
            GenKind.FLUTE -> minOf(1.0, noteT * 4.0) * (0.8 + 0.2 * sin(2.0 * PI * 5.0 * t))
            GenKind.VIOLIN -> minOf(1.0, noteT * 3.0) * (0.9 - 0.2 * progress)
            GenKind.HARP -> exp(-8.0 * noteT)
            else -> 1.0
        }
        return tone * envelope * 0.5
    }
}
