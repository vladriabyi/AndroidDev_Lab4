package com.example.mediaplayerpro.utils

import android.content.Context
import android.net.Uri
import com.example.mediaplayerpro.model.MediaItem
import com.example.mediaplayerpro.model.Playlist
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("MediaPlayerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val json = gson.toJson(playlists)
            sharedPreferences.edit()
                .putString("playlists", json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPlaylists(): List<Playlist> {
        return try {
            val json = sharedPreferences.getString("playlists", null)
            if (json != null) {
                val type = object : TypeToken<List<Playlist>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
} 