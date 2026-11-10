package com.raj.wavemind

import androidx.annotation.DrawableRes

/**
 * One binaural mode. A binaural beat is NOT a recorded sound: it is the
 * difference between two pure sine tones, one per ear. Left ear hears
 * [leftHz], right ear hears [rightHz]; the brain perceives a beat of
 * (rightHz - leftHz). Headphones / earbuds are therefore mandatory —
 * speakers mix the two tones in the air and the effect is lost.
 *
 * Every mode has a DISTINCT carrier pair — no two modes share the same
 * left/right frequencies, even when the beat number is the same
 * (e.g. Power Nap and Stress Relief are both 6 Hz, but on different
 * carriers; Relax, Calm Mind and Balance are all 10 Hz, on three
 * different carriers). [category] groups modes on the home screen.
 *
 * Honest labels: "Pain Relief" and "Healing" are relaxation-support
 * modes only. Binaural beats are relaxation / entertainment, NOT a
 * medical treatment and NOT a cure for any condition, pain or disease.
 */
data class Mode(
    val id: String,
    val title: String,
    val subtitle: String,
    val leftHz: Double,
    val rightHz: Double,
    val beatHz: Double,
    val defaultMinutes: Int,
    val tag: String,
    val category: String,
    @DrawableRes val thumbRes: Int
) {
    companion object {
        // ---------- Focus ----------
        val FOCUS = Mode(
            id = "focus", title = "Focus Mode",
            subtitle = "Stay sharp & productive",
            leftHz = 200.0, rightHz = 240.0, beatHz = 40.0,
            defaultMinutes = 10, tag = "Gamma 40 Hz", category = "Focus",
            thumbRes = R.drawable.thumb_focus
        )
        val ENERGY = Mode(
            id = "energy", title = "Energy",
            subtitle = "Boost alertness & drive",
            leftHz = 200.0, rightHz = 218.0, beatHz = 18.0,
            defaultMinutes = 15, tag = "Beta 18 Hz", category = "Focus",
            thumbRes = R.drawable.thumb_energy
        )

        // ---------- Sleep ----------
        val DEEP_SLEEP = Mode(
            id = "sleep", title = "Deep Sleep",
            subtitle = "Fall asleep faster, sleep deeper",
            leftHz = 100.0, rightHz = 103.0, beatHz = 3.0,
            defaultMinutes = 30, tag = "Delta 3 Hz", category = "Sleep",
            thumbRes = R.drawable.thumb_sleep
        )
        // Backwards-compatible alias — old code refers to Mode.SLEEP.
        val SLEEP: Mode get() = DEEP_SLEEP
        val POWER_NAP = Mode(
            id = "power-nap", title = "Power Nap",
            subtitle = "Short rest, wake up fresh",
            leftHz = 200.0, rightHz = 206.0, beatHz = 6.0,
            defaultMinutes = 20, tag = "Theta 6 Hz", category = "Sleep",
            thumbRes = R.drawable.thumb_power_nap
        )
        val RESTORE = Mode(
            id = "restore", title = "Restore",
            subtitle = "Rest & recharge",
            leftHz = 100.0, rightHz = 104.0, beatHz = 4.0,
            defaultMinutes = 20, tag = "Delta 4 Hz", category = "Sleep",
            thumbRes = R.drawable.thumb_restore
        )

        // ---------- Relax ----------
        val RELAX = Mode(
            id = "relax", title = "Relax Mode",
            subtitle = "Calm your mind",
            leftHz = 400.0, rightHz = 410.0, beatHz = 10.0,
            defaultMinutes = 15, tag = "Theta 10 Hz", category = "Relax",
            thumbRes = R.drawable.thumb_relax
        )
        val STRESS = Mode(
            id = "stress", title = "Stress Relief Therapy",
            subtitle = "Relaxation support — not a medical treatment",
            leftHz = 150.0, rightHz = 156.0, beatHz = 6.0,
            defaultMinutes = 15, tag = "Theta 6 Hz", category = "Relax",
            thumbRes = R.drawable.thumb_stress
        )
        val CALM = Mode(
            id = "calm", title = "Calm Mind Therapy",
            subtitle = "Slow down & breathe",
            leftHz = 250.0, rightHz = 260.0, beatHz = 10.0,
            defaultMinutes = 15, tag = "Alpha 10 Hz", category = "Relax",
            thumbRes = R.drawable.thumb_calm
        )
        val BALANCE = Mode(
            id = "balance", title = "Balance",
            subtitle = "Steady mind & body",
            leftHz = 300.0, rightHz = 310.0, beatHz = 10.0,
            defaultMinutes = 15, tag = "Alpha 10 Hz", category = "Relax",
            thumbRes = R.drawable.thumb_balance
        )

        // ---------- Relief ----------
        // Relaxation support only — not a treatment or cure.
        val PAIN_RELIEF = Mode(
            id = "pain-relief", title = "Pain Relief",
            subtitle = "Relaxation support for discomfort — not a medical treatment",
            leftHz = 100.0, rightHz = 102.0, beatHz = 2.0,
            defaultMinutes = 20, tag = "Delta 2 Hz", category = "Relief",
            thumbRes = R.drawable.thumb_pain_relief
        )
        val HEALING = Mode(
            id = "healing", title = "Healing",
            subtitle = "Deep relaxation — not a cure for any condition",
            leftHz = 120.0, rightHz = 122.5, beatHz = 2.5,
            defaultMinutes = 20, tag = "Delta 2.5 Hz", category = "Relief",
            thumbRes = R.drawable.thumb_healing
        )

        val ALL = listOf(
            FOCUS, ENERGY,
            DEEP_SLEEP, POWER_NAP, RESTORE,
            RELAX, STRESS, CALM, BALANCE,
            PAIN_RELIEF, HEALING
        )

        val CATEGORIES = listOf("Focus", "Sleep", "Relax", "Relief")

        fun byId(id: String): Mode = ALL.firstOrNull { it.id == id } ?: DEEP_SLEEP
    }
}
