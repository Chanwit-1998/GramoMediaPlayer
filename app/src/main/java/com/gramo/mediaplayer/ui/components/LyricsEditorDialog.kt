package com.gramo.mediaplayer.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
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
import com.gramo.mediaplayer.data.LyricsParser
import com.gramo.mediaplayer.data.MediaRepository
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.ui.theme.*

@Composable
fun LyricsEditorDialog(
    item: MediaItemModel,
    rawText: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onTextChange: (String) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onStartPlaybackForSync: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isSyncMode by remember { mutableStateOf(false) }
    var syncLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var syncIndex by remember { mutableIntStateOf(0) }
    var syncedBuffer by remember { mutableStateOf<List<String>>(emptyList()) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFileUri(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isSyncMode) "เคาะจังหวะเนื้อเพลง" else "เพิ่ม / แก้ไขเนื้อเพลง",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkIconColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    color = RoyalBlueAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isSyncMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("นำเข้าไฟล์ .lrc / .srt", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = onTextChange,
                        placeholder = {
                            Text(
                                text = "วางเนื้อเพลงที่นี่ (รองรับทั้งข้อความธรรมดา และรูปแบบเวลา [00:15.40] ท่อนร้อง)",
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val candidateLines = remember(rawText) { LyricsParser.stripTimestamps(rawText) }
                    FilledTonalButton(
                        onClick = {
                            if (candidateLines.isNotEmpty()) {
                                syncLines = candidateLines
                                syncIndex = 0
                                syncedBuffer = emptyList()
                                isSyncMode = true
                                onStartPlaybackForSync()
                            }
                        },
                        enabled = candidateLines.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SelectedPillBg,
                            contentColor = RoyalBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("เคาะจังหวะให้ตรงเสียงร้อง (${candidateLines.size} บรรทัด)", fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "เวลาปัจจุบัน: ${MediaRepository.formatDuration(currentPositionMs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlueAccent
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    syncIndex = 0
                                    syncedBuffer = emptyList()
                                    onStartPlaybackForSync()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "เริ่มใหม่", tint = DarkIconColor)
                            }
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "เล่น/หยุด",
                                    tint = DarkIconColor
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181A20))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val currentPrompt = syncLines.getOrNull(syncIndex) ?: "ครบทุกบรรทัดแล้ว กดบันทึกได้เลย!"
                            val nextPrompt = syncLines.getOrNull(syncIndex + 1) ?: ""
                            Text(
                                text = currentPrompt,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            if (nextPrompt.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ถัดไป: $nextPrompt",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (syncIndex < syncLines.size) {
                                val stamped = "${LyricsParser.formatTimestampTag(currentPositionMs)} ${syncLines[syncIndex]}"
                                val nextList = syncedBuffer + stamped
                                syncedBuffer = nextList
                                syncIndex += 1
                                if (syncIndex >= syncLines.size) {
                                    onTextChange(nextList.joinToString("\n"))
                                    isSyncMode = false
                                }
                            }
                        },
                        enabled = syncIndex < syncLines.size,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (syncIndex < syncLines.size) {
                                "เคาะเมื่อเริ่มร้อง (${syncIndex + 1}/${syncLines.size})"
                            } else {
                                "เคาะครบทุกบรรทัดแล้ว"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            if (syncedBuffer.isNotEmpty()) {
                                onTextChange(syncedBuffer.joinToString("\n"))
                            }
                            isSyncMode = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("กลับไปหน้าแก้ไขข้อความ", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalContent = if (isSyncMode && syncedBuffer.isNotEmpty()) {
                        syncedBuffer.joinToString("\n")
                    } else {
                        rawText
                    }
                    onSave(finalContent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent)
            ) {
                Text("บันทึกเนื้อเพลง")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
