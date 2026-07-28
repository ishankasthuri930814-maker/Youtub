package com.example.data

import kotlinx.coroutines.flow.Flow

class StreamRepository(private val mediaDao: MediaDao) {
    val allPlaylists: Flow<List<Playlist>> = mediaDao.getAllPlaylists()
    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems()
    val downloadedItems: Flow<List<MediaItem>> = mediaDao.getDownloadedMediaItems()

    fun getItemsForPlaylist(playlistId: Long): Flow<List<MediaItem>> {
        return mediaDao.getMediaItemsForPlaylist(playlistId)
    }

    suspend fun createPlaylist(name: String, description: String = "", colorHex: String = "#FF2E63"): Long {
        return mediaDao.insertPlaylist(Playlist(name = name, description = description, colorHex = colorHex))
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        mediaDao.deletePlaylist(playlist)
    }

    suspend fun saveMediaItem(item: MediaItem): Long {
        return mediaDao.insertMediaItem(item)
    }

    suspend fun updateMediaItem(item: MediaItem) {
        mediaDao.updateMediaItem(item)
    }

    suspend fun deleteMediaItem(item: MediaItem) {
        mediaDao.deleteMediaItem(item)
    }

    suspend fun deleteMediaItemById(id: Long) {
        mediaDao.deleteMediaItemById(id)
    }
}
