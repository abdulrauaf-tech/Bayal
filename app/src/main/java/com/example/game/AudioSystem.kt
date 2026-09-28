package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioSystem(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val random = Random()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    fun playGunshot(weaponType: WeaponType) {
        if (!isSoundEnabled) return
        scope.launch {
            val sampleRate = 22050
            val durationMs = when (weaponType) {
                WeaponType.ASSAULT_RIFLE -> 120
                WeaponType.SNIPER_RIFLE -> 260
                WeaponType.SHOTGUN -> 200
                WeaponType.ROCKET_LAUNCHER -> 350
            }
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = i.toFloat() / numSamples
                val envelope = exp(-progress * 10f)

                // White noise + low thump frequency
                val noise = (random.nextFloat() * 2f - 1f)
                val baseFreq = when (weaponType) {
                    WeaponType.SNIPER_RIFLE -> 60f
                    WeaponType.SHOTGUN -> 80f
                    WeaponType.ROCKET_LAUNCHER -> 45f
                    else -> 110f
                }
                val thump = sin(2 * PI * baseFreq * t).toFloat()
                val sample = ((noise * 0.7f + thump * 0.3f) * envelope * 32767f).toInt().coerceIn(-32768, 32767)
                buffer[i] = sample.toShort()
            }
            playPcmBuffer(buffer, sampleRate)
        }

        if (isHapticsEnabled) {
            triggerVibration(
                when (weaponType) {
                    WeaponType.SNIPER_RIFLE, WeaponType.ROCKET_LAUNCHER -> 70L
                    WeaponType.SHOTGUN -> 50L
                    else -> 25L
                }
            )
        }
    }

    fun playHitMarker() {
        if (!isSoundEnabled) return
        scope.launch {
            val sampleRate = 22050
            val durationMs = 60
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = i.toFloat() / numSamples
                val env = 1f - progress
                val freq = 1250f
                val sample = (sin(2 * PI * freq * t) * env * 24000f).toInt().coerceIn(-32768, 32767)
                buffer[i] = sample.toShort()
            }
            playPcmBuffer(buffer, sampleRate)
        }

        if (isHapticsEnabled) {
            triggerVibration(18L)
        }
    }

    fun playHeal() {
        if (!isSoundEnabled) return
        scope.launch {
            val sampleRate = 22050
            val durationMs = 300
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = 440f + (t * 800f) // sweep up
                val env = sin(PI * (i.toFloat() / numSamples)).toFloat()
                val sample = (sin(2 * PI * freq * t) * env * 20000f).toInt().coerceIn(-32768, 32767)
                buffer[i] = sample.toShort()
            }
            playPcmBuffer(buffer, sampleRate)
        }
    }

    fun playVictoryFanfare() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
            val noteDurationMs = 180
            val sampleRate = 22050

            for (freq in notes) {
                val numSamples = (noteDurationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val env = (1f - (i.toFloat() / numSamples) * 0.7f)
                    val sample = (sin(2 * PI * freq * t) * env * 25000f).toInt().coerceIn(-32768, 32767)
                    buffer[i] = sample.toShort()
                }
                playPcmBuffer(buffer, sampleRate)
                kotlinx.coroutines.delay(160)
            }
        }

        if (isHapticsEnabled) {
            triggerVibration(120L)
        }
    }

    fun playDefeatSound() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(392f, 349.2f, 311.1f, 261.6f) // Descending minor
            val noteDurationMs = 220
            val sampleRate = 22050

            for (freq in notes) {
                val numSamples = (noteDurationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val env = 1f - (i.toFloat() / numSamples)
                    val sample = (sin(2 * PI * freq * t) * env * 22000f).toInt().coerceIn(-32768, 32767)
                    buffer[i] = sample.toShort()
                }
                playPcmBuffer(buffer, sampleRate)
                kotlinx.coroutines.delay(200)
            }
        }
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 100L)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibration(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
