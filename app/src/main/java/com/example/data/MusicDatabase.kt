package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteSongEntity(
    @PrimaryKey val songUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface MusicDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // Playlist Songs
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songUri = :songUri")
    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    // Recently Played
    @Query("SELECT * FROM recently_played ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(song: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE songUri = :songUri")
    suspend fun deleteRecentlyPlayed(songUri: String)

    @Query("DELETE FROM recently_played")
    suspend fun clearRecentlyPlayed()

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songUri = :songUri)")
    fun isFavoriteFlow(songUri: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songUri = :songUri)")
    suspend fun isFavoriteDirect(songUri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteSongEntity)

    @Query("DELETE FROM favorites WHERE songUri = :songUri")
    suspend fun removeFavorite(songUri: String)
}

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        RecentlyPlayedEntity::class,
        FavoriteSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "onemusic_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
