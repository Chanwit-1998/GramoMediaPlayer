package com.gramo.mediaplayer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gramo.mediaplayer.model.MediaDetails
import com.gramo.mediaplayer.model.MediaItemModel
import com.gramo.mediaplayer.model.MediaType
import com.gramo.mediaplayer.ui.components.MediaArtwork
import com.gramo.mediaplayer.ui.theme.*

@Composable
fun DetailsScreen(
    item: MediaItemModel,
    details: MediaDetails?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPickCoverImage: (Uri) -> Unit,
    onRemoveCustomCover: () -> Unit,
    onToggleHighlight: () -> Unit,
    onOpenLyricsEditor: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickCoverImage(uri)
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "ย้อนกลับ",
                    tint = DarkIconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "รายละเอียด",
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = DarkIconColor
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "ปกอัลบั้ม",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = RoyalBlueAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaArtwork(
                    coverUri = item.activeCoverUri,
                    mediaType = item.type,
                    modifier = Modifier
                        .size(168.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    cornerRadius = 8.dp,
                    discIconFraction = 0.48f
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SelectedPillBg,
                            contentColor = DarkIconColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.customCoverPath != null) "เปลี่ยนปกอัลบั้ม" else "เพิ่มปกอัลบั้ม",
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleHighlight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (item.isHighlighted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (item.isHighlighted) RoyalBlueAccent else DarkIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.isHighlighted) "อยู่ในไฮไลท์แล้ว" else "เพิ่มในไฮไลท์",
                            fontSize = 13.sp,
                            color = if (item.isHighlighted) RoyalBlueAccent else DarkIconColor
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenLyricsEditor,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (item.hasLyrics) SelectedPillBg else PlaceholderGray,
                            contentColor = if (item.hasLyrics) RoyalBlueAccent else DarkIconColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Subtitles,
                            contentDescription = null,
                            tint = if (item.hasLyrics) RoyalBlueAccent else DarkIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.hasLyrics) "แก้ไขเนื้อเพลง" else "เพิ่มเนื้อเพลง",
                            fontSize = 13.sp
                        )
                    }

                    if (item.customCoverPath != null) {
                        TextButton(
                            onClick = onRemoveCustomCover,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MutedTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ลบรูปปกที่ตั้งเอง",
                                fontSize = 12.sp,
                                color = MutedTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = DividerGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading || details == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoyalBlueAccent)
                }
            } else {
                val titleLabel = if (item.type == MediaType.MUSIC) "ชื่อเพลง" else "ชื่อวิดีโอ"
                DetailMetadataRow(label = titleLabel, value = details.title)
                DetailMetadataRow(label = "ศิลปิน", value = details.artist)
                DetailMetadataRow(label = "อัลบั้ม", value = details.album)
                DetailMetadataRow(label = "ศิลปินอัลบั้ม", value = details.albumArtist)
                DetailMetadataRow(label = "แผ่นที่", value = details.discNumber)
                DetailMetadataRow(label = "หมายเลขแทร็ก", value = details.trackNumber)
                DetailMetadataRow(label = "แนวเพลง", value = details.genre)
                DetailMetadataRow(label = "ปี", value = details.year)
                DetailMetadataRow(label = "ความยาว", value = details.durationFormatted)
                DetailMetadataRow(label = "บิตเรต", value = details.bitrateFormatted)
                if (item.type == MediaType.VIDEO) {
                    DetailMetadataRow(label = "ความละเอียด", value = details.resolutionFormatted)
                }
                DetailMetadataRow(label = "ขนาดไฟล์", value = details.sizeFormatted)
                DetailMetadataRow(label = "ตำแหน่งไฟล์", value = details.filePath)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = RoyalBlueAccent,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            color = DarkIconColor,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
