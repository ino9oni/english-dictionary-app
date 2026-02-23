package com.example.englishdictionary.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishdictionary.ui.DeckWallpaperBackground
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.swipeToBack
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    deckId: String,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenMatching: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedInitial by rememberSaveable { mutableStateOf("A") }
    val listState = rememberLazyListState()
    val entries by viewModel.observeDeckEntries(deckId).collectAsStateWithLifecycle(initialValue = emptyList())
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()
    val indexedEntries = remember(entries, selectedInitial) {
        entries.filter { it.displayTerm.firstOrNull()?.uppercaseChar()?.toString() == selectedInitial }
    }
    val filtered = remember(indexedEntries, query) {
        viewModel.filterEntries(indexedEntries, query)
    }

    val palette = deckPalette(deckId)
    val alphabet = remember { ('A'..'Z').map { it.toString() } }
    val indexScrollState = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 10.dp,
                shadowElevation = 14.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = {
                            BottomMenuIcon(
                                selected = true,
                                icon = Icons.AutoMirrored.Filled.List,
                                contentDescription = "一覧"
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenQuiz,
                        icon = {
                            BottomMenuIcon(
                                selected = false,
                                icon = Icons.Filled.CheckCircle,
                                contentDescription = "クイズ"
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenMatching,
                        icon = {
                            BottomMenuIcon(
                                selected = false,
                                icon = Icons.Filled.SwapHoriz,
                                contentDescription = "ワードマッチ"
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenMemory,
                        icon = {
                            BottomMenuIcon(
                                selected = false,
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "記憶"
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        DeckWallpaperBackground(
            deckId = deckId,
            wallpaperUriString = deckWallpaperUris[deckId],
            modifier = Modifier
                .fillMaxSize()
                .swipeToBack(onBack = onBack)
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Word List",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Deck: $deckId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 34.dp)
                            .horizontalScroll(indexScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        alphabet.forEach { letter ->
                            val selected = selectedInitial == letter
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) palette.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = if (selected) 8.dp else 3.dp,
                                shadowElevation = if (selected) 10.dp else 4.dp,
                                modifier = Modifier.clickable { selectedInitial = letter }
                            ) {
                                Text(
                                    text = letter.lowercase(),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f),
                        tonalElevation = 5.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(34.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val target = (indexScrollState.value - 180).coerceAtLeast(0)
                                    indexScrollState.animateScrollTo(target)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Scroll index left",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f),
                        tonalElevation = 5.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(34.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val maxScroll = indexScrollState.maxValue
                                    val target = (indexScrollState.value + 180).coerceAtMost(maxScroll)
                                    indexScrollState.animateScrollTo(target)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Scroll index right",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search in '$selectedInitial'") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Text(text = "${filtered.size} results", style = MaterialTheme.typography.bodySmall)

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.entryId }) { entry ->
                        Surface(
                            tonalElevation = 7.dp,
                            shadowElevation = 10.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                                .fillMaxWidth()
                                .clickable { onOpenEntry(entry.entryId) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = entry.displayTerm,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                val phoneticPos = buildString {
                                    if (entry.pronunciationIpa.isNotBlank()) append("[${entry.pronunciationIpa}] ")
                                    if (entry.pos.isNotBlank()) append(entry.pos)
                                }.trim()
                                if (phoneticPos.isNotBlank()) {
                                    Text(text = phoneticPos, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(text = "(E) ${entry.meaningEn.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "(J) ${entry.meaningJa.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium)
                                val firstExample = entry.examples.firstOrNull()
                                if (firstExample != null) {
                                    Text(
                                        text = "Example: ${firstExample.textEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.widthIn(max = 1200.dp)
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
private fun BottomMenuIcon(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        tonalElevation = if (selected) 8.dp else 2.dp,
        shadowElevation = if (selected) 10.dp else 3.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.padding(10.dp)
        )
    }
}
