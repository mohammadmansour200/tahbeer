package com.tahbeer.app.details.domain

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow

interface MediaPlaybackManager {
    fun loadMedia(uri: Uri)

    fun releaseMedia()

    val events: MutableSharedFlow<Event>

    sealed interface Event {
        data class PositionChanged(val position: Long) : Event
        data object MediaError : Event
        data object MediaReady : Event
    }
}