package com.gramo.mediaplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gramo.mediaplayer.data.MediaRepository
import com.gramo.mediaplayer.model.AlbumGroup
import com.gramo.mediaplayer.model.MainTab
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.model.MediaType
import com.gramo.mediaplayer.ui.components.MediaArtwork
import com.gramo.mediaplayer.ui.theme.*
import com.gramo.mediaplayer.viewmodel.PlayerUiState

@Composable
fun MainLibraryScreen(
    uiState: PlayerUiState,
    onSelectTab: (MainTab) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleHighlightsFilter: () -> Unit,
    onSetHighlightsFilter: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPlayMedia: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenDetails: (MediaItemModel) -> Unit,
    onToggleHighlight: (MediaItemModel) -> Unit,
    onSetCustomCoverForItem: (MediaItemModel, Uri) -> Unit,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onCloseAlbum: () -> Unit,
    onSetCustomCoverForAlbum: (AlbumGroup, Uri) -> Unit
) {
    var pendingCoverTargetItem by remember { mutableStateOf<MediaItemModel?>(null) }
    var pendingCoverTargetAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }

    val itemCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingCoverTargetItem
        if (uri != null && target != null) {
            onSetCustomCoverForItem(target, uri)
        }
        pendingCoverTargetItem = null
    }

    val albumCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingCoverTargetAlbum
        if (uri != null && target != null) {
            onSetCustomCoverForAlbum(target, uri)
        }
        pendingCoverTargetAlbum = null
    }

    val query = uiState.searchQuery.trim().lowercase()

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (uiState.currentItem != null) {
                MiniPlayerBar(
                    item = uiState.currentItem,
                    isPlaying = uiState.isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onClickBar = onOpenFullPlayer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top bar: Search & Menu icons on right (matching 685389.jpg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isSearchActive) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("ค้นหาเพลง, วิดีโอ, อัลบั้ม...", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                }

                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = if (uiState.isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "ค้นหา",
                        tint = DarkIconColor,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "ตัวเลือกเพิ่มเติม",
                            tint = DarkIconColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ไฮไลท์ของฉัน (My Highlights)") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = RoyalBlueAccent)
                            },
                            onClick = {
                                showTopMenu = false
                                onSetHighlightsFilter(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("สแกนไฟล์ในเครื่องใหม่") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                            },
                            onClick = {
                                showTopMenu = false
                                onRefresh()
                            }
                        )
                    }
                }
            }

            // Large App Title (matching 685389.jpg)
            Text(
                text = "แกรโมโฟน",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = DarkIconColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ONLY 4 TABS in pill style (matching 685389.jpg red box):
            // 1. เพลง | 2. วิดีโอ | 3. อัลบั้มเพลง | 4. อัลบั้มวิดีโอ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = !uiState.showOnlyHighlights && uiState.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) SelectedPillBg else Color.Transparent)
                            .clickable {
                                onSetHighlightsFilter(false)
                                onSelectTab(tab)
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) DarkIconColor else MutedTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-header bar: Item count + My Highlights toggle chip + Play All / Shuffle
            val activeItemsForCount: List<MediaItemModel> = when {
                uiState.showOnlyHighlights -> (uiState.musicList + uiState.videoList).filter { it.isHighlighted }
                uiState.openedAlbum != null -> uiState.openedAlbum.items
                uiState.selectedTab == MainTab.MUSIC -> uiState.musicList
                uiState.selectedTab == MainTab.VIDEO -> uiState.videoList
                uiState.selectedTab == MainTab.MUSIC_ALBUMS -> uiState.musicList
                MainTab.VIDEO_ALBUMS -> uiState.videoList
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.openedAlbum != null) {
                        IconButton(
                            onClick = onCloseAlbum,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "กลับ",
                                tint = DarkIconColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.openedAlbum.albumName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkIconColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val countText = when {
                            uiState.showOnlyHighlights -> "ไฮไลท์ของฉัน (${activeItemsForCount.size})"
                            uiState.selectedTab == MainTab.MUSIC -> "${uiState.musicList.size} เพลง"
                            uiState.selectedTab == MainTab.VIDEO -> "${uiState.videoList.size} วิดีโอ"
                            uiState.selectedTab == MainTab.MUSIC_ALBUMS -> "${uiState.musicAlbums.size} อัลบั้มเพลง"
                            else -> "${uiState.videoAlbums.size} อัลบั้มวิดีโอ"
                        }
                        Text(
                            text = countText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkIconColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // My Highlights Filter Chip ("ไฮไลท์ของฉัน")
                    FilterChip(
                        selected = uiState.showOnlyHighlights,
                        onClick = onToggleHighlightsFilter,
                        label = { Text("ไฮไลท์ของฉัน", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.showOnlyHighlights) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = RoyalBlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Play All button
                    IconButton(
                        onClick = {
                            if (activeItemsForCount.isNotEmpty()) {
                                onPlayMedia(activeItemsForCount.first(), activeItemsForCount)
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "เล่นทั้งหมด",
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Shuffle Play button
                    IconButton(
                        onClick = {
                            if (activeItemsForCount.isNotEmpty()) {
                                val shuffled = activeItemsForCount.shuffled()
                                onPlayMedia(shuffled.first(), shuffled)
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "สุ่มเล่น",
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoyalBlueAccent)
                }
            } else if (uiState.showOnlyHighlights) {
                // MY HIGHLIGHTS VIEW: Shows highlighted Music & Videos + quick "Add Album Cover Art" button!
                val highlightedItems = (uiState.musicList + uiState.videoList)
                    .filter { it.isHighlighted }
                    .filter { query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) }

                if (highlightedItems.isEmpty()) {
                    EmptyStateMessage(
                        title = "ยังไม่มีรายการในไฮไลท์ของฉัน",
                        subtitle = "กดปุ่มหัวใจ หรือกดเพิ่มปกอัลบั้มในหน้ารายละเอียดของเพลงหรือวิดีโอ เพื่อแสดงที่นี่"
                    )
                } else {
                    MediaItemLazyList(
                        items = highlightedItems,
                        showAddCoverShortcut = true,
                        onPlay = { item -> onPlayMedia(item, highlightedItems) },
                        onOpenDetails = onOpenDetails,
                        onToggleHighlight = onToggleHighlight,
                        onAddCustomCover = { item ->
                            pendingCoverTargetItem = item
                            itemCoverPicker.launch("image/*")
                        }
                    )
                }
            } else if (uiState.openedAlbum != null) {
                val albumItems = uiState.openedAlbum.items.filter {
                    query.isEmpty() || it.title.lowercase().contains(query)
                }
                MediaItemLazyList(
                    items = albumItems,
                    showAddCoverShortcut = true,
                    onPlay = { item -> onPlayMedia(item, albumItems) },
                    onOpenDetails = onOpenDetails,
                    onToggleHighlight = onToggleHighlight,
                    onAddCustomCover = { item ->
                        pendingCoverTargetItem = item
                        itemCoverPicker.launch("image/*")
                    }
                )
            } else {
                when (uiState.selectedTab) {
                    MainTab.MUSIC -> {
                        val filtered = uiState.musicList.filter {
                            query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateMessage("ไม่พบไฟล์เพลงในเครื่อง", "ลองตรวจสอบไฟล์เสียงในโฟลเดอร์ Music หรือ Download")
                        } else {
                            MediaItemLazyList(
                                items = filtered,
                                showAddCoverShortcut = false,
                                onPlay = { item -> onPlayMedia(item, filtered) },
                                onOpenDetails = onOpenDetails,
                                onToggleHighlight = onToggleHighlight,
                                onAddCustomCover = { item ->
                                    pendingCoverTargetItem = item
                                    itemCoverPicker.launch("image/*")
                                }
                            )
                        }
                    }

                    MainTab.VIDEO -> {
                        val filtered = uiState.videoList.filter {
                            query.isEmpty() || it.title.lowercase().contains(query) || it.album.lowercase().contains(query)
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateMessage("ไม่พบไฟล์วิดีโอในเครื่อง", "ลองตรวจสอบไฟล์ MP4 / MKV ในโฟลเดอร์ Movies หรือ Download")
                        } else {
                            MediaItemLazyList(
                                items = filtered,
                                showAddCoverShortcut = false,
                                onPlay = { item -> onPlayMedia(item, filtered) },
                                onOpenDetails = onOpenDetails,
                                onToggleHighlight = onToggleHighlight,
                                onAddCustomCover = { item ->
                                    pendingCoverTargetItem = item
                                    itemCoverPicker.launch("image/*")
                                }
                            )
                        }
                    }

                    MainTab.MUSIC_ALBUMS -> {
                        val filteredAlbums = uiState.musicAlbums.filter {
                            query.isEmpty() || it.albumName.lowercase().contains(query)
                        }
                        AlbumGridList(
                            albums = filteredAlbums,
                            onOpenAlbum = onOpenAlbum,
                            onAddAlbumCover = { album ->
                                pendingCoverTargetAlbum = album
                                albumCoverPicker.launch("image/*")
                            }
                        )
                    }

                    MainTab.VIDEO_ALBUMS -> {
                        val filteredAlbums = uiState.videoAlbums.filter {
                            query.isEmpty() || it.albumName.lowercase().contains(query)
                        }
                        AlbumGridList(
                            albums = filteredAlbums,
                            onOpenAlbum = onOpenAlbum,
                            onAddAlbumCover = { album ->
                                pendingCoverTargetAlbum = album
                                albumCoverPicker.launch("image/*")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemLazyList(
    items: List<MediaItemModel>,
    showAddCoverShortcut: Boolean,
    onPlay: (MediaItemModel) -> Unit,
    onOpenDetails: (MediaItemModel) -> Unit,
    onToggleHighlight: (MediaItemModel) -> Unit,
    onAddCustomCover: (MediaItemModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items, key = { "${it.type}_${it.id}" }) { item ->
            var showItemMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(item) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    coverUri = item.activeCoverUri,
                    mediaType = item.type,
                    modifier = Modifier.size(54.dp),
                    cornerRadius = 10.dp,
                    discIconFraction = 0.50f,
                    showVideoBadge = item.type == MediaType.VIDEO
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = DarkIconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${item.artist} • ${MediaRepository.formatDuration(item.durationMs)}",
                        fontSize = 13.sp,
                        color = MutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showAddCoverShortcut) {
                    IconButton(onClick = { onAddCustomCover(item) }) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "เพิ่มปกอัลบั้ม",
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showItemMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "ตัวเลือก",
                            tint = MutedTextColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("รายละเอียด") },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                            onClick = {
                                showItemMenu = false
                                onOpenDetails(item)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("เพิ่ม/เปลี่ยนปกอัลบั้ม") },
                            leadingIcon = { Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null) },
                            onClick = {
                                showItemMenu = false
                                onAddCustomCover(item)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (item.isHighlighted) "นำออกจากไฮไลท์ของฉัน" else "เพิ่มในไฮไลท์ของฉัน")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (item.isHighlighted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = RoyalBlueAccent
                                )
                            },
                            onClick = {
                                showItemMenu = false
                                onToggleHighlight(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGridList(
    albums: List<AlbumGroup>,
    onOpenAlbum: (AlbumGroup) -> Unit,
    onAddAlbumCover: (AlbumGroup) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyStateMessage("ไม่พบอัลบั้ม", "ยังไม่มีข้อมูลอัลบั้มสำหรับหมวดหมู่นี้")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { "${it.type}_${it.albumId}" }) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(album) }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    MediaArtwork(
                        coverUri = album.coverUri,
                        mediaType = album.type,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        cornerRadius = 16.dp,
                        discIconFraction = 0.45f
                    )
                    IconButton(
                        onClick = { onAddAlbumCover(album) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppBackground.copy(alpha = 0.88f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "เปลี่ยนปกอัลบั้ม",
                            tint = RoyalBlueAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = album.albumName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkIconColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.subtitle,
                    fontSize = 12.sp,
                    color = MutedTextColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    item: MediaItemModel,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onClickBar: () -> Unit
) {
    Surface(
        color = SelectedPillBg,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickBar() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                coverUri = item.activeCoverUri,
                mediaType = item.type,
                modifier = Modifier.size(46.dp),
                cornerRadius = 10.dp,
                showVideoBadge = item.type == MediaType.VIDEO
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlueAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artist,
                    fontSize = 12.sp,
                    color = MutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "เล่น/หยุด",
                    tint = DarkIconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkIconColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MutedTextColor
        )
    }
}
