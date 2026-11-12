/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.Date
import kotlin.random.Random
import org.prauga.clock.DeskClock
import org.prauga.clock.R

/**
 * Plays a generated noise until a stop time: white, pink or brown noise made on the device, so no
 * recordings have to be shipped or licensed. Fades in and out.
 */
class SleepSoundService : Service() {

    enum class Noise(val labelRes: Int) {
        WHITE(R.string.sleep_sound_white),
        PINK(R.string.sleep_sound_pink),
        BROWN(R.string.sleep_sound_brown),
    }

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var playing = false
    private var thread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }
        val noise = Noise.entries[intent?.getIntExtra(EXTRA_NOISE, 0) ?: 0]
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 30) ?: 30
        val stopAt = System.currentTimeMillis() + minutes * DateUtils.MINUTE_IN_MILLIS
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(noise, stopAt),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        isPlaying = true
        startNoise(noise, stopAt)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopPlayback() }, stopAt - System.currentTimeMillis())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        playing = false
        isPlaying = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun stopPlayback() {
        playing = false
        isPlaying = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startNoise(noise: Noise, stopAt: Long) {
        playing = false
        thread?.join(500)
        playing = true
        thread =
            Thread({ play(noise, stopAt) }, "SleepSound").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
    }

    private fun play(noise: Noise, stopAt: Long) {
        val rate = 44100
        val minBuffer =
            AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
        val track =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        val buffer = FloatArray(rate / 10)
        val generator = NoiseGenerator(noise)
        val start = System.currentTimeMillis()
        track.play()
        try {
            while (playing) {
                val now = System.currentTimeMillis()
                // Fade in over the first 10 s and out over the last 30 s.
                val gain =
                    minOf(1f, (now - start) / FADE_IN_MS, maxOf(0f, (stopAt - now) / FADE_OUT_MS))
                for (i in buffer.indices) {
                    buffer[i] = generator.next() * gain * LEVEL
                }
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private fun notification(noise: Noise, stopAt: Long) =
        NotificationCompat.Builder(this, channel())
            .setSmallIcon(R.drawable.stat_notify_bedtime)
            .setContentTitle(getString(R.string.sleep_sound_playing, getString(noise.labelRes)))
            .setContentText(
                getString(
                    R.string.sleep_sound_stops_at,
                    DateFormat.getTimeFormat(this).format(Date(stopAt)),
                )
            )
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, DeskClock::class.java)
                        .setAction(BedtimeScheduler.ACTION_SHOW_BEDTIME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .addAction(
                0,
                getString(R.string.sleep_sound_stop),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, SleepSoundService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    private fun channel(): String {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.sleep_sound_channel),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        return CHANNEL_ID
    }

    /** White noise, and pink and brown noise filtered from it. */
    private class NoiseGenerator(private val noise: Noise) {
        private val random = Random(System.nanoTime())
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var brown = 0f

        fun next(): Float {
            val white = random.nextFloat() * 2f - 1f
            return when (noise) {
                Noise.WHITE -> white * 0.5f
                Noise.PINK -> {
                    // Paul Kellet's economy pink filter.
                    b0 = 0.99765f * b0 + white * 0.0990460f
                    b1 = 0.96300f * b1 + white * 0.2965164f
                    b2 = 0.57000f * b2 + white * 1.0526913f
                    (b0 + b1 + b2 + white * 0.1848f) * 0.2f
                }
                Noise.BROWN -> {
                    brown = (brown + white * 0.02f) / 1.02f
                    brown * 3.5f
                }
            }
        }
    }

    companion object {
        private const val ACTION_STOP = "org.prauga.clock.action.STOP_SLEEP_SOUND"
        private const val EXTRA_NOISE = "noise"
        private const val EXTRA_MINUTES = "minutes"
        private const val CHANNEL_ID = "sleep_sounds"
        private const val NOTIFICATION_ID = 0x51ee9
        private const val FADE_IN_MS = 10_000f
        private const val FADE_OUT_MS = 30_000f
        private const val LEVEL = 0.6f

        /** Whether a sleep sound is playing in this process. */
        @Volatile
        var isPlaying = false
            private set

        fun start(context: Context, noise: Noise, minutes: Int) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepSoundService::class.java)
                    .putExtra(EXTRA_NOISE, noise.ordinal)
                    .putExtra(EXTRA_MINUTES, minutes),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SleepSoundService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
