package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.LanguageManager

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    filteredSongs: List<Song>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (List<Song>, Int) -> Unit,
    onOptionsClicked: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        CategoryItem("Rock", Color(0xFFE91E63), Color(0xFFC2185B)),
        CategoryItem("Pop", Color(0xFF9C27B0), Color(0xFF7B1FA2)),
        CategoryItem("Hip-Hop", Color(0xFF2196F3), Color(0xFF1976D2)),
        CategoryItem("Jazz", Color(0xFF009688), Color(0xFF00796B)),
        CategoryItem("Electronic", Color(0xFF4CAF50), Color(0xFF388E3C)),
        CategoryItem("Workout", Color(0xFFFF9800), Color(0xFFF57C00)),
        CategoryItem("Focus", Color(0xFF607D8B), Color(0xFF455A64)),
        CategoryItem("Chill", Color(0xFF795548), Color(0xFF5D4037))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Big Display Title
        Text(
            text = "Search",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Custom Premium Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text(text = "What do you want to listen to?", fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
                // Category mood browse grid
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Browse Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(categories) { category ->
                            CategoryCard(category = category) {
                                onSearchQueryChanged(category.name)
                            }
                        }
                    }
                }
            } else {
                // Real-time matched song list
                if (filteredSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.MusicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching music found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filteredSongs) { index, song ->
                            SongListItem(
                                song = song,
                                isCurrent = song.uri == currentPlayingSong?.uri,
                                isPlaying = isPlaying,
                                onClick = { onSongSelected(filteredSongs, index) },
                                onOptionsClick = { onOptionsClicked(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CategoryItem(val name: String, val startColor: Color, val endColor: Color)

@Composable
fun CategoryCard(category: CategoryItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = listOf(category.startColor, category.endColor)))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = category.name,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.BottomEnd)
        )
    }
}
