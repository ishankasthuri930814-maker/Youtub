package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdTimestamp DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    // Media Items
    @Query("SELECT * FROM media_items ORDER BY addedTimestamp DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE playlistId = :playlistId ORDER BY addedTimestamp DESC")
    fun getMediaItemsForPlaylist(playlistId: Long): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isDownloaded = 1 ORDER BY addedTimestamp DESC")
    fun getDownloadedMediaItems(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItem): Long

    @Update
    suspend fun updateMediaItem(item: MediaItem)

    @Delete
    suspend fun deleteMediaItem(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: Long)
}
