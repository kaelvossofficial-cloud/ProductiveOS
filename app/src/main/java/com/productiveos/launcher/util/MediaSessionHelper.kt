package com.productiveos.launcher.util

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.productiveos.launcher.data.service.ProductiveNotificationListenerService

data class MusicInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val hasControls: Boolean
)

object MediaSessionHelper {

    private fun activeController(context: Context): MediaController? = runCatching {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listener = ComponentName(context, ProductiveNotificationListenerService::class.java)
        val sessions = manager.getActiveSessions(listener)
        sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata != null }
    }.getOrNull()

    /** Returns current music info or null when nothing is playing / available. */
    fun query(context: Context): MusicInfo? {
        val controller = activeController(context)
        if (controller != null) {
            val meta = controller.metadata
            val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
            val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (title.isNotBlank()) {
                return MusicInfo(title, artist, playing, hasControls = true)
            }
        }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return if (audio?.isMusicActive == true) {
            MusicInfo("audio playing", "", isPlaying = true, hasControls = false)
        } else null
    }

    fun playPause(context: Context) {
        val c = activeController(context)
        if (c != null) {
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } else {
            SystemLauncherHelper.toggleMediaPlayback(context)
        }
    }

    fun next(context: Context) {
        activeController(context)?.transportControls?.skipToNext()
    }

    fun previous(context: Context) {
        activeController(context)?.transportControls?.skipToPrevious()
    }
}
