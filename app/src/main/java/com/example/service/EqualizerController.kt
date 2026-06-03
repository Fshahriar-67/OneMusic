package com.example.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object EqualizerController {
    private const val TAG = "EqualizerController"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _bandLevels = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val bandLevels: StateFlow<Map<Int, Int>> = _bandLevels

    private val _bassLevel = MutableStateFlow(0)
    val bassLevel: StateFlow<Int> = _bassLevel

    private val _virtualizerLevel = MutableStateFlow(0)
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel

    private val _bands = MutableStateFlow<List<String>>(emptyList())
    val bands: StateFlow<List<String>> = _bands

    var minLevel: Short = -1500
    var maxLevel: Short = 1500

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                val numBands = numberOfBands
                val bandMap = mutableMapOf<Int, Int>()
                val bandNames = mutableListOf<String>()

                minLevel = bandLevelRange[0]
                maxLevel = bandLevelRange[1]

                for (i in 0 until numBands.toInt()) {
                    val bandShort = i.toShort()
                    val savedVal = _bandLevels.value[i] ?: getBandLevel(bandShort).toInt()
                    setBandLevel(bandShort, savedVal.toShort())
                    bandMap[i] = savedVal

                    val freqHz = getCenterFreq(bandShort) / 1000 // Convert mHz to Hz
                    bandNames.add(if (freqHz >= 1000) "${freqHz / 1000.0} kHz" else "$freqHz Hz")
                }
                _bandLevels.value = bandMap
                _bands.value = bandNames
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_bassLevel.value.toShort())
                }
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_virtualizerLevel.value.toShort())
                }
            }

            Log.d(TAG, "Successfully bound equalizer effects to session: $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding equalizer elements", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling equalizer enable state", e)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        val currentMap = _bandLevels.value.toMutableMap()
        currentMap[band] = level
        _bandLevels.value = currentMap

        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level for $band", e)
        }
    }

    fun setBassLevel(level: Int) {
        _bassLevel.value = level
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(level.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass strength", e)
        }
    }

    fun setVirtualizerLevel(level: Int) {
        _virtualizerLevel.value = level
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(level.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting virtualizer strength", e)
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing EQ components", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
        }
    }
}
