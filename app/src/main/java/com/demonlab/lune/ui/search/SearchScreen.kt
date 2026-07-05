package com.demonlab.lune.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.demonlab.lune.R
import com.demonlab.lune.data.Playlist
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.tools.SettingsManager
import com.demonlab.lune.tools.Song
import com.demonlab.lune.ui.components.SongItem
import com.demonlab.lune.ui.data.Album
import com.demonlab.lune.ui.playlist.PlaylistPreviewCovers
import com.demonlab.lune.ui.viewmodels.MusicViewModel

data class SearchResults(
    val songs: List<Song>,
    val favoriteSongs: List<Song>,
    val albumResults: Map<Album, List<Song>>,
    val playlistResults: Map<Playlist, List<Song>>,
    val tagResults: Map<String, List<Song>>,
    val realAlbumResults: Map<Album, List<Song>> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    allFolders: List<String>,
    onDismiss: () -> Unit,
    onSongClick: (Song, List<Song>, String, Long) -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onNavigateToAlbum: (Album) -> Unit,
    onNavigateToPlaylist: (Playlist) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onOptionsClick: (Song) -> Unit,
    currentlyPlayingId: Long?,
    activeCategory: String?,
    activePlaylistId: Long?,
    onFavoriteClick: ((Song) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val pm = PlaybackManager.getInstance(context)
    val settings = remember { SettingsManager.getInstance(context) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterSongs by remember { mutableStateOf(true) }
    var filterPlaylists by remember { mutableStateOf(true) }
    var filterAlbums by remember { mutableStateOf(true) }
    var filterArtists by remember { mutableStateOf(true) }
    var filterFolders by remember { mutableStateOf(true) }
    var filterFavorites by remember { mutableStateOf(true) }
    val isSearchActive = activePlaylistId == -300L
    var searchShuffle by remember { mutableStateOf(settings.getPlaylistShuffle(-300L)) }

    if (settings.isSectionCustomizationEnabled) {
        val hiddenTabs = settings.hiddenSectionTabs
        if ("ALL" in hiddenTabs) filterSongs = false
        if ("FAVORITES" in hiddenTabs) filterFavorites = false
        if ("PLAYLISTS" in hiddenTabs) filterPlaylists = false
        if ("ALBUMS" in hiddenTabs) { filterAlbums = false; filterArtists = false }
        if ("FOLDERS" in hiddenTabs) filterFolders = false
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchShuffle = pm.isShuffle
    }

    val isPlaying = pm.isPlaying
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val allSongs = remember(viewModel.filteredSongs, allFolders) {
        viewModel.filteredSongs.filter { song -> 
            allFolders.contains(song.folderName) 
        }
    }
    val allAlbums = remember(allSongs) {
        allSongs.groupBy { it.artist ?: "Desconocido" }
            .map { (artistName, songs) -> 
                Album(
                    id = artistName.hashCode().toLong(), 
                    name = artistName, 
                    artist = "", 
                    albumArtUri = songs.first().albumArtUri, 
                    coverUrl = songs.first().coverUrl, 
                    songs = songs.sortedWith(compareBy({ it.album }, { it.title }))
                )
            }.sortedBy { it.name }
    }
    val allRealAlbums = remember(allSongs) {
        allSongs.groupBy { it.album }
            .map { (albumName, songs) ->
                Album(
                    id = albumName.hashCode().toLong(),
                    name = albumName,
                    artist = songs.first().artist,
                    albumArtUri = songs.first().albumArtUri,
                    coverUrl = songs.first().coverUrl,
                    songs = songs.sortedBy { it.title }
                )
            }.sortedBy { it.name }
    }
    val allPlaylists = viewModel.playlists
    val playlistMappings = viewModel.playlistMappings

    val sTabAll = stringResource(R.string.tab_all)
    val sTabFavorites = stringResource(R.string.tab_favorites)
    val sTabAlbums = stringResource(R.string.tab_albums)
    val sTabAlbumsReal = stringResource(R.string.tab_albums_real)
    val sPlaylists = stringResource(R.string.playlists)
    val sFilterTitle = stringResource(R.string.search_filter)
    val sFilterAll = stringResource(R.string.search_filter_all)
    val sFilterPlaylist = stringResource(R.string.search_filter_playlist)
    val sFilterAlbum = stringResource(R.string.search_filter_album)
    val sFilterArtist = stringResource(R.string.search_filter_artist)
    val sFilterFolder = stringResource(R.string.search_filter_folder)
    val sFilterCancel = stringResource(R.string.search_filter_cancel)

    val searchResults = remember(query, allSongs, allAlbums, allRealAlbums, allPlaylists, allFolders, sTabFavorites, playlistMappings) {
        if (query.isBlank()) return@remember SearchResults(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())
        
        val searchTerms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        
        val matchedSongs = allSongs.filter { song ->
            val searchTarget = "${song.title} ${song.artist} ${song.album}".lowercase()
            searchTerms.all { term -> searchTarget.contains(term) }
        }

        val albumResults = mutableMapOf<Album, List<Song>>()
        allAlbums.forEach { album ->
            val nameMatches = searchTerms.all { term -> album.name.lowercase().contains(term) }
            val matchingSongs = album.songs.filter { song ->
                val searchTarget = "${song.title} ${song.artist} ${song.album}".lowercase()
                searchTerms.all { term -> searchTarget.contains(term) }
            }
            if (nameMatches || matchingSongs.isNotEmpty()) {
                albumResults[album] = matchingSongs
            }
        }

        val realAlbumResults = mutableMapOf<Album, List<Song>>()
        allRealAlbums.forEach { album ->
            val nameMatches = searchTerms.all { term -> album.name.lowercase().contains(term) }
            val matchingSongs = album.songs.filter { song ->
                val searchTarget = "${song.title} ${song.artist} ${song.album}".lowercase()
                searchTerms.all { term -> searchTarget.contains(term) }
            }
            if (nameMatches || matchingSongs.isNotEmpty()) {
                realAlbumResults[album] = matchingSongs
            }
        }

        val playlistResults = mutableMapOf<Playlist, List<Song>>()
        allPlaylists.forEach { playlist ->
            val nameMatches = searchTerms.all { term -> playlist.name.lowercase().contains(term) }
            
            val playlistSongs = playlistMappings.filter { it.playlistId == playlist.id }
                .mapNotNull { mapping -> allSongs.find { it.id == mapping.songId } }
                
            val matchingSongs = playlistSongs.filter { song ->
                val searchTarget = "${song.title} ${song.artist} ${song.album}".lowercase()
                searchTerms.all { term -> searchTarget.contains(term) }
            }
            
            if (nameMatches || matchingSongs.isNotEmpty()) {
                playlistResults[playlist] = matchingSongs
            }
        }

        val favoriteSongs = matchedSongs.filter { it.isFavorite }

        val tagResults = mutableMapOf<String, List<Song>>()
        
        if (favoriteSongs.isNotEmpty()) {
            tagResults[sTabFavorites] = favoriteSongs
        }

        allFolders.forEach { folder ->
            if (folder.lowercase() == sTabFavorites.lowercase() || folder.lowercase() == "favorites" || folder.lowercase() == "favoritos") {
                return@forEach
            }
            val nameMatches = searchTerms.all { term -> folder.lowercase().contains(term) }
            if (nameMatches) {
                val folderSongs = allSongs.filter { it.folderName == folder && matchedSongs.contains(it) }
                tagResults[folder] = folderSongs
            }
        }

        matchedSongs.forEach { song ->
            val folder = song.folderName ?: "Desconocido"
            if (allFolders.contains(folder) && !tagResults.containsKey(folder)) {
                val folderSongs = allSongs.filter { it.folderName == folder && matchedSongs.contains(it) }
                tagResults[folder] = folderSongs
            }
        }

        SearchResults(matchedSongs, favoriteSongs, albumResults, playlistResults, tagResults, realAlbumResults)
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (filterSongs && searchResults.songs.isNotEmpty()) {
                item {
                    Text(
                        text = sTabAll,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToFolder("ALL") }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }
                if (searchResults.songs.size > 1) {
                    item {
                        val pm = PlaybackManager.getInstance(LocalContext.current)
                        val btnHeight = 52.dp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    onClick = { onPlayAll(searchResults.songs, searchShuffle) },
                                    shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(btnHeight)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.search_play_all),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    onClick = {
                                        if (isSearchActive) {
                                            pm.toggleShuffle()
                                            searchShuffle = pm.isShuffle
                                        } else {
                                            val newState = !searchShuffle
                                            searchShuffle = newState
                                            settings.setPlaylistShuffle(-300L, newState)
                                        }
                                    },
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 26.dp, bottomEnd = 26.dp),
                                    color = if (searchShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(btnHeight)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.search_shuffle),
                                                color = if (searchShuffle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                itemsIndexed(searchResults.songs) { index, song ->
                    val isFirst = index == 0
                    val isLast = index == searchResults.songs.lastIndex
                    val isCurrent = song.id == currentlyPlayingId && activeCategory == "ALL"
                    SongItem(
                        isFirst = isFirst,
                        isLast = isLast,
                        song = song,
                        currentlyPlaying = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSongClick(song, allSongs, "ALL", -300L) },
                        onOptionsClick = { onOptionsClick(song) },
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }

            if (filterFavorites) searchResults.tagResults[sTabFavorites]?.let { songs ->
                if (songs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sTabFavorites,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToFolder("FAVORITES") }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    itemsIndexed(songs) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == songs.lastIndex
                        val isCurrent = song.id == currentlyPlayingId && activeCategory == "FAVORITES"
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song,
                            currentlyPlaying = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onSongClick(song, searchResults.favoriteSongs, "FAVORITES", -200L) },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text(sTabFavorites) },
                            leadingContent = { 
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            modifier = Modifier.clickable { onNavigateToFolder("FAVORITES") }
                        )
                    }
                }
            }

            if (filterPlaylists && searchResults.playlistResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.playlists),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val playlistList = searchResults.playlistResults.toList()
                if (playlistList.isNotEmpty()) {
                    for ((playlist, matchingSongs) in playlistList) {
                        item {
                            val context = LocalContext.current
                            ListItem(
                                headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    PlaylistPreviewCovers(playlistId = playlist.id, viewModel = viewModel, size = 50.dp)
                                },
                                modifier = Modifier.clickable { onNavigateToPlaylist(playlist) }
                            )
                        }
                        itemsIndexed(matchingSongs) { index, song ->
                            val isFirst = index == 0
                            val isLast = index == matchingSongs.lastIndex
                            val isCurrent = song.id == currentlyPlayingId && activeCategory == "PLAYLISTS" && activePlaylistId == playlist.id
                            SongItem(
                                isFirst = isFirst,
                                isLast = isLast,
                                song = song,
                                currentlyPlaying = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                modifier = Modifier.padding(start = 32.dp),
                                onClick = { 
                                    val fullPlaylistSongs = playlistMappings.filter { it.playlistId == playlist.id }
                                        .mapNotNull { mapping -> allSongs.find { s -> s.id == mapping.songId } }
                                    onSongClick(song, fullPlaylistSongs, "PLAYLISTS", playlist.id) 
                                    onNavigateToPlaylist(playlist)
                                },
                                onOptionsClick = { onOptionsClick(song) },
                                onFavoriteClick = onFavoriteClick
                            )
                        }
                    }
                }
            }

            if (filterAlbums && searchResults.realAlbumResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sTabAlbumsReal,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val realAlbumList = searchResults.realAlbumResults.toList()
                if (realAlbumList.isNotEmpty()) {
                    for ((album, matchingSongs) in realAlbumList) {
                        item {
                            val context = LocalContext.current
                            ListItem(
                                headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    AsyncImage(
                                        model = album.songs.firstOrNull()?.let { it.coverUrl ?: it.uri } ?: R.drawable.ic_launcher_foreground,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                modifier = Modifier.clickable { onNavigateToAlbum(album) }
                            )
                        }
                        itemsIndexed(matchingSongs) { index, song ->
                            val isFirst = index == 0
                            val isLast = index == matchingSongs.lastIndex
                            val isCurrent = song.id == currentlyPlayingId && activeCategory == "ALBUMS" && activePlaylistId == album.id
                            SongItem(
                                isFirst = isFirst,
                                isLast = isLast,
                                song = song,
                                currentlyPlaying = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                modifier = Modifier.padding(start = 32.dp),
                                onClick = { 
                                    onSongClick(song, album.songs, "ALBUMS", album.id)
                                    onNavigateToAlbum(album)
                                },
                                onOptionsClick = { onOptionsClick(song) },
                                onFavoriteClick = onFavoriteClick
                            )
                        }
                    }
                }
            }

            if (filterArtists && searchResults.albumResults.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tab_albums),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val albumList = searchResults.albumResults.toList()
                if (albumList.isNotEmpty()) {
                    for ((album, matchingSongs) in albumList) {
                        item {
                            val context = LocalContext.current
                            ListItem(
                                headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("${album.songs.size} canciones") },
                                leadingContent = {
                                    AsyncImage(
                                        model = album.songs.firstOrNull()?.let { it.coverUrl ?: it.uri } ?: R.drawable.ic_launcher_foreground,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                modifier = Modifier.clickable { onNavigateToAlbum(album) }
                            )
                        }
                        itemsIndexed(matchingSongs) { index, song ->
                            val isFirst = index == 0
                            val isLast = index == matchingSongs.lastIndex
                            val isCurrent = song.id == currentlyPlayingId && activeCategory == "ALBUMS" && activePlaylistId == album.id
                            SongItem(
                                isFirst = isFirst,
                                isLast = isLast,
                                song = song,
                                currentlyPlaying = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                modifier = Modifier.padding(start = 32.dp),
                                onClick = { 
                                    onSongClick(song, album.songs, "ALBUMS", album.id)
                                    onNavigateToAlbum(album)
                                },
                                onOptionsClick = { onOptionsClick(song) },
                                onFavoriteClick = onFavoriteClick
                            )
                        }
                    }
                }
            }

            if (filterFolders) {
            searchResults.tagResults.filterKeys { it != sTabFavorites }.forEach { (tagName, songs) ->
                if (songs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tagName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToFolder(tagName) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    itemsIndexed(songs) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == songs.lastIndex
                        val isCurrent = song.id == currentlyPlayingId && activeCategory == "FOLDERS"
                        SongItem(
                            isFirst = isFirst,
                            isLast = isLast,
                            song = song,
                            currentlyPlaying = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = {
                                onSongClick(song, songs, "FOLDERS", tagName.hashCode().toLong())
                                onNavigateToFolder(tagName)
                            },
                            onOptionsClick = { onOptionsClick(song) },
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text(tagName) },
                            leadingContent = { 
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            modifier = Modifier.clickable { onNavigateToFolder(tagName) }
                        )
                    }
                }
            }
            }
            
            if (query.isNotBlank() && (!filterSongs || searchResults.songs.isEmpty()) && (!filterPlaylists || searchResults.playlistResults.isEmpty()) && (!filterAlbums || searchResults.realAlbumResults.isEmpty()) && (!filterArtists || searchResults.albumResults.isEmpty()) && (!filterFolders || searchResults.tagResults.filterKeys { it != sTabFavorites }.all { (_, songs) -> songs.isEmpty() })) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        val sectionCustomizationEnabled = settings.isSectionCustomizationEnabled
        val hiddenTabs = settings.hiddenSectionTabs
        val isAllDisabled = sectionCustomizationEnabled && "ALL" in hiddenTabs
        val isPlaylistsDisabled = sectionCustomizationEnabled && "PLAYLISTS" in hiddenTabs
        val isAlbumsDisabled = sectionCustomizationEnabled && "ALBUMS" in hiddenTabs
        val isArtistsDisabled = sectionCustomizationEnabled && "ALBUMS" in hiddenTabs
        val isFoldersDisabled = sectionCustomizationEnabled && "FOLDERS" in hiddenTabs

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text(sFilterTitle) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterSongs, enabled = !isAllDisabled, onCheckedChange = { if (!isAllDisabled) filterSongs = it })
                        Spacer(Modifier.width(8.dp))
                        Text(sFilterAll, modifier = Modifier.clickable { if (!isAllDisabled) filterSongs = !filterSongs }, color = if (isAllDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Unspecified)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterPlaylists, enabled = !isPlaylistsDisabled, onCheckedChange = { if (!isPlaylistsDisabled) filterPlaylists = it })
                        Spacer(Modifier.width(8.dp))
                        Text(sFilterPlaylist, modifier = Modifier.clickable { if (!isPlaylistsDisabled) filterPlaylists = !filterPlaylists }, color = if (isPlaylistsDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Unspecified)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterAlbums, enabled = !isAlbumsDisabled, onCheckedChange = { if (!isAlbumsDisabled) filterAlbums = it })
                        Spacer(Modifier.width(8.dp))
                        Text(sFilterAlbum, modifier = Modifier.clickable { if (!isAlbumsDisabled) filterAlbums = !filterAlbums }, color = if (isAlbumsDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Unspecified)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterArtists, enabled = !isArtistsDisabled, onCheckedChange = { if (!isArtistsDisabled) filterArtists = it })
                        Spacer(Modifier.width(8.dp))
                        Text(sFilterArtist, modifier = Modifier.clickable { if (!isArtistsDisabled) filterArtists = !filterArtists }, color = if (isArtistsDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Unspecified)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterFolders, enabled = !isFoldersDisabled, onCheckedChange = { if (!isFoldersDisabled) filterFolders = it })
                        Spacer(Modifier.width(8.dp))
                        Text(sFilterFolder, modifier = Modifier.clickable { if (!isFoldersDisabled) filterFolders = !filterFolders }, color = if (isFoldersDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Unspecified)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    filterSongs = true
                    filterPlaylists = true
                    filterAlbums = true
                    filterArtists = true
                    filterFolders = true
                }) {
                    Text(sFilterAll)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text(sFilterCancel)
                }
            }
        )
    }
}
