package com.example.mediaplayerpro.model

import android.net.Uri

data class MediaItem(
    val id: String,
    val title: String,
    val uriString: String,
    val type: MediaType,
    val artist: String? = null
) {
    val uri: Uri get() = Uri.parse(uriString)

    enum class MediaType {
        AUDIO, VIDEO
    }

    companion object {
        fun create(id: String, title: String, uri: Uri, type: MediaType, artist: String? = null): MediaItem {
            return MediaItem(id, title, uri.toString(), type, artist)
        }
    }
} 