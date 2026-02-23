package com.example.englishdictionary.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun DeckWallpaperBackground(
    deckId: String?,
    wallpaperUriString: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val defaultRes = deckWallpaperRes(deckId.orEmpty())
    Box(modifier = modifier) {
        DeckWallpaperImage(
            deckId = deckId,
            wallpaperUriString = wallpaperUriString,
            defaultWallpaperRes = defaultRes,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun DeckWallpaperImage(
    deckId: String?,
    wallpaperUriString: String?,
    defaultWallpaperRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    androidScaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER
) {
    val context = LocalContext.current
    val parsedUri = remember(wallpaperUriString) {
        runCatching { wallpaperUriString?.let(Uri::parse) }.getOrNull()
    }
    val assetBackgroundPath = remember(deckId) {
        resolveDeckBackgroundAsset(context, deckId.orEmpty())
    }
    val assetBackgroundBitmap = remember(assetBackgroundPath) {
        assetBackgroundPath?.let { loadBitmapFromAsset(context, it) }
    }
    if (parsedUri == null && assetBackgroundBitmap != null) {
        Image(
            bitmap = assetBackgroundBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            alignment = Alignment.Center,
            modifier = modifier
        )
        return
    }
    if (parsedUri == null) {
        Image(
            painter = painterResource(id = defaultWallpaperRes),
            contentDescription = null,
            contentScale = contentScale,
            alignment = Alignment.Center,
            modifier = modifier
        )
        return
    }

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = androidScaleType
                setImageResource(defaultWallpaperRes)
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.scaleType = androidScaleType
            val loaded = runCatching {
                imageView.setImageURI(parsedUri)
                true
            }.getOrDefault(false)
            if (!loaded) {
                if (assetBackgroundBitmap != null) {
                    imageView.setImageBitmap(assetBackgroundBitmap)
                } else {
                    imageView.setImageResource(defaultWallpaperRes)
                }
            }
        }
    )
}

fun readImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    return runCatching {
        context.contentResolver.openInputStream(uri).use { stream ->
            if (stream == null) return null
            BitmapFactory.decodeStream(stream, null, options)
            val width = options.outWidth
            val height = options.outHeight
            if (width > 0 && height > 0) width to height else null
        }
    }.getOrNull()
}

private fun loadBitmapFromAsset(context: Context, path: String): Bitmap? {
    return runCatching {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()
}
