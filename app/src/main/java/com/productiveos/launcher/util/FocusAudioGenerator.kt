package com.productiveos.launcher.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class FocusSoundMode(val id: String, val label: String) {
    RAIN("rain", "rainfall"),
    CAFE_BROWN("cafe", "warm brownian"),
    BINAURAL_ALPHA("alpha", "alpha binaural flow")
}

@Singleton
class FocusAudioGenerator @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _activeMode = MutableStateFlow<FocusSoundMode?>(null)
    val activeMode: StateFlow<FocusSoundMode?> = _activeMode.asStateFlow()

    fun startSound(mode: FocusSoundMode) {
        if (_activeMode.value == mode && audioJob?.isActive == true) {
            stopSound()
            return
        }
        stopSound()
        _activeMode.value = mode

        audioJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@launch

            audioTrack = track
            runCatching { track.play() }

            val samples = ShortArray(2048)
            var brownState = 0.0
            var pinkB0 = 0.0
            var pinkB1 = 0.0
            var pinkB2 = 0.0
            var phase1 = 0.0
            var phase2 = 0.0

            try {
                while (isActive && _activeMode.value == mode) {
                    for (i in samples.indices) {
                        val white = Random.nextDouble(-1.0, 1.0)
                        val sampleDouble = when (mode) {
                            FocusSoundMode.CAFE_BROWN -> {
                                brownState = (brownState + (0.02 * white)) / 1.02
                                (brownState * 3.2).coerceIn(-0.9, 0.9) * 0.35
                            }
                            FocusSoundMode.RAIN -> {
                                pinkB0 = 0.99765 * pinkB0 + white * 0.0990460
                                pinkB1 = 0.96300 * pinkB1 + white * 0.2965164
                                pinkB2 = 0.57000 * pinkB2 + white * 1.0526913
                                val pink = (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.06
                                pink.coerceIn(-0.9, 0.9) * 0.30
                            }
                            FocusSoundMode.BINAURAL_ALPHA -> {
                                phase1 += (2.0 * PI * 200.0) / sampleRate
                                phase2 += (2.0 * PI * 210.0) / sampleRate
                                if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                                if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                                brownState = (brownState + (0.015 * white)) / 1.02
                                val drone = (sin(phase1) + sin(phase2)) * 0.12
                                val warmBed = (brownState * 2.5).coerceIn(-0.8, 0.8) * 0.18
                                (drone + warmBed).coerceIn(-0.9, 0.9)
                            }
                        }
                        samples[i] = (sampleDouble * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(samples, 0, samples.size)
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun toggleOrCycleSound() {
        val current = _activeMode.value
        if (current != null) {
            stopSound()
        } else {
            startSound(FocusSoundMode.RAIN)
        }
    }

    fun stopSound() {
        _activeMode.value = null
        audioJob?.cancel()
        audioJob = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }
}

