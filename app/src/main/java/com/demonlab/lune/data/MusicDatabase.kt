package com.demonlab.lune.data

import androidx.room3.*

@Entity(tableName = "song_overrides")
data class SongOverride(
    @PrimaryKey val songId: Long, // Using MediaStore ID as primary key
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val coverUri: String? = null,
    val isFavorite: Boolean = false
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val position: Int = 0
)

@Dao
interface SongOverrideDao {
    @Query("SELECT * FROM song_overrides")
    suspend fun getAllOverrides(): List<SongOverride>

    @Query("SELECT * FROM song_overrides WHERE isFavorite = 1")
    suspend fun getFavorites(): List<SongOverride>

    @Query("SELECT * FROM song_overrides WHERE songId = :songId")
    suspend fun getOverrideForSong(songId: Long): SongOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: SongOverride)

    @Delete
    suspend fun deleteOverride(override: SongOverride)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAllPlaylistMappings(): List<PlaylistSong>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSong)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongsToPlaylist(playlistSongs: List<PlaylistSong>)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    suspend fun getSongIdsForPlaylist(playlistId: Long): List<Long>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSong>

    @Update
    suspend fun updatePlaylistSongs(playlistSongs: List<PlaylistSong>)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistCountForSong(songId: Long): Int
}

@Entity(tableName = "playback_stats")
data class PlaybackStats(
    @PrimaryKey val id: String, // e.g., "SONG_123", "PLAYLIST_456", "ARTIST_Queen"
    val type: String, // "SONG", "PLAYLIST", "ARTIST"
    val playCount: Long = 0,
    val totalTimeMs: Long = 0,
    val lastPlayed: Long = System.currentTimeMillis()
)

@Dao
interface PlaybackStatsDao {
    @Query("SELECT * FROM playback_stats WHERE id = :id")
    suspend fun getStatsById(id: String): PlaybackStats?

    @Query("SELECT * FROM playback_stats WHERE type = :type ORDER BY playCount DESC LIMIT :limit")
    fun getTopByCountFlow(type: String, limit: Int): kotlinx.coroutines.flow.Flow<List<PlaybackStats>>

    @Query("SELECT * FROM playback_stats WHERE type = :type ORDER BY totalTimeMs DESC LIMIT :limit")
    fun getTopByTimeFlow(type: String, limit: Int): kotlinx.coroutines.flow.Flow<List<PlaybackStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: PlaybackStats)
}

@Database(
    entities = [SongOverride::class, Playlist::class, PlaylistSong::class, PlaybackStats::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ],
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songOverrideDao(): SongOverrideDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStatsDao(): PlaybackStatsDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: android.content.Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
