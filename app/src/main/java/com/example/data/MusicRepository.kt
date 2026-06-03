package com.example.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context, private val dao: MusicDao) {

    private val _scannedSongs = MutableStateFlow<List<Song>>(emptyList())
    val scannedSongs: StateFlow<List<Song>> = _scannedSongs

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // Standard online demo songs in case storage is empty or for instant preview
    val onlineDemoSongs = listOf(
        Song(
            id = -1,
            title = "Midnight Horizon",
            artist = "Aether Flow",
            album = "Chill Beats Vol. 1",
            duration = 184000,
            path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            folder = "Cloud Preview"
        ),
        Song(
            id = -2,
            title = "Golden Hour Loop",
            artist = "Lofi Sands",
            album = "Chill Beats Vol. 1",
            duration = 218000,
            path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            folder = "Cloud Preview"
        ),
        Song(
            id = -3,
            title = "Synthesized Echoes",
            artist = "Cyber Runner",
            album = "Retro Neon",
            duration = 302000,
            path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            folder = "Cloud Preview"
        )
    )

    suspend fun scanLocalMusic() {
        _isScanning.value = true
        withContext(Dispatchers.IO) {
            val songsList = mutableListOf<Song>()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE
            )

            // Select only files that are recognized as music
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            try {
                context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Song"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val path = cursor.getString(dataCol) ?: ""
                        val size = cursor.getLong(sizeCol)

                        val songUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        val folderName = try {
                            File(path).parentFile?.name ?: "Unknown Folder"
                        } catch (e: Exception) {
                            "Unknown Folder"
                        }

                        // Only add songs with valid duration to avoid non-physical media
                        if (duration > 0) {
                            songsList.add(
                                Song(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    duration = duration,
                                    path = path,
                                    uri = songUri,
                                    folder = folderName,
                                    size = size
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback to online demos if local repository is empty
            if (songsList.isEmpty()) {
                songsList.addAll(onlineDemoSongs)
            }
            _scannedSongs.value = songsList
            _isScanning.value = false
        }
    }

    // Room DB Interactions exposed reactively
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val recentlyPlayed: Flow<List<RecentlyPlayedEntity>> = dao.getRecentlyPlayed()
    val favorites: Flow<List<FavoriteSongEntity>> = dao.getFavorites()

    fun isFavoriteFlow(songUri: String): Flow<Boolean> = dao.isFavoriteFlow(songUri)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>> =
        dao.getSongsForPlaylist(playlistId)

    // DB Suspend functions run on Dispatchers.IO
    suspend fun toggleFavorite(song: Song) = withContext(Dispatchers.IO) {
        val favorited = dao.isFavoriteDirect(song.uri)
        if (favorited) {
            dao.removeFavorite(song.uri)
        } else {
            dao.insertFavorite(
                FavoriteSongEntity(
                    songUri = song.uri,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration
                )
            )
        }
    }

    suspend fun addRecentlyPlayed(song: Song) = withContext(Dispatchers.IO) {
        dao.insertRecentlyPlayed(
            RecentlyPlayedEntity(
                songUri = song.uri,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(
            PlaylistEntity(
                name = name,
                description = description
            )
        )
    }

    suspend fun removePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
        dao.clearPlaylistSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) = withContext(Dispatchers.IO) {
        dao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songUri = song.uri,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String) = withContext(Dispatchers.IO) {
        dao.removeSongFromPlaylist(playlistId, songUri)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearRecentlyPlayed()
    }
}
