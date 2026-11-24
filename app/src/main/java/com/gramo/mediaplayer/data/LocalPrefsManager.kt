package com.gramo.mediaplayer.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalPrefsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("gramo_media_prefs", Context.MODE_PRIVATE)
    private val coversDir: File by lazy {
        File(context.filesDir, "custom_covers").apply {
            if (!exists()) mkdirs()
        }
    }
    private val lyricsDir: File by lazy {
        File(context.filesDir, "custom_lyrics").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun mediaKey(type: String, id: Long): String = "${type}_$id"

    fun isHighlighted(type: String, id: Long): Boolean {
        return prefs.getBoolean("hl_${mediaKey(type, id)}", false)
    }

    fun toggleHighlight(type: String, id: Long): Boolean {
        val key = "hl_${mediaKey(type, id)}"
        val next = !prefs.getBoolean(key, false)
        prefs.edit().putBoolean(key, next).apply()
        return next
    }

    fun getCustomCoverPath(type: String, id: Long): String? {
        val path = prefs.getString("cover_${mediaKey(type, id)}", null) ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 0) path else null
    }

    suspend fun saveCustomCover(type: String, id: Long, imageUri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val destFile = File(coversDir, "${mediaKey(type, id)}_${System.currentTimeMillis()}.jpg")
                getCustomCoverPath(type, id)?.let { oldPath ->
                    runCatching { File(oldPath).delete() }
                }
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    prefs.edit()
                        .putString("cover_${mediaKey(type, id)}", destFile.absolutePath)
                        .putBoolean("hl_${mediaKey(type, id)}", true)
                        .apply()
                    destFile.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun removeCustomCover(type: String, id: Long) {
        getCustomCoverPath(type, id)?.let { oldPath ->
            runCatching { File(oldPath).delete() }
        }
        prefs.edit().remove("cover_${mediaKey(type, id)}").apply()
    }

    private fun internalLyricsFile(type: String, id: Long): File {
        return File(lyricsDir, "${mediaKey(type, id)}.lrc")
    }

    private fun findSidecarLyricsFile(mediaFilePath: String): File? {
        if (mediaFilePath.isBlank()) return null
        val mediaFile = File(mediaFilePath)
        val parent = mediaFile.parentFile ?: return null
        val baseName = mediaFile.nameWithoutExtension
        val lrcFile = File(parent, "$baseName.lrc")
        if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() > 0) return lrcFile
        val srtFile = File(parent, "$baseName.srt")
        if (srtFile.exists() && srtFile.canRead() && srtFile.length() > 0) return srtFile
        return null
    }

    fun hasLyrics(type: String, id: Long, mediaFilePath: String = ""): Boolean {
        val internal = internalLyricsFile(type, id)
        if (internal.exists() && internal.length() > 0) return true
        return findSidecarLyricsFile(mediaFilePath) != null
    }

    suspend fun loadLyricsText(type: String, id: Long, mediaFilePath: String = ""): String =
        withContext(Dispatchers.IO) {
            val internal = internalLyricsFile(type, id)
            if (internal.exists() && internal.length() > 0) {
                return@withContext runCatching { internal.readText(Charsets.UTF_8) }.getOrDefault("")
            }
            val sidecar = findSidecarLyricsFile(mediaFilePath)
            if (sidecar != null) {
                return@withContext runCatching { sidecar.readText(Charsets.UTF_8) }.getOrDefault("")
            }
            ""
        }

    suspend fun saveLyricsText(type: String, id: Long, content: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = internalLyricsFile(type, id)
                if (content.isBlank()) {
                    if (file.exists()) file.delete()
                    return@withContext false
                }
                file.writeText(content.trim(), Charsets.UTF_8)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun readUriText(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
