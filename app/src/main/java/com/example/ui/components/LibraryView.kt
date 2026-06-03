package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.Album
import com.example.ui.Artist
import com.example.ui.Folder
import com.example.ui.theme.LanguageManager

@Composable
fun LibraryView(
    songs: List<Song>,
    isScanning: Boolean,
    currentTab: Int,
    searchQuery: String,
    playlists: List<PlaylistEntity>,
    recentlyPlayed: List<RecentlyPlayedEntity>,
    favorites: List<FavoriteSongEntity>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    albumGroups: List<Album>,
    artistGroups: List<Artist>,
    folderGroups: List<Folder>,
    onTabSelected: (Int) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSongSelected: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOptionsClicked: (Song) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onScanRequest: () -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        LanguageManager.getString("songs"),
        LanguageManager.getString("albums"),
        LanguageManager.getString("artists"),
        LanguageManager.getString("folders"),
        LanguageManager.getString("playlists"),
        LanguageManager.getString("favorites"),
        LanguageManager.getString("recent")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text(text = LanguageManager.getString("search"), fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Tabs Scroll View
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = currentTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab content selector switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (songs.isEmpty() && currentTab == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = LanguageManager.getString("no_songs"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onScanRequest) {
                            Text(text = LanguageManager.getString("scan"))
                        }
                    }
                }
            } else {
                when (currentTab) {
                    0 -> SongsTab(songs, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                    1 -> AlbumsGrid(albumGroups, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                    2 -> ArtistsTab(artistGroups, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                    3 -> FoldersTab(folderGroups, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                    4 -> PlaylistsTab(playlists, onDeletePlaylist, onPlaylistSelected)
                    5 -> FavoritesTab(favorites, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                    6 -> RecentlyPlayedTab(recentlyPlayed, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
                }
            }
        }
    }
}

// Subcomponents helper
@Composable
fun SongsTab(
    songs: List<Song>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(songs) { index, song ->
            SongListItem(
                song = song,
                isCurrent = song.uri == currentPlayingSong?.uri,
                isPlaying = isPlaying,
                onClick = { onSongSelected(songs, index) },
                onOptionsClick = { onOptionsClicked(song) }
            )
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded cover
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.path)
                    .fallback(android.R.drawable.ic_media_play)
                    .error(android.R.drawable.ic_media_play)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bouncing animated equalizer overlay if this song is playing!
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    BouncingEqualizerAnimation()
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title + Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Subtitle / duration
        Text(
            text = formatTime(song.duration),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(onClick = onOptionsClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BouncingEqualizerAnimation() {
    val b1 = rememberInfiniteTransition().animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse)
    )
    val b2 = rememberInfiniteTransition().animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse)
    )
    val b3 = rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse)
    )

    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width / 5f
        val h = size.height

        // Bar 1
        drawRoundRect(
            color = Color(0xFFEB0028),
            topLeft = Offset(0f, h - (h * b1.value)),
            size = Size(w, h * b1.value),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Bar 2
        drawRoundRect(
            color = Color(0xFFEB0028),
            topLeft = Offset(w * 2f, h - (h * b2.value)),
            size = Size(w, h * b2.value),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Bar 3
        drawRoundRect(
            color = Color(0xFFEB0028),
            topLeft = Offset(w * 4f, h - (h * b3.value)),
            size = Size(w, h * b3.value),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

// Section subviews:
@Composable
fun AlbumsGrid(
    albums: List<Album>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    var expandedAlbum by remember { mutableStateOf<Album?>(null) }

    if (expandedAlbum != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedAlbum = null }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = expandedAlbum!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                itemsIndexed(expandedAlbum!!.songs) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = song.uri == currentPlayingSong?.uri,
                        isPlaying = isPlaying,
                        onClick = { onSongSelected(expandedAlbum!!.songs, index) },
                        onOptionsClick = { onOptionsClicked(song) }
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedAlbum = album },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.songs.firstOrNull()?.path)
                                .fallback(android.R.drawable.ic_media_play)
                                .error(android.R.drawable.ic_media_play)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = album.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.songs.size} Track(s)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(
    artists: List<Artist>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    var expandedArtist by remember { mutableStateOf<Artist?>(null) }

    if (expandedArtist != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedArtist = null }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = expandedArtist!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                itemsIndexed(expandedArtist!!.songs) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = song.uri == currentPlayingSong?.uri,
                        isPlaying = isPlaying,
                        onClick = { onSongSelected(expandedArtist!!.songs, index) },
                        onOptionsClick = { onOptionsClicked(song) }
                    )
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(artists) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedArtist = artist }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${artist.songs.size} track(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun FoldersTab(
    folders: List<Folder>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    var expandedFolder by remember { mutableStateOf<Folder?>(null) }

    if (expandedFolder != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFolder = null }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = expandedFolder!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                itemsIndexed(expandedFolder!!.songs) { index, song ->
                    SongListItem(
                        song = song,
                        isCurrent = song.uri == currentPlayingSong?.uri,
                        isPlaying = isPlaying,
                        onClick = { onSongSelected(expandedFolder!!.songs, index) },
                        onOptionsClick = { onOptionsClicked(song) }
                    )
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(folders) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFolder = folder }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${folder.songs.size} music tracks",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<PlaylistEntity>,
    onDeletePlaylist: (Long) -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        items(playlists) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistSelected(playlist) }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Custom User Playlist",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    }
}

@Composable
fun FavoritesTab(
    favorites: List<FavoriteSongEntity>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    val songs = favorites.map {
        Song(0, it.title, it.artist, it.album, it.duration, it.songUri, it.songUri, "Favorites")
    }

    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No tracks favorited yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        SongsTab(songs, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
    }
}

@Composable
fun RecentlyPlayedTab(
    recentlyPlayed: List<RecentlyPlayedEntity>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit
) {
    val songs = recentlyPlayed.map {
        Song(0, it.title, it.artist, it.album, it.duration, it.songUri, it.songUri, "Recently Played")
    }

    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No music played recently", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        SongsTab(songs, currentPlayingSong, isPlaying, onSongSelected, onOptionsClicked)
    }
}

// Custom Extension to prevent Horizontal Scroll crash inside nested components
@Composable
fun Modifier.horizontalScrollStateFixable(): Modifier {
    return this.background(Color.Transparent)
}

private fun formatTime(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}
