package com.example.mediaplayerpro.model

data class Playlist(
    val id: String,
    val name: String,
    val mediaItems: List<MediaItem> = emptyList()
) 