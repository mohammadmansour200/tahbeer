// Credit: https://github.com/lincollincol/compose-audiowaveform/blob/master/app/src/main/java/com/linc/audiowaveform/sample/android/AudioPlaybackManager.kt
package com.tahbeer.app.details.data.media

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.tahbeer.app.details.domain.MediaPlaybackManager
import com.tahbeer.app.details.domain.MediaPlaybackManager.Event
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

class LocalMediaPlaybackManager(
    private val player: Player
) : MediaPlaybackManager, Player.Listener {

    companion object {
        private const val PLAYER_POSITION_UPDATE_TIME = 500L
    }

    override val events: MutableSharedFlow<Event> = MutableSharedFlow()

    private var lastEmittedPosition: Long = 0
    private var handler: Handler? = null

    private val playerPositionRunnable = object : Runnable {
        override fun run() {
            val playbackPosition = player.currentPosition
            if (playbackPosition != lastEmittedPosition) {
                sendEvent(Event.PositionChanged(playbackPosition))
                lastEmittedPosition = playbackPosition
            }
            handler?.postDelayed(this, PLAYER_POSITION_UPDATE_TIME)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {
                sendEvent(Event.MediaReady)
            }

            else -> {
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        sendEvent(Event.MediaError)
    }

    override fun releaseMedia() {
        clearMedia()
        player.removeListener(this)
        handler?.removeCallbacks(playerPositionRunnable)
        handler = null
    }

    override fun loadMedia(uri: Uri) {
        clearMedia()

        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem, true)
        player.prepare()
        startTrackingPlaybackPosition()
    }

    private fun clearMedia() {
        player.stop()
        player.clearMediaItems()
    }

    private fun startTrackingPlaybackPosition() {
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed(playerPositionRunnable, PLAYER_POSITION_UPDATE_TIME)
        player.addListener(this)
    }

    private fun sendEvent(event: Event) {
        runBlocking { events.emit(event) }
    }
}