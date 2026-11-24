package com.gramo.mediaplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gramo.mediaplayer.data.MediaRepository
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.model.MediaType
import com.gramo.mediaplayer.ui.components.MediaArtwork
import com.gramo.mediaplayer.ui.theme.*

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullPlayerScreen(
    item: MediaItemModel,
    queue: List<MediaItemModel>,
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int,
    lyricsVisible: Boolean,
    activeLyricText: String?,
    hasLyrics: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleHighlight: () -> Unit,
    onToggleLyricsVisibility: () -> Unit,
    onOpenDetails: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSleepTimer: (Int) -> Unit,
    onSelectQueueItem: (MediaItemModel) -> Unit
) {
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPos by remember { mutableFloatStateOf(0f) }

    val effectiveDuration = durationMs.coerceAtLeast(1L)
    val sliderValue = if (isDraggingSlider) {
        sliderDragPos
    } else {
        (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "ย่อหน้าต่าง",
                        tint = DarkIconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasLyrics) {
                        IconButton(onClick = onToggleLyricsVisibility) {
                            Icon(
                                imageVector = if (lyricsVisible) Icons.Outlined.Subtitles else Icons.Outlined.SubtitlesOff,
                                contentDescription = "แสดง/ซ่อนเนื้อเพลง",
                                tint = if (lyricsVisible) RoyalBlueAccent else DarkIconColor,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }

                    if (sleepTimerRemainingSec > 0) {
                        Text(
                            text = "${sleepTimerRemainingSec / 60}:${(sleepTimerRemainingSec % 60).toString().padStart(2, '0')}",
                            color = RoyalBlueAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            imageVector = if (sleepTimerRemainingSec > 0) Icons.Outlined.Timer else Icons.Outlined.TimerOff,
                            contentDescription = "ตั้งเวลาปิด",
                            tint = if (sleepTimerRemainingSec > 0) RoyalBlueAccent else DarkIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { showSpeedDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "ความเร็วในการเล่น",
                            tint = if (playbackSpeed != 1.0f) RoyalBlueAccent else DarkIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (item.type == MediaType.VIDEO) Color.Black else PlaceholderGray),
                contentAlignment = Alignment.Center
            ) {
                if (item.type == MediaType.VIDEO) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                keepScreenOn = true
                            }
                        },
                        update = { view ->
                            if (view.player !== exoPlayer) {
                                view.player = exoPlayer
                            }
                            view.keepScreenOn = isPlaying
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MediaArtwork(
                        coverUri = item.activeCoverUri,
                        mediaType = MediaType.MUSIC,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 20.dp,
                        discIconFraction = 0.46f
                    )
                }

                if (lyricsVisible && !activeLyricText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeLyricText.orEmpty(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.title,
                    color = RoyalBlueAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.artist,
                    color = MutedTextColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue,
                    onValueChange = { fraction ->
                        isDraggingSlider = true
                        sliderDragPos = fraction
                    },
                    onValueChangeFinished = {
                        val targetMs = (sliderDragPos * effectiveDuration).toLong()
                        onSeekTo(targetMs)
                        isDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = RoyalBlueAccent,
                        activeTrackColor = RoyalBlueAccent,
                        inactiveTrackColor = SoftBlueTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayedPosMs = if (isDraggingSlider) {
                        (sliderDragPos * effectiveDuration).toLong()
                    } else {
                        currentPositionMs
                    }
                    Text(
                        text = MediaRepository.formatDuration(displayedPosMs),
                        fontSize = 12.sp,
                        color = DarkIconColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = MediaRepository.formatDuration(durationMs),
                        fontSize = 12.sp,
                        color = DarkIconColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "ก่อนหน้า",
                        tint = DarkIconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(PlayCircleBg)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "หยุดชั่วคราว" else "เล่น",
                        tint = DarkIconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "ถัดไป",
                        tint = DarkIconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDetails) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "รายละเอียด",
                        tint = DarkIconColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(onClick = onCycleRepeat) {
                    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    }
                    val repeatTint = if (repeatMode != Player.REPEAT_MODE_OFF) {
                        RoyalBlueAccent
                    } else {
                        MutedTextColor.copy(alpha = 0.65f)
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "เล่นซ้ำ",
                        tint = repeatTint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "สุ่มเล่น",
                        tint = if (shuffleEnabled) RoyalBlueAccent else MutedTextColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(onClick = onToggleHighlight) {
                    Icon(
                        imageVector = if (item.isHighlighted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "ไฮไลท์ของฉัน",
                        tint = if (item.isHighlighted) RoyalBlueAccent else DarkIconColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = "คิวการเล่น",
                        tint = DarkIconColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("ความเร็วในการเล่น") },
            text = {
                Column {
                    speeds.forEach { spd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSpeed(spd)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == spd,
                                onClick = {
                                    onSelectSpeed(spd)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${spd}x", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("ปิด")
                }
            }
        )
    }

    if (showSleepDialog) {
        val options = listOf(0 to "ปิดการตั้งเวลา", 15 to "15 นาที", 30 to "30 นาที", 45 to "45 นาที", 60 to "60 นาที")
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("ตั้งเวลาหยุดเล่นอัตโนมัติ") },
            text = {
                Column {
                    options.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSleepTimer(mins)
                                    showSleepDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("ปิด")
                }
            }
        )
    }

    if (showQueueSheet) {
        AlertDialog(
            onDismissRequest = { showQueueSheet = false },
            title = { Text("คิวการเล่น (${queue.size} รายการ)") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(queue) { qItem ->
                        val isCurrent = qItem.id == item.id && qItem.type == item.type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectQueueItem(qItem)
                                    showQueueSheet = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaArtwork(
                                coverUri = qItem.activeCoverUri,
                                mediaType = qItem.type,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 8.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = qItem.title,
                                    color = if (isCurrent) RoyalBlueAccent else DarkIconColor,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = qItem.artist,
                                    color = MutedTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueueSheet = false }) {
                    Text("ปิด")
                }
            }
        )
    }
}
