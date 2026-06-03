package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.Language
import com.example.ui.theme.LanguageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Album(val name: String, val artist: String, val songs: List<Song>)
data class Artist(val name: String, val songs: List<Song>)
data class Folder(val name: String, val songs: List<Song>)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(application, database.musicDao())

    val scannedSongs = repository.scannedSongs
    val isScanning = repository.isScanning

    // Playlists & Favorites from DB
    val playlists = repository.allPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyPlayed = repository.recentlyPlayed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Active visual tab
    private val _currentLibraryTab = MutableStateFlow(0) // 0: Songs, 1: Albums, 2: Artists, 3: Folders, 4: Playlists, 5: Favorites, 6: Recent
    val currentLibraryTab: StateFlow<Int> = _currentLibraryTab

    // Language switcher (English / Bangla)
    val currentLanguage = LanguageManager.currentLanguage

    // UI Theme Preferences (Dark / Light)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    // Sleep Timer states
    private val _sleepTimeRemaining = MutableStateFlow(0L) // Remaining time in ms
    val sleepTimeRemaining: StateFlow<Long> = _sleepTimeRemaining

    private var sleepTimerJob: Job? = null

    init {
        // Init Playback Manager
        MusicPlaybackManager.initialize(application, repository)
        
        // Scan library once on startup
        viewModelScope.launch {
            repository.scanLocalMusic()
        }
    }

    // Dynamic groupings calculated on the fly
    val albums: StateFlow<List<Album>> = scannedSongs.map { songs ->
        songs.groupBy { it.album }.map { (name, list) ->
            Album(name, list.firstOrNull()?.artist ?: "Unknown Artist", list)
        }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = scannedSongs.map { songs ->
        songs.groupBy { it.artist }.map { (name, list) ->
            Artist(name, list)
        }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = scannedSongs.map { songs ->
        songs.groupBy { it.folder }.map { (name, list) ->
            Folder(name, list)
        }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter songs based on search query
    val filteredSongs: StateFlow<List<Song>> = combine(scannedSongs, searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLibraryTab(tabIndex: Int) {
        _currentLibraryTab.value = tabIndex
    }

    fun toggleLanguage() {
        val nextLang = if (currentLanguage.value == Language.ENGLISH) Language.BANGLA else Language.ENGLISH
        LanguageManager.setLanguage(nextLang)
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            repository.scanLocalMusic()
        }
    }

    // Favorite Actions
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun isFavoriteFlow(songUri: String): Flow<Boolean> {
        return repository.isFavoriteFlow(songUri)
    }

    // Playlist Database managers
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songUri: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songUri)
        }
    }

    fun getPlaylistSongsFlow(playlistId: Long): Flow<List<Song>> {
        return repository.getSongsForPlaylist(playlistId).map { playlistSongs ->
            playlistSongs.map {
                Song(
                    id = 0,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    duration = it.duration,
                    path = it.songUri,
                    uri = it.songUri,
                    folder = "Playlist"
                )
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.removePlaylist(playlistId)
        }
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Playback proxies
    fun playSongFromLibrary(songs: List<Song>, songIndex: Int) {
        MusicPlaybackManager.playSongs(songs, songIndex)
    }

    // Sleep Timer Logic
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimeRemaining.value = 0
            return
        }

        val totalMs = minutes * 60 * 1000L
        _sleepTimeRemaining.value = totalMs

        sleepTimerJob = viewModelScope.launch {
            var timeMs = totalMs
            while (timeMs > 0) {
                delay(1000)
                timeMs -= 1000
                _sleepTimeRemaining.value = timeMs
            }
            // Timer expired - Pause playback safely
            MusicPlaybackManager.mediaController?.pause()
            _sleepTimeRemaining.value = 0
            Log.d("MusicViewModel", "Sleep timer completed: Paused playback")
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopSleepTimer()
    }
}
