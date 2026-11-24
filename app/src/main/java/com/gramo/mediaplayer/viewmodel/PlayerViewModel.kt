package com.gramo.mediaplayer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.gramo.mediaplayer.data.LocalPrefsManager
import com.gramo.mediaplayer.data.MediaRepository
import com.gramo.mediaplayer.model.AlbumGroup
import com.gramo.mediaplayer.model.MainTab
import com.gramo.mediaplayer.model.MediaDetails
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.model.MediaType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val selectedTab: MainTab = MainTab.MUSIC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showOnlyHighlights: Boolean = false,
    val isLoading: Boolean = false,

    val musicList: List<MediaItemModel> = emptyList(),
    val videoList: List<MediaItemModel> = emptyList(),
    val musicAlbums: List<AlbumGroup> = emptyList(),
    val videoAlbums: List<AlbumGroup> = emptyList(),

    // Selected album drill-down
    val openedAlbum: AlbumGroup? = null,

    // Active playback state
    val queue: List<MediaItemModel> = emptyList(),
    val currentIndex: Int = -1,
    val currentItem: MediaItemModel? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSec: Int = 0,

    // Screen overlays
    val isFullPlayerOpen: Boolean = false,
    val detailsItem: MediaItemModel? = null,
    val detailsMetadata: MediaDetails? = null,
    val isLoadingDetails: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = LocalPrefsManager(application)
    private val repository = MediaRepository(application, prefsManager)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _uiState.update { it.copy(durationMs = dur) }
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext(autoAdvance = true)
                }
            }
        })

        startPositionTracker()
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _uiState.update {
                        it.copy(
                            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            durationMs = exoPlayer.duration.coerceAtLeast(it.durationMs)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val music = repository.loadAllAudio()
            val videos = repository.loadAllVideos()

            val mAlbums = music.groupBy { it.albumId to it.album }
                .map { (key, items) ->
                    AlbumGroup(
                        albumId = key.first,
                        albumName = key.second,
                        subtitle = "${items.size} เพลง",
                        type = MediaType.MUSIC,
                        items = items,
                        customCoverPath = prefsManager.getCustomCoverPath("ALBUM_MUSIC", key.first)
                    )
                }
                .sortedBy { it.albumName.lowercase() }

            val vAlbums = videos.groupBy { it.albumId to it.album }
                .map { (key, items) ->
                    AlbumGroup(
                        albumId = key.first,
                        albumName = key.second,
                        subtitle = "${items.size} วิดีโอ",
                        type = MediaType.VIDEO,
                        items = items,
                        customCoverPath = prefsManager.getCustomCoverPath("ALBUM_VIDEO", key.first)
                    )
                }
                .sortedBy { it.albumName.lowercase() }

            _uiState.update { state ->
                val updatedCurrent = state.currentItem?.let { cur ->
                    (if (cur.type == MediaType.MUSIC) music else videos).find { it.id == cur.id } ?: cur
                }
                val updatedDetails = state.detailsItem?.let { det ->
                    (if (det.type == MediaType.MUSIC) music else videos).find { it.id == det.id } ?: det
                }
                val updatedOpenedAlbum = state.openedAlbum?.let { opened ->
                    (if (opened.type == MediaType.MUSIC) mAlbums else vAlbums)
                        .find { it.albumId == opened.albumId }
                }
                state.copy(
                    isLoading = false,
                    musicList = music,
                    videoList = videos,
                    musicAlbums = mAlbums,
                    videoAlbums = vAlbums,
                    currentItem = updatedCurrent,
                    detailsItem = updatedDetails,
                    openedAlbum = updatedOpenedAlbum
                )
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, openedAlbum = null) }
    }

    fun toggleSearch() {
        _uiState.update {
            val next = !it.isSearchActive
            it.copy(isSearchActive = next, searchQuery = if (!next) "" else it.searchQuery)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleHighlightsFilter() {
        _uiState.update { it.copy(showOnlyHighlights = !it.showOnlyHighlights, openedAlbum = null) }
    }

    fun setHighlightsFilter(enabled: Boolean) {
        _uiState.update { it.copy(showOnlyHighlights = enabled, openedAlbum = null) }
    }

    fun openAlbum(album: AlbumGroup) {
        _uiState.update { it.copy(openedAlbum = album) }
    }

    fun closeAlbum() {
        _uiState.update { it.copy(openedAlbum = null) }
    }

    fun playMedia(item: MediaItemModel, queueList: List<MediaItemModel>, openFullPlayer: Boolean = true) {
        val index = queueList.indexOfFirst { it.id == item.id && it.type == item.type }.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                queue = queueList,
                currentIndex = index,
                currentItem = item,
                currentPositionMs = 0L,
                durationMs = item.durationMs,
                isFullPlayerOpen = openFullPlayer
            )
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(item.contentUri))
        exoPlayer.prepare()
        exoPlayer.setPlaybackSpeed(_uiState.value.playbackSpeed)
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun playNext(autoAdvance: Boolean = false) {
        val state = _uiState.value
        val q = state.queue
        if (q.isEmpty()) return

        if (autoAdvance && state.repeatMode == Player.REPEAT_MODE_ONE) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            return
        }

        val nextIdx = if (state.shuffleEnabled && q.size > 1) {
            (q.indices - state.currentIndex).random()
        } else {
            val candidate = state.currentIndex + 1
            if (candidate < q.size) {
                candidate
            } else if (state.repeatMode == Player.REPEAT_MODE_ALL || !autoAdvance) {
                0
            } else {
                exoPlayer.pause()
                return
            }
        }
        playMedia(q[nextIdx], q, openFullPlayer = state.isFullPlayerOpen)
    }

    fun playPrevious() {
        val state = _uiState.value
        val q = state.queue
        if (q.isEmpty()) return
        if (exoPlayer.currentPosition > 3500L) {
            seekTo(0L)
            return
        }
        val prevIdx = if (state.currentIndex - 1 >= 0) state.currentIndex - 1 else q.lastIndex
        playMedia(q[prevIdx], q, openFullPlayer = state.isFullPlayerOpen)
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingSec = 0) }
            return
        }
        val totalSec = minutes * 60
        _uiState.update { it.copy(sleepTimerRemainingSec = totalSec) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (remaining <= 0) {
                exoPlayer.pause()
            }
        }
    }

    fun setFullPlayerOpen(open: Boolean) {
        _uiState.update { it.copy(isFullPlayerOpen = open) }
    }

    fun toggleHighlight(item: MediaItemModel) {
        val newState = prefsManager.toggleHighlight(item.type.name, item.id)
        updateItemInState(item.copy(isHighlighted = newState))
    }

    fun setCustomCoverForItem(item: MediaItemModel, imageUri: Uri) {
        viewModelScope.launch {
            val savedPath = prefsManager.saveCustomCover(item.type.name, item.id, imageUri)
            if (savedPath != null) {
                updateItemInState(item.copy(customCoverPath = savedPath, isHighlighted = true))
            }
        }
    }

    fun removeCustomCoverForItem(item: MediaItemModel) {
        prefsManager.removeCustomCover(item.type.name, item.id)
        updateItemInState(item.copy(customCoverPath = null))
    }

    fun setCustomCoverForAlbum(album: AlbumGroup, imageUri: Uri) {
        viewModelScope.launch {
            val keyType = if (album.type == MediaType.MUSIC) "ALBUM_MUSIC" else "ALBUM_VIDEO"
            val savedPath = prefsManager.saveCustomCover(keyType, album.albumId, imageUri)
            if (savedPath != null) {
                refreshLibrary()
            }
        }
    }

    private fun updateItemInState(updated: MediaItemModel) {
        _uiState.update { state ->
            fun replaceList(list: List<MediaItemModel>): List<MediaItemModel> =
                list.map { if (it.id == updated.id && it.type == updated.type) updated else it }

            val newMusic = replaceList(state.musicList)
            val newVideos = replaceList(state.videoList)
            val newQueue = replaceList(state.queue)
            val newCurrent = if (state.currentItem?.id == updated.id && state.currentItem.type == updated.type) {
                updated
            } else {
                state.currentItem
            }
            val newDetails = if (state.detailsItem?.id == updated.id && state.detailsItem.type == updated.type) {
                updated
            } else {
                state.detailsItem
            }
            val newOpenedAlbum = state.openedAlbum?.copy(items = replaceList(state.openedAlbum.items))

            state.copy(
                musicList = newMusic,
                videoList = newVideos,
                queue = newQueue,
                currentItem = newCurrent,
                detailsItem = newDetails,
                openedAlbum = newOpenedAlbum
            )
        }
    }

    fun openDetails(item: MediaItemModel) {
        _uiState.update {
            it.copy(
                detailsItem = item,
                detailsMetadata = null,
                isLoadingDetails = true
            )
        }
        viewModelScope.launch {
            val details = repository.extractDetails(item)
            _uiState.update {
                it.copy(
                    detailsMetadata = details,
                    isLoadingDetails = false
                )
            }
        }
    }

    fun closeDetails() {
        _uiState.update { it.copy(detailsItem = null, detailsMetadata = null) }
    }

    override fun onCleared() {
        super.onCleared()
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.release()
    }
}
