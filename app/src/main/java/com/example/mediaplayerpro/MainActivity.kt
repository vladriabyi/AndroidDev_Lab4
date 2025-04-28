package com.example.mediaplayerpro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.mediaplayerpro.model.MediaItem
import com.example.mediaplayerpro.ui.MediaPlayerScreen
import com.example.mediaplayerpro.ui.theme.MediaPlayerProTheme
import com.example.mediaplayerpro.viewmodel.MediaPlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MediaPlayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MediaPlayerViewModel(application) as T
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    private val urlInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val url = result.data?.getStringExtra("url") ?: return@registerForActivityResult
            handleUrlInput(url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initializePlayer()

        setContent {
            MediaPlayerProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MediaPlayerScreen(
                        viewModel = viewModel,
                        onSelectFile = { filePickerLauncher.launch("*/*") },
                        onSelectUrl = { showUrlInputDialog() }
                    )
                }
            }
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            // Take persistent permission
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    val mimeType = contentResolver.getType(uri)
                    val type = when {
                        mimeType?.startsWith("audio/") == true -> MediaItem.MediaType.AUDIO
                        mimeType?.startsWith("video/") == true -> MediaItem.MediaType.VIDEO
                        else -> return
                    }
                    viewModel.addMediaToCurrentPlaylist(uri, displayName, type)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleUrlInput(url: String) {
        try {
            val uri = Uri.parse(url)
            val type = when {
                url.endsWith(".mp3") || url.endsWith(".wav") || url.endsWith(".ogg") -> 
                    MediaItem.MediaType.AUDIO
                url.endsWith(".mp4") || url.endsWith(".webm") || url.endsWith(".mkv") -> 
                    MediaItem.MediaType.VIDEO
                else -> return
            }
            viewModel.addMediaToCurrentPlaylist(uri, url.substringAfterLast('/'), type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUrlInputDialog() {
        val intent = Intent(this, UrlInputActivity::class.java)
        urlInputLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            viewModel.getPlayer()?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 