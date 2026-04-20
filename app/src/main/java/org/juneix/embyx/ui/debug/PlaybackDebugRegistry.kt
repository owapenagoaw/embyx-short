package com.lalakiop.embyx.ui.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackDebugRegistry {
    private val _decoderBySource = MutableStateFlow<Map<String, String>>(emptyMap())
    val decoderBySource: StateFlow<Map<String, String>> = _decoderBySource.asStateFlow()

    fun updateDecoder(sourceId: String, decoderName: String?) {
        val safeName = decoderName?.trim().orEmpty()
        if (safeName.isBlank()) {
            return
        }
        _decoderBySource.value = _decoderBySource.value.toMutableMap().apply {
            this[sourceId] = safeName
        }
    }

    fun clearSource(sourceId: String) {
        _decoderBySource.value = _decoderBySource.value.toMutableMap().apply {
            remove(sourceId)
        }
    }

    fun clearAll() {
        _decoderBySource.value = emptyMap()
    }
}
