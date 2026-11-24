package com.gramo.mediaplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.gramo.mediaplayer.model.MediaDetails
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MediaRepository(
    private val context: Context,
    private val prefsManager: LocalPrefsManager
) {

    suspend fun loadAllAudio(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.DURATION} > 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                val artworkBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "ไม่ทราบชื่อเพลง"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                        "ศิลปินที่ไม่รู้จัก"
                    } else {
                        rawArtist
                    }
                    val rawAlbum = cursor.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") {
                        "อัลบั้มที่ไม่รู้จัก"
                    } else {
                        rawAlbum
                    }
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val filePath = cursor.getString(dataCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val artUri = ContentUris.withAppendedId(artworkBaseUri, albumId)

                    list.add(
                        MediaItemModel(
                            id = id,
                            contentUri = contentUri,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedSec = dateAdded,
                            filePath = filePath,
                            type = MediaType.MUSIC,
                            defaultArtworkUri = artUri,
                            customCoverPath = prefsManager.getCustomCoverPath("MUSIC", id),
                            isHighlighted = prefsManager.isHighlighted("MUSIC", id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun loadAllVideos(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemModel>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA
        )

        val selection = "${MediaStore.Video.Media.DURATION} > 500"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "วิดีโอไม่มีชื่อ"
                    val bucketName = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "วิดีโอในเครื่อง"
                    val bucketId = cursor.getLong(bucketIdCol)
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                        bucketName
                    } else {
                        rawArtist
                    }
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val filePath = cursor.getString(dataCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    list.add(
                        MediaItemModel(
                            id = id,
                            contentUri = contentUri,
                            title = title,
                            artist = artist,
                            album = bucketName,
                            albumId = bucketId,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedSec = dateAdded,
                            filePath = filePath,
                            type = MediaType.VIDEO,
                            defaultArtworkUri = contentUri, // Coil VideoDecoder extracts frame thumbnail automatically
                            customCoverPath = prefsManager.getCustomCoverPath("VIDEO", id),
                            isHighlighted = prefsManager.isHighlighted("VIDEO", id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun extractDetails(item: MediaItemModel): MediaDetails = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var albumArtist = "ศิลปินที่ไม่รู้จัก"
        var discNumber = "0"
        var trackNumber = "0"
        var genre = "ไม่ทราบแนวเพลง"
        var year = "ไม่ทราบปี"
        var bitrateStr = "-"
        var resolutionStr = "-"

        try {
            retriever.setDataSource(context, item.contentUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.takeIf { it.isNotBlank() }?.let { albumArtist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                ?.takeIf { it.isNotBlank() }?.let { discNumber = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.takeIf { it.isNotBlank() }?.let { trackNumber = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }?.let { genre = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.takeIf { it.isNotBlank() }?.let { year = it }

            val bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            if (bitrateBps != null && bitrateBps > 0) {
                bitrateStr = "${bitrateBps / 1000} kbps"
            }

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (!width.isNullOrBlank() && !height.isNullOrBlank()) {
                resolutionStr = "${width}x${height}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching { retriever.release() }
        }

        MediaDetails(
            title = item.title,
            artist = item.artist,
            album = item.album,
            albumArtist = albumArtist,
            discNumber = discNumber,
            trackNumber = trackNumber,
            genre = genre,
            year = year,
            durationFormatted = formatDuration(item.durationMs),
            bitrateFormatted = bitrateStr,
            resolutionFormatted = resolutionStr,
            sizeFormatted = formatSize(item.sizeBytes),
            filePath = item.filePath.ifBlank { item.contentUri.toString() }
        )
    }

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0L) return "0:00"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }

        fun formatSize(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.2f MB", mb)
            }
        }
    }
}
