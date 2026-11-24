package com.gramo.mediaplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.gramo.mediaplayer.model.MediaType
import com.gramo.mediaplayer.ui.theme.DarkIconColor
import com.gramo.mediaplayer.ui.theme.PlaceholderGray

@Composable
fun MediaArtwork(
    coverUri: Any?,
    mediaType: MediaType,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    discIconFraction: Float = 0.48f,
    showVideoBadge: Boolean = false
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(PlaceholderGray),
        contentAlignment = Alignment.Center
    ) {
        if (coverUri != null) {
            val request = ImageRequest.Builder(context)
                .data(coverUri)
                .crossfade(true)
                .apply {
                    if (mediaType == MediaType.VIDEO && coverUri.toString().startsWith("content://")) {
                        decoderFactory(VideoFrameDecoder.Factory())
                    }
                }
                .build()

            SubcomposeAsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    DiscPlaceholderGraphic(fraction = discIconFraction)
                },
                loading = {
                    DiscPlaceholderGraphic(fraction = discIconFraction)
                }
            )
        } else {
            DiscPlaceholderGraphic(fraction = discIconFraction)
        }

        if (showVideoBadge && mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Rounded.PlayCircleFilled,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DiscPlaceholderGraphic(fraction: Float = 0.48f) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(fraction)) {
            val radius = size.minDimension / 2f
            val outerStroke = radius * 0.22f
            // Outer thick dark ring
            drawCircle(
                color = DarkIconColor,
                radius = radius - outerStroke / 2f,
                style = Stroke(width = outerStroke)
            )
            // Inner solid disc
            drawCircle(
                color = DarkIconColor,
                radius = radius * 0.44f
            )
            // Center spindle hole
            drawCircle(
                color = PlaceholderGray,
                radius = radius * 0.14f
            )
        }
    }
}
