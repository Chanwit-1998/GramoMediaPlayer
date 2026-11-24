package com.gramo.mediaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gramo.mediaplayer.ui.components.LyricsEditorDialog
import com.gramo.mediaplayer.ui.screens.DetailsScreen
import com.gramo.mediaplayer.ui.screens.FullPlayerScreen
import com.gramo.mediaplayer.ui.screens.MainLibraryScreen
import com.gramo.mediaplayer.ui.theme.AppBackground
import com.gramo.mediaplayer.ui.theme.DarkIconColor
import com.gramo.mediaplayer.ui.theme.GramoMediaTheme
import com.gramo.mediaplayer.ui.theme.RoyalBlueAccent
import com.gramo.mediaplayer.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GramoMediaTheme {
                GramoAppRoot()
            }
        }
    }
}

@Composable
fun GramoAppRoot(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
        if (hasPermission) {
            viewModel.refreshLibrary()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.refreshLibrary()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    if (!hasPermission) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ต้องการสิทธิ์เข้าถึงไฟล์เพลงและวิดีโอ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkIconColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "แอปทำงานแบบออฟไลน์ 100% กรุณาอนุญาตสิทธิ์เพื่อสแกนไฟล์เพลงและวิดีโอในเครื่องของคุณ",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(requiredPermissions) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent)
                ) {
                    Text("อนุญาตการเข้าถึงสื่อ")
                }
            }
        }
        return
    }

    BackHandler(enabled = uiState.detailsItem != null || uiState.isFullPlayerOpen || uiState.openedAlbum != null) {
        when {
            uiState.detailsItem != null -> viewModel.closeDetails()
            uiState.isFullPlayerOpen -> viewModel.setFullPlayerOpen(false)
            uiState.openedAlbum != null -> viewModel.closeAlbum()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainLibraryScreen(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onToggleSearch = viewModel::toggleSearch,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSetHighlightsFilter = viewModel::setHighlightsFilter,
            onRefresh = viewModel::refreshLibrary,
            onPlayMedia = { item, list -> viewModel.playMedia(item, list, openFullPlayer = true) },
            onTogglePlayPause = viewModel::togglePlayPause,
            onOpenFullPlayer = { viewModel.setFullPlayerOpen(true) },
            onOpenDetails = viewModel::openDetails,
            onOpenAlbum = viewModel::openAlbum,
            onCloseAlbum = viewModel::closeAlbum,
            onSetCustomCoverForAlbum = viewModel::setCustomCoverForAlbum
        )

        val currentItem = uiState.currentItem
        if (uiState.isFullPlayerOpen && currentItem != null) {
            FullPlayerScreen(
                item = currentItem,
                queue = uiState.queue,
                exoPlayer = viewModel.exoPlayer,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                repeatMode = uiState.repeatMode,
                shuffleEnabled = uiState.shuffleEnabled,
                playbackSpeed = uiState.playbackSpeed,
                sleepTimerRemainingSec = uiState.sleepTimerRemainingSec,
                lyricsVisible = uiState.lyricsVisible,
                activeLyricText = uiState.activeLyricText,
                hasLyrics = currentItem.hasLyrics || uiState.parsedLyrics.isNotEmpty(),
                onCollapse = { viewModel.setFullPlayerOpen(false) },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onPrevious = viewModel::playPrevious,
                onNext = { viewModel.playNext(autoAdvance = false) },
                onCycleRepeat = viewModel::cycleRepeatMode,
                onToggleShuffle = viewModel::toggleShuffle,
                onToggleHighlight = { viewModel.toggleHighlight(currentItem) },
                onToggleLyricsVisibility = viewModel::toggleLyricsVisibility,
                onOpenDetails = { viewModel.openDetails(currentItem) },
                onSelectSpeed = viewModel::setPlaybackSpeed,
                onSelectSleepTimer = viewModel::setSleepTimerMinutes,
                onSelectQueueItem = { qItem -> viewModel.playMedia(qItem, uiState.queue, openFullPlayer = true) }
            )
        }

        val detailsItem = uiState.detailsItem
        if (detailsItem != null) {
            DetailsScreen(
                item = detailsItem,
                details = uiState.detailsMetadata,
                isLoading = uiState.isLoadingDetails,
                onBack = viewModel::closeDetails,
                onPickCoverImage = { uri -> viewModel.setCustomCoverForItem(detailsItem, uri) },
                onRemoveCustomCover = { viewModel.removeCustomCoverForItem(detailsItem) },
                onToggleHighlight = { viewModel.toggleHighlight(detailsItem) },
                onOpenLyricsEditor = { viewModel.openLyricsEditor(detailsItem) }
            )
        }

        val editorItem = uiState.lyricsEditorItem
        if (editorItem != null) {
            LyricsEditorDialog(
                item = editorItem,
                rawText = uiState.lyricsEditorRawText,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                onTextChange = viewModel::updateLyricsEditorText,
                onImportFileUri = viewModel::importLyricsFromUri,
                onStartPlaybackForSync = { viewModel.startPlaybackForSync(editorItem) },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSave = { content -> viewModel.saveLyricsForItem(editorItem, content) },
                onDismiss = viewModel::closeLyricsEditor
            )
        }
    }
}
