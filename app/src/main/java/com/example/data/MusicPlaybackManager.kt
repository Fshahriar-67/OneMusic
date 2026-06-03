package com.example.data

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
object MusicPlaybackManager {
    private const val TAG = "PlaybackManager"

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Live UI states
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress

    private val _songDuration = MutableStateFlow(0L)
    val songDuration: StateFlow<Long> = _songDuration

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    // Repo reference to save to Recently Played
    private var repository: MusicRepository? = null

    // Track original indices for shuffle management
    private var isInitialized = false

    fun initialize(context: Context, repo: MusicRepository) {
        if (isInitialized) return
        repository = repo
        isInitialized = true

        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                startProgressTracker()
                Log.d(TAG, "MediaController connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect MediaController", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                updateQueueStatus()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateCurrentSong(mediaItem)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeatMode.value = mode
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    _songDuration.value = mediaController?.duration ?: 0
                }
            }
        })

        // Initial trigger
        mediaController?.let { controller ->
            _isPlaying.value = controller.isPlaying
            _shuffleEnabled.value = controller.shuffleModeEnabled
            _repeatMode.value = controller.repeatMode
            updateCurrentSong(controller.currentMediaItem)
        }
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            return
        }

        // Search in active queue first
        val songUri = mediaItem.mediaId
        val matchedSong = _currentQueue.value.find { it.uri == songUri }
            ?: repository?.scannedSongs?.value?.find { it.uri == songUri }
            ?: Song(
                id = System.currentTimeMillis(),
                title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "Unknown",
                duration = mediaController?.duration ?: 0,
                path = songUri,
                uri = songUri,
                folder = "Cloud"
            )

        _currentSong.value = matchedSong

        // Save to Android history database asynchronously
        managerScope.launch {
            repository?.addRecentlyPlayed(matchedSong)
        }
    }

    private fun updateQueueStatus() {
        val controller = mediaController ?: return
        _songDuration.value = controller.duration.coerceAtLeast(0)
    }

    private fun startProgressTracker() {
        managerScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playbackProgress.value = controller.currentPosition
                        _songDuration.value = controller.duration.coerceAtLeast(0)
                    }
                }
                delay(100)
            }
        }
    }

    // Playback Commands
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController ?: return
        _currentQueue.value = songs

        controller.stop()
        controller.clearMediaItems()

        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .build()

            MediaItem.Builder()
                .setMediaId(song.uri)
                .setUri(song.uri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.addMediaItems(mediaItems)
        controller.seekTo(startIndex, 0)
        controller.prepare()
        controller.play()
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackProgress.value = positionMs
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
        _shuffleEnabled.value = nextMode
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val currentMode = controller.repeatMode
        val nextMode = when (currentMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun release() {
        managerScope.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
