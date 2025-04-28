package com.example.mediaplayerpro.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mediaplayerpro.model.MediaItem
import com.example.mediaplayerpro.model.Playlist
import com.example.mediaplayerpro.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class MediaPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private var player: ExoPlayer? = null

    init {
        loadSavedPlaylists()
    }

    fun initializePlayer() {
        if (player == null) {
            try {
                player = ExoPlayer.Builder(getApplication())
                    .setHandleAudioBecomingNoisy(true)
                    .build().apply {
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            error.printStackTrace()
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSavedPlaylists() {
        viewModelScope.launch {
            try {
                val savedPlaylists = preferencesManager.loadPlaylists()
                _playlists.value = savedPlaylists
                if (savedPlaylists.isNotEmpty()) {
                    _currentPlaylist.value = savedPlaylists[0]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun savePlaylists() {
        viewModelScope.launch {
            try {
                preferencesManager.savePlaylists(_playlists.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPlaylist(name: String) {
        try {
            val newPlaylist = Playlist(
                id = UUID.randomUUID().toString(),
                name = name,
                mediaItems = mutableListOf()
            )
            _playlists.value = _playlists.value + newPlaylist
            if (_currentPlaylist.value == null) {
                _currentPlaylist.value = newPlaylist
            }
            savePlaylists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _currentPlaylist.value = playlist
    }

    fun addMediaToCurrentPlaylist(uri: Uri, title: String, type: MediaItem.MediaType) {
        try {
            val currentPlaylist = _currentPlaylist.value ?: return
            val newMediaItem = MediaItem.create(
                id = UUID.randomUUID().toString(),
                uri = uri,
                title = title,
                type = type
            )
            
            val updatedPlaylist = currentPlaylist.copy(
                mediaItems = currentPlaylist.mediaItems + newMediaItem
            )
            
            _playlists.value = _playlists.value.map { 
                if (it.id == currentPlaylist.id) updatedPlaylist else it 
            }
            _currentPlaylist.value = updatedPlaylist
            savePlaylists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playMedia(mediaItem: MediaItem) {
        try {
            initializePlayer()
            _currentMediaItem.value = mediaItem
            player?.apply {
                // Clear any previous media items
                clearMediaItems()
                
                // Create ExoPlayer MediaItem with additional configuration
                val exoMediaItem = ExoMediaItem.Builder()
                    .setUri(mediaItem.uri)
                    .setMediaId(mediaItem.id)
                    .build()
                
                setMediaItem(exoMediaItem)
                prepare()
                playWhenReady = true
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            player?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            player?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNext() {
        try {
            val currentPlaylist = _currentPlaylist.value ?: return
            val currentItem = _currentMediaItem.value
            val currentIndex = currentPlaylist.mediaItems.indexOf(currentItem)
            
            if (currentIndex != -1) {
                val nextIndex = if (_isShuffleEnabled.value) {
                    Random().nextInt(currentPlaylist.mediaItems.size)
                } else {
                    (currentIndex + 1) % currentPlaylist.mediaItems.size
                }
                playMedia(currentPlaylist.mediaItems[nextIndex])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPrevious() {
        try {
            val currentPlaylist = _currentPlaylist.value ?: return
            val currentItem = _currentMediaItem.value
            val currentIndex = currentPlaylist.mediaItems.indexOf(currentItem)
            
            if (currentIndex != -1) {
                val previousIndex = if (_isShuffleEnabled.value) {
                    Random().nextInt(currentPlaylist.mediaItems.size)
                } else {
                    if (currentIndex == 0) currentPlaylist.mediaItems.size - 1
                    else currentIndex - 1
                }
                playMedia(currentPlaylist.mediaItems[previousIndex])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun getPlayer(): ExoPlayer? = player

    fun removeMediaFromCurrentPlaylist(mediaItem: MediaItem) {
        try {
            val currentPlaylist = _currentPlaylist.value ?: return
            
            // If this is the currently playing item, stop playback
            if (_currentMediaItem.value?.id == mediaItem.id) {
                player?.stop()
                _currentMediaItem.value = null
            }
            
            val updatedPlaylist = currentPlaylist.copy(
                mediaItems = currentPlaylist.mediaItems.filter { it.id != mediaItem.id }
            )
            
            _playlists.value = _playlists.value.map { 
                if (it.id == currentPlaylist.id) updatedPlaylist else it 
            }
            _currentPlaylist.value = updatedPlaylist
            savePlaylists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            player?.release()
            player = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 