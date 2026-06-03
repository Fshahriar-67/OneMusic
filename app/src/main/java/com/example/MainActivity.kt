package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.MusicPlaybackManager
import com.example.data.PlaylistEntity
import com.example.data.Song
import com.example.service.FloatingPlayerService
import com.example.ui.MusicViewModel
import com.example.ui.components.*
import com.example.ui.theme.Language
import com.example.ui.theme.LanguageManager
import com.example.ui.theme.MyApplicationTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreenContent(viewModel = viewModel, activity = this)
                }
            }
        }
    }

    fun launchOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(viewModel: MusicViewModel, activity: MainActivity) {
    val context = LocalContext.current

    // Observe DB States
    val songs by viewModel.scannedSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Observe derived dynamic states
    val activeLibraryTab by viewModel.currentLibraryTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val albumGroups by viewModel.albums.collectAsState()
    val artistGroups by viewModel.artists.collectAsState()
    val folderGroups by viewModel.folders.collectAsState()

    // Observe Player states
    val isPlaying by MusicPlaybackManager.isPlaying.collectAsState()
    val currentSong by MusicPlaybackManager.currentSong.collectAsState()
    val progress by MusicPlaybackManager.playbackProgress.collectAsState()
    val duration by MusicPlaybackManager.songDuration.collectAsState()
    val shuffleEnabled by MusicPlaybackManager.shuffleEnabled.collectAsState()
    val repeatMode by MusicPlaybackManager.repeatMode.collectAsState()

    // Observe Settings states
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val timerRemainingMs by viewModel.sleepTimeRemaining.collectAsState()

    // UI Panel trigger flags
    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showPlaylistDialogForSong by remember { mutableStateOf<Song?>(null) }
    var selectedPlaylistDetails by remember { mutableStateOf<PlaylistEntity?>(null) }
    var floatingOverlayEnabled by remember { mutableStateOf(false) }

    // Floating overlay service sync state
    val overlayGranted = remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }
    }

    // Storage permission checks
    val hasStoragePermission = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Multi permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        val granted = map.values.any { it }
        hasStoragePermission.value = granted
        if (granted) {
            viewModel.scanLocalMusic()
        } else {
            Toast.makeText(context, "Storage permission is needed to import offline tracks", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(hasStoragePermission.value) {
        if (hasStoragePermission.value) {
            viewModel.scanLocalMusic()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LanguageManager.getString("app_name"),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Language Switcher Toggle
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Float Overlay popups Switcher
                    IconButton(
                        onClick = {
                            if (overlayGranted.value) {
                                floatingOverlayEnabled = !floatingOverlayEnabled
                                val intent = Intent(context, FloatingPlayerService::class.java)
                                if (floatingOverlayEnabled) {
                                    context.startService(intent)
                                    Toast.makeText(context, "Overlay Capsule enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    context.stopService(intent)
                                }
                            } else {
                                Toast.makeText(context, "Enable overlay permissions to start float pill player", Toast.LENGTH_LONG).show()
                                activity.launchOverlaySettings()
                            }
                        }
                    ) {
                        val overlayActive = floatingOverlayEnabled && overlayGranted.value
                        Icon(
                            imageVector = if (overlayActive) Icons.Filled.SmartButton else Icons.Outlined.SmartButton,
                            contentDescription = "Float Overlay",
                            tint = if (overlayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Theme Selector Toggle
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        val isDark by viewModel.isDarkMode.collectAsState()
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Storage scan trigger
                    IconButton(onClick = { viewModel.scanLocalMusic() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Render beautiful compact MiniPlayer anchored right above bottom navigation
                if (currentSong != null) {
                    val isFavState = remember(currentSong) {
                        derivedStateOf {
                            currentSong?.let { item ->
                                favorites.any { it.songUri == item.uri }
                            } ?: false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            progress = progress,
                            duration = duration,
                            isFavorite = isFavState.value,
                            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                            onPlayPause = { MusicPlaybackManager.playPause() },
                            onNext = { MusicPlaybackManager.skipToNext() },
                            onClick = { showNowPlayingSheet = true }
                        )
                    }
                }

                PremiumBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasStoragePermission.value) {
                // Beautiful user permission onboarding card list
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = LanguageManager.getString("permission"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To access and scan local .mp3, .flac, and audio tracks on device memory, please grant storage permissions.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(permissionsToRequest)
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = LanguageManager.getString("grant"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("home") {
                        HomeScreen(
                            recentlyPlayed = recentlyPlayed,
                            favorites = favorites,
                            scannedSongsCount = songs.size,
                            playlistsCount = playlists.size,
                            currentPlayingSong = currentSong,
                            isPlaying = isPlaying,
                            onSongSelected = { list, idx -> viewModel.playSongFromLibrary(list, idx) },
                            onNavigateToTab = { targetRoute ->
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSleepTimerClicked = { showSleepTimer = true },
                            onEqualizerClicked = { showEqualizer = true }
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            filteredSongs = filteredSongs,
                            currentPlayingSong = currentSong,
                            isPlaying = isPlaying,
                            onSongSelected = { list, idx -> viewModel.playSongFromLibrary(list, idx) },
                            onOptionsClicked = { showPlaylistDialogForSong = it }
                        )
                    }

                    composable("library") {
                        LibraryView(
                            songs = filteredSongs,
                            isScanning = isScanning,
                            currentTab = activeLibraryTab,
                            searchQuery = searchQuery,
                            playlists = playlists,
                            recentlyPlayed = recentlyPlayed,
                            favorites = favorites,
                            currentPlayingSong = currentSong,
                            isPlaying = isPlaying,
                            albumGroups = albumGroups,
                            artistGroups = artistGroups,
                            folderGroups = folderGroups,
                            onTabSelected = { viewModel.setLibraryTab(it) },
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onSongSelected = { list, idx -> viewModel.playSongFromLibrary(list, idx) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOptionsClicked = { showPlaylistDialogForSong = it },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onScanRequest = { viewModel.scanLocalMusic() },
                            onPlaylistSelected = { selectedPlaylistDetails = it }
                        )
                    }

                    composable("playlists") {
                        PlaylistsScreen(
                            playlists = playlists,
                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlaylistSelected = { selectedPlaylistDetails = it }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { viewModel.toggleTheme() },
                            currentLanguage = currentLanguage,
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            floatingOverlayEnabled = floatingOverlayEnabled,
                            onToggleFloatingOverlay = {
                                if (overlayGranted.value) {
                                    floatingOverlayEnabled = !floatingOverlayEnabled
                                    val intent = Intent(context, FloatingPlayerService::class.java)
                                    if (floatingOverlayEnabled) {
                                        context.startService(intent)
                                        Toast.makeText(context, "Overlay Capsule enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.stopService(intent)
                                    }
                                } else {
                                    Toast.makeText(context, "Enable overlay permissions to start float pill player", Toast.LENGTH_LONG).show()
                                    activity.launchOverlaySettings()
                                }
                            },
                            sleepTimeRemainingMs = timerRemainingMs,
                            onSleepTimerClicked = { showSleepTimer = true },
                            onScanRequest = { viewModel.scanLocalMusic() },
                            tracksCount = songs.size
                        )
                    }
                }
            }

            // Playlist Details Viewer Sliding Subsheet
            selectedPlaylistDetails?.let { playlist ->
                val playlistSongsFlow = remember(playlist.id) { viewModel.getPlaylistSongsFlow(playlist.id) }
                val playlistTracks by playlistSongsFlow.collectAsState(initial = emptyList())

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedPlaylistDetails = null }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (playlistTracks.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No tracks added to this playlist. Search songs and click options to append!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                        .fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                itemsIndexed(playlistTracks) { index, song ->
                                    val isCurrent = song.uri == currentSong?.uri
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable { viewModel.playSongFromLibrary(playlistTracks, index) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = song.artist,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.uri) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expanded NowPlaying sheets overlay
            AnimatedVisibility(
                visible = showNowPlayingSheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val isFavState = remember(currentSong) {
                    derivedStateOf {
                        currentSong?.let { item ->
                            favorites.any { it.songUri == item.uri }
                        } ?: false
                    }
                }

                NowPlayingScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    duration = duration,
                    isFavorite = isFavState.value,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                    onPlayPause = { MusicPlaybackManager.playPause() },
                    onNext = { MusicPlaybackManager.skipToNext() },
                    onPrev = { MusicPlaybackManager.skipToPrevious() },
                    onSeek = { MusicPlaybackManager.seekTo(it) },
                    onToggleShuffle = { MusicPlaybackManager.toggleShuffle() },
                    onToggleRepeat = { MusicPlaybackManager.toggleRepeat() },
                    onOpenEqualizer = { showEqualizer = true },
                    onOpenSleepTimer = { showSleepTimer = true },
                    onMinimize = { showNowPlayingSheet = false }
                )
            }

            // Interactive Equalizer Slider Sheet popup
            if (showEqualizer) {
                Dialog(onDismissRequest = { showEqualizer = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        EqualizerScreen(onDismiss = { showEqualizer = false })
                    }
                }
            }

            // Sleep Timer countdown chooser popup
            if (showSleepTimer) {
                SleepTimerDialog(
                    activeRemainingMs = timerRemainingMs,
                    onStartTimer = { viewModel.startSleepTimer(it) },
                    onStopTimer = { viewModel.stopSleepTimer() },
                    onDismiss = { showSleepTimer = false }
                )
            }

            // Playlist select insertion popup dialog
            showPlaylistDialogForSong?.let { songToAppend ->
                PlaylistDialog(
                    song = songToAppend,
                    playlists = playlists,
                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                    onAddSongToPlaylist = { plId, track ->
                        viewModel.addSongToPlaylist(plId, track)
                        Toast.makeText(context, LanguageManager.getString("added_to_playlist"), Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showPlaylistDialogForSong = null }
                )
            }
        }
    }
}

@Composable
fun PremiumBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val items = listOf(
            BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
            BottomNavItem("playlists", "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic),
            BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
)
