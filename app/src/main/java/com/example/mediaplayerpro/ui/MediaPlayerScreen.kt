package com.example.mediaplayerpro.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.mediaplayerpro.model.MediaItem
import com.example.mediaplayerpro.model.Playlist
import com.example.mediaplayerpro.viewmodel.MediaPlayerViewModel

@Composable
fun MediaPlayerScreen(
    viewModel: MediaPlayerViewModel,
    onSelectFile: () -> Unit,
    onSelectUrl: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val player = viewModel.getPlayer()
    val context = LocalContext.current

    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Video Player View
        if (currentMediaItem?.type == MediaItem.MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                            keepScreenOn = true
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { playerView ->
                        playerView.player = player
                        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                )
            }
        }

        // Playlist Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentPlaylist?.name ?: "Select Playlist",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { 
                showNewPlaylistDialog = true
                newPlaylistName = "" // Reset the name when opening dialog
            }) {
                Icon(Icons.Default.Add, "Create new playlist")
            }
        }

        // Media Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.playPrevious() }) {
                Icon(Icons.Default.SkipPrevious, "Previous")
            }
            IconButton(onClick = { if (isPlaying) viewModel.pause() else viewModel.resume() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = { viewModel.playNext() }) {
                Icon(Icons.Default.SkipNext, "Next")
            }
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(Icons.Default.Shuffle, "Shuffle")
            }
        }

        // Playlist Selection
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    isSelected = currentPlaylist?.id == playlist.id,
                    onSelect = { viewModel.selectPlaylist(playlist) }
                )
            }
        }

        // Media List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            currentPlaylist?.mediaItems?.let { items ->
                items(items) { item ->
                    MediaItemCard(
                        item = item,
                        isPlaying = currentMediaItem?.id == item.id && isPlaying,
                        onPlayClick = { 
                            if (currentMediaItem?.id == item.id) {
                                if (isPlaying) viewModel.pause() else viewModel.resume()
                            } else {
                                viewModel.playMedia(item)
                            }
                        },
                        onDeleteClick = { viewModel.removeMediaFromCurrentPlaylist(item) }
                    )
                }
            }
        }

        // Add Media Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onSelectFile) {
                Text("Select File")
            }
            Button(onClick = onSelectUrl) {
                Text("Load from URL")
            }
        }
    }

    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewPlaylistDialog = false
                newPlaylistName = "" 
            },
            title = { Text("Create New Playlist") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            showNewPlaylistDialog = false
                            newPlaylistName = ""
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNewPlaylistDialog = false
                    newPlaylistName = "" 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${playlist.mediaItems.size} items",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MediaItemCard(
    item: MediaItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                item.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 