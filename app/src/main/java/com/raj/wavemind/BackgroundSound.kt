package com.raj.wavemind

/**
 * Music Mixer — background sounds that can be mixed under ANY binaural
 * beat (Deep Sleep, Focus, any mode). OFF by default: the user hears
 * the pure beat only, exactly as in v1.0/v1.1.
 *
 * These are REAL recordings, not generated tones (v1.2 used live
 * generated ambience; Raj asked for pure copyright-free recordings
 * instead, so v1.2 was reworked to play downloaded files):
 *
 * - Noise: Freesound, newagesoup, CC0 — 10-second loops, generated
 *   in Audition by the uploader (colour noise is by nature a
 *   generated signal, not a field recording; these are the uploader's
 *   files, not tones made by this app). Green Noise: no standalone
 *   CC0 green-noise recording could be found, so it is the same
 *   CC0 white-noise recording mid-band filtered (150–2500 Hz) —
 *   labelled as such below, not presented as a separate recording.
 *   Grey Noise: the Freesound grey file in the app was SILENT
 *   (verified 2026-11-10: 60 s, max amplitude 0.0), so it was
 *   replaced with Wikimedia Commons "Gray noise.ogg" by Omegatron
 *   (10 s inverse A-weighted grey noise, freely licensed on Commons),
 *   looped 6× with 0.3 s crossfades to 58.5 s.
 * - Nature + Music: Mixkit, Mixkit Sound Effects / Stock Music Free
 *   Licence — free to use, no attribution required.
 *   Nature files are Mixkit preview MP3s (real recordings);
 *   Rain (57 s), Thunder (22.7 s) and Ocean (48 s) are the full
 *   previews; Forest (full 2:31) and Wind (full 1:01) are 60 s
 *   excerpts; music files are 60-second excerpts (from 5s in,
 *   2s fade in/out) of calm Mixkit tracks, so they loop cleanly.
 *   All Nature files are Mixkit SOUND EFFECTS (no music in them):
 *   verified 2026-11-10 by source-page descriptions + audio
 *   analysis (0–1 tonal windows per file vs 44–98 for the music
 *   files) — see the verification table in the parent report.
 *
 * Each file lives in res/raw/ and is played looped by a MediaPlayer
 * under the live-generated binaural tones. The binaural tones
 * themselves are still generated live (two sine tones, one per ear)
 * — that part is real generation, not a recording, and is unchanged.
 */
enum class MixerSection(val label: String) {
    NOISE("Noise"),
    NATURE("Nature"),
    MUSIC("Music")
}

data class BackgroundSound(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: MixerSection,
    val rawRes: Int,
    val source: String
) {
    companion object {
        // ---------- Noise — Freesound, newagesoup, CC0 ----------
        val WHITE = BackgroundSound(
            "white", "White Noise", "Real recording — Freesound, CC0",
            MixerSection.NOISE, R.raw.bg_white_noise,
            "Freesound: WHITE_NOISE-10s.wav by newagesoup, CC0"
        )
        val PINK = BackgroundSound(
            "pink", "Pink Noise", "Real recording — Freesound, CC0",
            MixerSection.NOISE, R.raw.bg_pink_noise,
            "Freesound: PINK_NOISE-10s.wav by newagesoup, CC0"
        )
        val BROWN = BackgroundSound(
            "brown", "Brown Noise", "Real recording — Freesound, CC0",
            MixerSection.NOISE, R.raw.bg_brown_noise,
            "Freesound: BROWN_NOISE-10s.wav by newagesoup, CC0"
        )
        val GREEN = BackgroundSound(
            "green", "Green Noise",
            "From the CC0 white-noise recording, mid-band filtered",
            MixerSection.NOISE, R.raw.bg_green_noise,
            "Derived from Freesound WHITE_NOISE-10s.wav by newagesoup, CC0"
        )
        val GREY = BackgroundSound(
            "grey", "Grey Noise",
            "Real grey-noise file — Wikimedia Commons, free licence",
            MixerSection.NOISE, R.raw.bg_grey_noise,
            "Wikimedia Commons: Gray noise.ogg by Omegatron, freely licensed (10 s inverse A-weighted grey noise, looped)"
        )

        // ---------- Nature — Mixkit, free licence ----------
        val RAIN = BackgroundSound(
            "rain", "Rain", "Real rain recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_rain,
            "Mixkit: Rain long loop (sfx 2394), Mixkit Sound Effects Free Licence"
        )
        val THUNDER = BackgroundSound(
            "thunder", "Thunder", "Real thunder recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_thunder,
            "Mixkit: Big thunder rumble (sfx 1297), Mixkit Sound Effects Free Licence"
        )
        val OCEAN = BackgroundSound(
            "ocean", "Ocean Waves", "Real ocean recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_ocean,
            "Mixkit: Sea waves loop (sfx 1196), Mixkit Sound Effects Free Licence"
        )
        val FOREST = BackgroundSound(
            "forest", "Forest", "Real forest recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_forest,
            "Mixkit: Forest birds ambience (sfx 1210), Mixkit Sound Effects Free Licence"
        )
        val WIND = BackgroundSound(
            "wind", "Wind", "Real wind recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_wind,
            "Mixkit: Wind blowing ambience (sfx 2658), Mixkit Sound Effects Free Licence"
        )
        val FIREPLACE = BackgroundSound(
            "fireplace", "Fireplace", "Real fire recording — Mixkit, free licence",
            MixerSection.NATURE, R.raw.bg_fireplace,
            "Mixkit: Campfire crackles (sfx 1330), Mixkit Sound Effects Free Licence"
        )

        // ---------- Music — Mixkit, free licence ----------
        // 60-second excerpts of calm tracks, looped.
        val GUITAR = BackgroundSound(
            "guitar", "Guitar", "Real guitar recording — Mixkit, free licence",
            MixerSection.MUSIC, R.raw.bg_guitar,
            "Mixkit: Spanish Heart by Michael Ramir C., Mixkit Stock Music Free Licence"
        )
        val PIANO = BackgroundSound(
            "piano", "Piano", "Real piano recording — Mixkit, free licence",
            MixerSection.MUSIC, R.raw.bg_piano,
            "Mixkit: Piano Reflections by Ahjay Stelino, Mixkit Stock Music Free Licence"
        )
        val FLUTE = BackgroundSound(
            "flute", "Flute", "Real flute recording — Mixkit, free licence",
            MixerSection.MUSIC, R.raw.bg_flute,
            "Mixkit: Indian Meditations by Ahjay Stelino, Mixkit Stock Music Free Licence"
        )
        val VIOLIN = BackgroundSound(
            "violin", "Violin", "Real violin recording — Mixkit, free licence",
            MixerSection.MUSIC, R.raw.bg_violin,
            "Mixkit: Violin Uplift by Francisco Alvear, Mixkit Stock Music Free Licence"
        )
        val HARP = BackgroundSound(
            "harp", "Harp", "Real harp recording — Mixkit, free licence",
            MixerSection.MUSIC, R.raw.bg_harp,
            "Mixkit: Harp Relax by Francisco Alvear, Mixkit Stock Music Free Licence"
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
