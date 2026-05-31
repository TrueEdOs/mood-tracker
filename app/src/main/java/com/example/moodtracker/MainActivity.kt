package com.example.moodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {

    private val csvFile by lazy { File(filesDir, FILE_NAME) }
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { refreshButtons() }

    private lateinit var moodButtons: List<Button>
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bad = findViewById<Button>(R.id.button_bad)
        val ok = findViewById<Button>(R.id.button_ok)
        val good = findViewById<Button>(R.id.button_good)
        moodButtons = listOf(bad, ok, good)
        statusText = findViewById(R.id.status_text)

        bad.setOnClickListener { onMoodClicked("Bad") }
        ok.setOnClickListener { onMoodClicked("Ok") }
        good.setOnClickListener { onMoodClicked("Good") }

        findViewById<Button>(R.id.button_share).setOnClickListener { shareCsv() }
    }

    override fun onResume() {
        super.onResume()
        refreshButtons()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        super.onDestroy()
    }

    private fun onMoodClicked(mood: String) {
        if (logMood(mood)) {
            Toast.makeText(this, getString(R.string.saved, mood), Toast.LENGTH_SHORT).show()
        }
        refreshButtons()
    }

    /** Appends "timestamp,mood" to the CSV. Returns true on success. */
    private fun logMood(mood: String): Boolean {
        return try {
            if (!csvFile.exists()) {
                csvFile.writeText("timestamp,mood\n")
            }
            val timestamp = LocalDateTime.now().format(FORMATTER)
            csvFile.appendText("$timestamp,$mood\n")
            true
        } catch (e: Exception) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Epoch millis of the most recent logged entry, or null if there is no
     * valid entry yet (treated as "unlocked").
     */
    private fun lastEntryMillis(): Long? {
        return try {
            if (!csvFile.exists()) return null
            val lastLine = csvFile.readLines()
                .lastOrNull { it.isNotBlank() && !it.startsWith("timestamp,") }
                ?: return null
            val ts = lastLine.substringBefore(',').trim()
            LocalDateTime.parse(ts, FORMATTER)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    /** Enables/disables the mood buttons based on the 5-minute lock window. */
    private fun refreshButtons() {
        handler.removeCallbacks(refreshRunnable)
        val last = lastEntryMillis()
        val remaining = if (last == null) 0L else LOCK_MS - (System.currentTimeMillis() - last)

        if (remaining > 0) {
            moodButtons.forEach { it.isEnabled = false }
            val availableAt = Instant.ofEpochMilli(last!! + LOCK_MS)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FORMATTER)
            statusText.text = getString(R.string.status_locked, availableAt)
            handler.postDelayed(refreshRunnable, remaining)
        } else {
            moodButtons.forEach { it.isEnabled = true }
            statusText.text = getString(R.string.status_ready)
        }
    }

    private fun shareCsv() {
        if (!csvFile.exists() || csvFile.length() == 0L) {
            Toast.makeText(this, R.string.nothing_to_share, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", csvFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val FILE_NAME = "moods.csv"
        private const val LOCK_MS = 5 * 60 * 1000L
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
