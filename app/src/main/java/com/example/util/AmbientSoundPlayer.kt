package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundOption(val label: String) {
    SILENT("Silent"),
    RAIN("Rain"),
    FOREST("Forest"),
    WHITE_NOISE("White Noise"),
    SOFT_DRONE("Lo-Fi Pad")
}

class AmbientSoundPlayer {
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setSound(option: AmbientSoundOption, enabled: Boolean) {
        stop()
        if (!enabled || option == AmbientSoundOption.SILENT) return
        playJob = scope.launch {
            val sampleRate = 22050
            val bufferSamples = 2048
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferSamples * 2)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            audioTrack = track
            try {
                track.play()
                val pcm = ShortArray(bufferSamples)
                var lp1 = 0f
                var lp2 = 0f
                var phase1 = 0.0
                var phase2 = 0.0
                var lfoPhase = 0.0
                val rnd = Random(42)

                while (isActive) {
                    for (i in 0 until bufferSamples) {
                        val sample = when (option) {
                            AmbientSoundOption.WHITE_NOISE -> {
                                val white = (rnd.nextFloat() * 2f - 1f) * 0.14f
                                lp1 = lp1 * 0.65f + white * 0.35f
                                lp1
                            }
                            AmbientSoundOption.RAIN -> {
                                // Filtered pink-ish rain patter
                                val white = (rnd.nextFloat() * 2f - 1f)
                                lp1 = lp1 * 0.86f + white * 0.14f
                                lp2 = lp2 * 0.96f + lp1 * 0.04f
                                val drop = if (rnd.nextFloat() < 0.0025f) (rnd.nextFloat() - 0.5f) * 0.25f else 0f
                                (lp1 * 0.18f + lp2 * 0.22f + drop).coerceIn(-0.45f, 0.45f)
                            }
                            AmbientSoundOption.FOREST -> {
                                // Gentle wind swell through leaves
                                lfoPhase += (2.0 * PI * 0.12) / sampleRate
                                val swell = 0.5f + 0.45f * sin(lfoPhase).toFloat()
                                val white = (rnd.nextFloat() * 2f - 1f)
                                lp1 = lp1 * 0.92f + white * 0.08f
                                (lp1 * 0.26f * swell).coerceIn(-0.4f, 0.4f)
                            }
                            AmbientSoundOption.SOFT_DRONE -> {
                                // Warm 174Hz + 261Hz ambient study pad
                                phase1 += (2.0 * PI * 174.0) / sampleRate
                                phase2 += (2.0 * PI * 261.0) / sampleRate
                                lfoPhase += (2.0 * PI * 0.18) / sampleRate
                                val mod = 0.6f + 0.3f * sin(lfoPhase).toFloat()
                                ((sin(phase1) * 0.12 + sin(phase2) * 0.08) * mod).toFloat()
                            }
                            AmbientSoundOption.SILENT -> 0f
                        }
                        pcm[i] = (sample * Short.MAX_VALUE * 0.65f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } catch (_: Exception) {
                // Ignore audio stop exceptions
            } finally {
                try {
                    track.stop()
                } catch (_: Exception) {}
                try {
                    track.release()
                } catch (_: Exception) {}
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
    }
}
