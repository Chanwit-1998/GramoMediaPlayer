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

    fun setHighlighted(type: String, id: Long, value: Boolean) {
        prefs.edit().putBoolean("hl_${mediaKey(type, id)}", value).apply()
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
                // Clean up older cover file for this item if any
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
                        .putBoolean("hl_${mediaKey(type, id)}", true) // Auto-include in My Highlights when custom cover is added
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
}
