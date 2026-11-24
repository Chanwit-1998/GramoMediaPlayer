package com.gramo.mediaplayer.model

import android.net.Uri

enum class MediaType {
    MUSIC,
    VIDEO
}

enum class MainTab(val title: String) {
    MUSIC("เพลง"),
    VIDEO("วิดีโอ"),
    MUSIC_ALBUMS("อัลบั้มเพลง"),
    VIDEO_ALBUMS("อัลบั้มวิดีโอ")
}

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

data class MediaItemModel(
    val id: Long,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val filePath: String,
    val type: MediaType,
    val defaultArtworkUri: Uri? = null,
    val customCoverPath: String? = null,
    val isHighlighted: Boolean = false,
    val hasLyrics: Boolean = false
) {
    val activeCoverUri: Any?
        get() = customCoverPath?.let { "file://$it" } ?: defaultArtworkUri
}

data class AlbumGroup(
    val albumId: Long,
    val albumName: String,
    val subtitle: String,
    val type: MediaType,
    val items: List<MediaItemModel>,
    val customCoverPath: String? = null
) {
    val coverUri: Any?
        get() = customCoverPath?.let { "file://$it" }
            ?: items.firstOrNull { it.activeCoverUri != null }?.activeCoverUri
}

data class MediaDetails(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val discNumber: String,
    val trackNumber: String,
    val genre: String,
    val year: String,
    val durationFormatted: String,
    val bitrateFormatted: String,
    val resolutionFormatted: String,
    val sizeFormatted: String,
    val filePath: String
)
