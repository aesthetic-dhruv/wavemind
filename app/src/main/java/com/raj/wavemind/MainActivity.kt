package com.raj.wavemind

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * WaveMind — Raj's own binaural beats app.
 * Dark black + blue theme, modelled on the reference UI:
 * welcome ("Train your mind with sound") -> home
 * ("How can I help you today?") -> player (circular art, timer, controls).
 *
 * All sound is generated live by [BinauralEngine] through
 * [BinauralService]. There are no audio files in this app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var modeList: LinearLayout
    private lateinit var chipRow: LinearLayout
    private lateinit var playerTitle: TextView
    private lateinit var playerTag: TextView
    private lateinit var playerTimer: TextView
    private lateinit var playerArt: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var volumeBar: SeekBar
    private lateinit var headphoneWarning: TextView
    private lateinit var timerLabel: TextView

    private var service: BinauralService? = null
    private var bound = false
    private var selectedMode: Mode = Mode.SLEEP
    private var selectedMinutes: Int = Mode.SLEEP.defaultMinutes

    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            updatePlayerUi()
            pollHandler.postDelayed(this, 1000)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as BinauralService.LocalBinder).getService()
            bound = true
            updatePlayerUi()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flipper = findViewById(R.id.flipper)
        modeList = findViewById(R.id.mode_list)
        chipRow = findViewById(R.id.chip_row)
        playerTitle = findViewById(R.id.player_title)
        playerTag = findViewById(R.id.player_tag)
        playerTimer = findViewById(R.id.player_timer)
        playerArt = findViewById(R.id.player_art)
        playPauseBtn = findViewById(R.id.btn_play_pause)
        volumeBar = findViewById(R.id.volume_bar)
        headphoneWarning = findViewById(R.id.headphone_warning)
        timerLabel = findViewById(R.id.timer_label)

        findViewById<Button>(R.id.btn_start).setOnClickListener { showHome() }
        findViewById<ImageButton>(R.id.btn_player_back).setOnClickListener { showHome() }
        findViewById<ImageButton>(R.id.btn_stop).setOnClickListener { stopSession() }
        playPauseBtn.setOnClickListener { togglePlayPause() }

        // Timer chips: 15 / 30 / 60 min — auto-stop at the end.
        val timerRow = findViewById<LinearLayout>(R.id.timer_row)
        listOf(15, 30, 60).forEach { minutes ->
            val chip = makeChip("$minutes min", minutes == selectedMinutes) {
                selectedMinutes = minutes
                refreshTimerChips()
                timerLabel.text = "Timer: $minutes min — auto-stop"
            }
            timerRow.addView(chip)
        }

        volumeBar.max = 100
        volumeBar.progress = 45
        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                service?.setVolume(progress / 100f)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        buildHome()
        updateHeadphoneWarning()
        requestNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, BinauralService::class.java),
            connection, Context.BIND_AUTO_CREATE
        )
        pollHandler.post(pollRunnable)
    }

    override fun onStop() {
        super.onStop()
        pollHandler.removeCallbacks(pollRunnable)
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    // ---------- screens ----------

    private fun showHome() {
        flipper.displayedChild = 1
    }

    private fun showPlayer(mode: Mode) {
        selectedMode = mode
        selectedMinutes = mode.defaultMinutes
        refreshTimerChips()
        playerTitle.text = mode.title
        playerTag.text = "${mode.tag} • ${formatHz(mode.leftHz)} / ${formatHz(mode.rightHz)}"
        playerArt.text = mode.title.first().toString()
        timerLabel.text = "Timer: $selectedMinutes min — auto-stop"
        updateHeadphoneWarning()
        flipper.displayedChild = 2
    }

    // ---------- home ----------

    private fun buildHome() {
        // Filter chips (all modes) — tap = open that mode.
        Mode.ALL.forEach { mode ->
            chipRow.addView(makeChip(mode.title, false) { openMode(mode) })
        }
        // Mode cards, grouped under a category heading.
        Mode.CATEGORIES.forEach { category ->
            val header = TextView(this).apply {
                text = category
                setTextColor(ContextCompat.getColor(context, R.color.blue_light))
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(4, 28, 4, 0)
            }
            modeList.addView(header)
            Mode.ALL.filter { it.category == category }.forEach { mode ->
                val card = layoutInflater.inflate(R.layout.item_mode, modeList, false)
                card.findViewById<TextView>(R.id.mode_card_title).text = mode.title
                card.findViewById<TextView>(R.id.mode_card_sub).text = mode.subtitle
                card.findViewById<TextView>(R.id.mode_card_time).text =
                    "${mode.defaultMinutes} min • ${mode.tag}"
                card.findViewById<TextView>(R.id.mode_card_art).text = mode.title.first().toString()
                card.findViewById<ImageButton>(R.id.mode_card_play)
                    .setOnClickListener { openMode(mode) }
                card.setOnClickListener { openMode(mode) }
                modeList.addView(card)
            }
        }
    }

    private fun openMode(mode: Mode) {
        showPlayer(mode)
        startSession(mode)
    }

    // ---------- playback ----------

    private fun startSession(mode: Mode) {
        if (!hasHeadphones()) {
            Toast.makeText(
                this,
                "Headphones / earbuds lagao — binaural beat bina unke kaam nahi karta",
                Toast.LENGTH_LONG
            ).show()
        }
        val intent = Intent(this, BinauralService::class.java).apply {
            action = BinauralService.ACTION_PLAY
            putExtra(BinauralService.EXTRA_MODE_ID, mode.id)
            putExtra(BinauralService.EXTRA_MINUTES, selectedMinutes)
            putExtra(BinauralService.EXTRA_VOLUME, volumeBar.progress / 100f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Re-bind so the activity can read live timer state (only if not
        // already bound — a second bind with the same connection leaks it).
        if (!bound) {
            bindService(
                Intent(this, BinauralService::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
        }
        updatePlayerUi()
    }

    private fun togglePlayPause() {
        val svc = service ?: return
        if (svc.isPlaying()) {
            svc.pause()
        } else if (svc.getRemainingMs() > 0) {
            svc.resume()
        } else {
            startSession(selectedMode)
        }
        updatePlayerUi()
    }

    private fun stopSession() {
        service?.stopSession()
        stopService(Intent(this, BinauralService::class.java))
        updatePlayerUi()
        Toast.makeText(this, "Session stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updatePlayerUi() {
        val svc = service
        if (svc == null) {
            playerTimer.text = formatTime(selectedMinutes * 60L * 1000L)
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
            return
        }
        val remaining = svc.getRemainingMs()
        playerTimer.text = formatTime(remaining)
        playPauseBtn.setImageResource(
            if (svc.isPlaying()) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
        val mode = svc.getCurrentMode()
        if (flipper.displayedChild == 2) {
            playerTitle.text = mode.title
            playerTag.text = "${mode.tag} • ${formatHz(mode.leftHz)} / ${formatHz(mode.rightHz)}"
            playerArt.text = mode.title.first().toString()
        }
    }

    private fun refreshTimerChips() {
        val timerRow = findViewById<LinearLayout>(R.id.timer_row)
        for (i in 0 until timerRow.childCount) {
            val chip = timerRow.getChildAt(i) as Button
            val minutes = chip.text.toString().replace(" min", "").toInt()
            chip.isSelected = minutes == selectedMinutes
            chip.setBackgroundResource(
                if (minutes == selectedMinutes) R.drawable.bg_chip_selected
                else R.drawable.bg_chip
            )
        }
    }

    // ---------- helpers ----------

    private fun makeChip(text: String, selected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isSelected = selected
            setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 12f
            isAllCaps = false
            setPadding(28, 12, 28, 12)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            layoutParams = params
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun hasHeadphones(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
        }
    }

    private fun updateHeadphoneWarning() {
        headphoneWarning.visibility =
            if (hasHeadphones()) View.GONE else View.VISIBLE
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // 122.5 Hz must show as "122.5 Hz", 100 Hz as "100 Hz".
    private fun formatHz(hz: Double): String =
        if (hz == hz.toInt().toDouble()) "${hz.toInt()} Hz" else "$hz Hz"
}
