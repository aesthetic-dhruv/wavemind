# WaveMind

Raj's own binaural beats app — **real binaural generation, not pre-recorded audio**.

Two pure sine tones are generated live with Android `AudioTrack` (stereo):
left ear hears one frequency, right ear hears a slightly different one,
and the brain perceives the difference as the beat. Headphones / earbuds
are mandatory — speakers mix the two tones and the effect is lost.

## Categories & modes — 11 modes, all real, all distinct

Every mode has a **distinct left/right carrier pair** — no two modes
share the same pair, even when the beat number is the same (e.g. Power
Nap and Stress Relief are both 6 Hz, on different carriers; Relax, Calm
Mind and Balance are all 10 Hz, on three different carriers).

### Focus
| Mode | Beat | Left / Right | Default |
|---|---|---|---|
| Focus Mode | Gamma 40 Hz | 200 Hz / 240 Hz | 10 min |
| Energy | Beta 18 Hz | 200 Hz / 218 Hz | 15 min |

### Sleep
| Mode | Beat | Left / Right | Default |
|---|---|---|---|
| Deep Sleep | Delta 3 Hz | 100 Hz / 103 Hz | 30 min |
| Power Nap | Theta 6 Hz | 200 Hz / 206 Hz | 20 min |
| Restore | Delta 4 Hz | 100 Hz / 104 Hz | 20 min |

### Relax
| Mode | Beat | Left / Right | Default |
|---|---|---|---|
| Relax Mode | Theta 10 Hz | 400 Hz / 410 Hz | 15 min |
| Stress Relief Therapy | Theta 6 Hz | 150 Hz / 156 Hz | 15 min |
| Calm Mind Therapy | Alpha 10 Hz | 250 Hz / 260 Hz | 15 min |
| Balance | Alpha 10 Hz | 300 Hz / 310 Hz | 15 min |

### Relief
| Mode | Beat | Left / Right | Default |
|---|---|---|---|
| Pain Relief | Delta 2 Hz | 100 Hz / 102 Hz | 20 min |
| Healing | Delta 2.5 Hz | 120 Hz / 122.5 Hz | 20 min |

Player: play/pause, timer 15/30/60 min with auto-stop, volume slider,
foreground service so it keeps playing with the screen off and offline,
dark black + blue UI (welcome → "How can I help you today?" grouped by
category → player).

## Honest disclaimer

Binaural beats are **relaxation / entertainment, not a medical
treatment**. "Pain Relief" and "Healing" are labels for relaxation
modes — they are **not** a treatment for pain, not a cure for any
condition or disease, and they do not replace medical care. If pain
or a health problem persists, see a doctor. Do not listen while
driving. Stop if you feel headache, dizziness or ear discomfort.
Epilepsy / seizure history — ask a doctor first.

## App identity

- Label: **WaveMind**
- Package: `com.raj.wavemind`
- Version: 1.0 (versionCode 1)

## 1) Create the repo (one-time, Raj)

The Muse GitHub App cannot create repositories, so create the empty
repo manually first:

1. Open https://github.com/new
2. Repository name: `wavemind`
3. Owner: `aesthetic-dhruv` — **Public**, do NOT add a README / .gitignore / licence
4. Create repository
5. In GitHub Settings → Muse GitHub App installation, make sure the
   `wavemind` repo is selected, then tell Muse to push.

## 2) Push & build (Muse)

Push this whole folder to `aesthetic-dhruv/wavemind` (branch `main`).
The workflow `.github/workflows/android.yml` runs automatically
(JDK 17, Gradle 8.2, AGP 8.2.2 — same proven setup as Nova Premium v2)
and uploads artifact **`wavemind-debug-apk`**.

## 3) Install

- **Uninstall any previous WaveMind build first** — each cloud run uses
  a fresh debug keystore, so the new APK will not update over the old one.
- Install the debug APK from the workflow artifact.

## Phone test list (do on Raj's real phone)

1. App opens → welcome "Train your mind with sound" → Start → home
   "How can I help you today?" with 4 category headings (Focus, Sleep,
   Relax, Relief) and all 11 mode cards.
2. Headphone warning shows when no earbuds are connected, hides when
   earbuds are connected.
3. Tap Deep Sleep → player opens, timer shows 30:00, tone starts
   (earbuds in, volume ~45%). Left/right tones differ — check by
   removing one earbud: the pitch should change slightly.
4. Play/pause works; volume slider changes loudness; timer chips
   15/30/60 change the countdown; session auto-stops at 00:00.
5. Lock the screen / switch to another app — sound keeps playing
   (foreground service). Stop it from the notification.
6. Airplane mode ON — playback still works (fully offline, no files).
7. Try each of the 11 modes once and check the player tag shows the
   right frequencies — Healing must show "120 Hz / 122.5 Hz".
8. Power Nap default is 20 min; Pain Relief and Healing are labelled
   as relaxation support, not treatment.

Built-APK verification only — phone playback is NOT confirmed until
this list is tested on the real phone. **No APK was built yet** — the
repo `aesthetic-dhruv/wavemind` does not exist yet.

## Web prototype

`web-prototype/index.html` is a working browser version of the same app
(WebAudio generates the same live tones, same 11 modes and frequencies).
Open it in a browser to hear the modes and check the design before
installing the APK. It is a prototype only — the real app is the
native APK.
