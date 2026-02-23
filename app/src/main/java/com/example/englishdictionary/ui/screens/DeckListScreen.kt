package com.example.englishdictionary.ui.screens

import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.englishdictionary.model.AppDeck
import com.example.englishdictionary.ui.DeckWallpaperImage
import com.example.englishdictionary.ui.TitleBgmPlayer
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.deckWallpaperRes
import com.example.englishdictionary.ui.readImageDimensions
import kotlin.math.roundToInt

@Composable
fun DeckListScreen(
    decks: List<AppDeck>,
    deckWallpaperUris: Map<String, String>,
    deckEntryCounts: Map<String, Int>,
    onSetDeckWallpaper: (String, String?) -> Unit,
    onOpenDeck: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val titleBgmPlayer = remember(context) { TitleBgmPlayer(context) }
    var pendingDeckId by rememberSaveable { mutableStateOf<String?>(null) }
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            val targetDeckId = pendingDeckId
            pendingDeckId = null
            if (targetDeckId == null || uri == null) {
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val imageSize = readImageDimensions(context, uri)
            val density = context.resources.displayMetrics.density
            val screenW = (context.resources.displayMetrics.widthPixels / density).roundToInt()
            val screenH = (context.resources.displayMetrics.heightPixels / density).roundToInt()
            val imageInfo = if (imageSize != null) {
                "Image ${imageSize.first}x${imageSize.second}px"
            } else {
                "Image size unknown"
            }
            Toast.makeText(
                context,
                "$imageInfo / Screen ${screenW}x${screenH}dp / Full-screen crop",
                Toast.LENGTH_LONG
            ).show()
            onSetDeckWallpaper(targetDeckId, uri.toString())
        }
    )
    DisposableEffect(titleBgmPlayer, lifecycleOwner) {
        titleBgmPlayer.start()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> titleBgmPlayer.start()
                Lifecycle.Event.ON_STOP -> titleBgmPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            titleBgmPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Decks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Choose your study deck",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(decks, key = { it.deckId }) { deck ->
                    val palette = deckPalette(deck.deckId)
                    val customWallpaperUri = deckWallpaperUris[deck.deckId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDeck(deck.deckId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.container.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(138.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            ) {
                                DeckWallpaperImage(
                                    deckId = deck.deckId,
                                    wallpaperUriString = customWallpaperUri,
                                    defaultWallpaperRes = deckWallpaperRes(deck.deckId),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    androidScaleType = ImageView.ScaleType.CENTER_CROP
                                )

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RoundIconActionButton(
                                        onClick = {
                                            pendingDeckId = deck.deckId
                                            wallpaperPickerLauncher.launch(arrayOf("image/*"))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Image,
                                            contentDescription = "Change wallpaper",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    RoundIconActionButton(
                                        onClick = { onSetDeckWallpaper(deck.deckId, null) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Restore,
                                            contentDescription = "Reset wallpaper",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(10.dp),
                                            tonalElevation = 2.dp
                                        ) {
                                            Text(
                                                text = deck.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = palette.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        if (deck.description.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(8.dp),
                                                tonalElevation = 1.dp
                                            ) {
                                                Text(
                                                    text = deck.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = "Open",
                                        tint = palette.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(10.dp),
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = "Words: ${deckEntryCounts[deck.deckId] ?: 0}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(10.dp),
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = "v${deck.version}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shadowElevation = 5.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            content()
        }
    }
}
