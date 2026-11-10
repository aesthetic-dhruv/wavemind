package com.raj.wavemind

/**
 * Music Mixer — background sounds that can be mixed under ANY binaural
 * beat (Deep Sleep, Focus, any mode). OFF by default: the user hears
 * the pure beat only, exactly as in v1.0/v1.1.
 *
 * EVERYTHING here is generated live in code — there are NO audio files
 * and NO recordings in the app, same as the binaural tones themselves:
 *
 * - Noise: white noise is raw random samples. Pink / brown / green /
 *   grey are filtered random noise (one-pole low-pass / integration
 *   filtering). These are standard approximations, not lab-grade
 *   coloured-noise generators — they sound like the named noise, with
 *   the right rough character (white = hiss, brown = deep rumble,
 *   pink = in-between, green = mid, grey = balanced).
 * - Nature: filtered + slowly modulated noise, with occasional
 *   generated events (rain droplets, ocean swell, forest bird chirps,
 *   fireplace crackles). Labelled in the UI as generated ambience —
 *   they are NOT recordings of real rain / ocean / fire.
 * - Music: soft instrumental tones — a slow pentatonic arpeggio
 *   (A minor) played with sine harmonics. Each "instrument" is only a
 *   different harmonic mix + envelope (guitar = fast-decay harmonics,
 *   piano = slower-decay harmonics, flute = near-pure sine + vibrato,
 *   violin = rich harmonics + slow attack, harp = very fast decay).
 *   These are generated instrumental TONES, not recordings of a real
 *   guitar / piano / flute / violin / harp.
 *
 * The background sound is diotic (same in both ears), so it never
 * disturbs the binaural beat — the beat is still left-tone vs
 * right-tone, the background just sits underneath it at its own
 * volume (default 30%).
 */
enum class MixerSection(val label: String) {
    NOISE("Noise"),
    NATURE("Nature"),
    MUSIC("Music")
}

enum class GenKind {
    WHITE, PINK, BROWN, GREEN, GREY,
    RAIN, THUNDER, OCEAN, FOREST, WIND, FIREPLACE,
    GUITAR, PIANO, FLUTE, VIOLIN, HARP
}

data class BackgroundSound(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: MixerSection,
    val kind: GenKind
) {
    companion object {
        val WHITE = BackgroundSound(
            "white", "White Noise", "Even hiss — masks other sounds",
            MixerSection.NOISE, GenKind.WHITE
        )
        val PINK = BackgroundSound(
            "pink", "Pink Noise", "Softer hiss — deep sleep favourite",
            MixerSection.NOISE, GenKind.PINK
        )
        val BROWN = BackgroundSound(
            "brown", "Brown Noise", "Deep low rumble",
            MixerSection.NOISE, GenKind.BROWN
        )
        val GREEN = BackgroundSound(
            "green", "Green Noise", "Mid, gentle — like a fan",
            MixerSection.NOISE, GenKind.GREEN
        )
        val GREY = BackgroundSound(
            "grey", "Grey Noise", "Balanced across frequencies",
            MixerSection.NOISE, GenKind.GREY
        )

        val RAIN = BackgroundSound(
            "rain", "Rain", "Soft rain — generated ambience",
            MixerSection.NATURE, GenKind.RAIN
        )
        val THUNDER = BackgroundSound(
            "thunder", "Thunder", "Low distant rumble",
            MixerSection.NATURE, GenKind.THUNDER
        )
        val OCEAN = BackgroundSound(
            "ocean", "Ocean Waves", "Slow waves rolling in",
            MixerSection.NATURE, GenKind.OCEAN
        )
        val FOREST = BackgroundSound(
            "forest", "Forest", "Quiet forest, birds far away",
            MixerSection.NATURE, GenKind.FOREST
        )
        val WIND = BackgroundSound(
            "wind", "Wind", "Gentle wind blowing",
            MixerSection.NATURE, GenKind.WIND
        )
        val FIREPLACE = BackgroundSound(
            "fireplace", "Fireplace", "Warm fire, soft crackle",
            MixerSection.NATURE, GenKind.FIREPLACE
        )

        val GUITAR = BackgroundSound(
            "guitar", "Guitar", "Soft guitar tones — generated, not a recording",
            MixerSection.MUSIC, GenKind.GUITAR
        )
        val PIANO = BackgroundSound(
            "piano", "Piano", "Soft piano tones — generated, not a recording",
            MixerSection.MUSIC, GenKind.PIANO
        )
        val FLUTE = BackgroundSound(
            "flute", "Flute", "Soft flute tones — generated, not a recording",
            MixerSection.MUSIC, GenKind.FLUTE
        )
        val VIOLIN = BackgroundSound(
            "violin", "Violin", "Soft violin tones — generated, not a recording",
            MixerSection.MUSIC, GenKind.VIOLIN
        )
        val HARP = BackgroundSound(
            "harp", "Harp", "Soft harp tones — generated, not a recording",
            MixerSection.MUSIC, GenKind.HARP
        )

        val ALL = listOf(
            WHITE, PINK, BROWN, GREEN, GREY,
            RAIN, THUNDER, OCEAN, FOREST, WIND, FIREPLACE,
            GUITAR, PIANO, FLUTE, VIOLIN, HARP
        )

        val SECTIONS = listOf(
            MixerSection.NOISE, MixerSection.NATURE, MixerSection.MUSIC
        )

        fun byId(id: String?): BackgroundSound? =
            ALL.firstOrNull { it.id == id }
    }
}
