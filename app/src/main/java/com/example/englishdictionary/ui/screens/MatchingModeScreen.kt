package com.example.englishdictionary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.ui.DeckWallpaperBackground
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.FeedbackJinglePlayer
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.swipeToBack
import java.util.UUID
import kotlin.random.Random

private data class MeaningChoice(
    val id: String,
    val entryId: String,
    val text: String
)

private enum class MatchingStage {
    SELECT_COUNT,
    RUNNING,
    RESULT
}

@Composable
fun MatchingModeScreen(
    deckId: String,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.observeDeckEntries(deckId).collectAsStateWithLifecycle(initialValue = emptyList())
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()
    val palette = deckPalette(deckId)

    var stage by rememberSaveable { mutableStateOf(MatchingStage.SELECT_COUNT) }
    var solvedCount by rememberSaveable { mutableIntStateOf(0) }
    var missCount by rememberSaveable { mutableIntStateOf(0) }
    var totalTarget by rememberSaveable { mutableIntStateOf(0) }
    var selectedLeftSlot by rememberSaveable { mutableIntStateOf(-1) }
    var selectedRightSlot by rememberSaveable { mutableIntStateOf(-1) }

    val leftSlots = remember { mutableStateListOf<AppEntry?>() }
    val pendingEntries = remember { mutableStateListOf<AppEntry>() }
    val rightSlots = remember { mutableStateListOf<MeaningChoice?>() }
    val resultEntries = remember { mutableStateListOf<AppEntry>() }

    val jingle = remember(deckId) { FeedbackJinglePlayer(context.applicationContext, deckId) }
    DisposableEffect(Unit) {
        onDispose { jingle.release() }
    }

    fun toChoice(entry: AppEntry): MeaningChoice = MeaningChoice(
        id = UUID.randomUUID().toString(),
        entryId = entry.entryId,
        text = entry.meaningJa.ifBlank { entry.meaningEn.ifBlank { "-" } }
    )

    fun startMatching(count: Int) {
        val picked = if (count == -1 || count >= entries.size) entries.shuffled() else entries.shuffled().take(count)
        leftSlots.clear()
        pendingEntries.clear()
        rightSlots.clear()
        solvedCount = 0
        missCount = 0
        totalTarget = picked.size
        selectedLeftSlot = -1
        selectedRightSlot = -1
        stage = MatchingStage.RUNNING
        resultEntries.clear()
        resultEntries.addAll(picked)
        pendingEntries.addAll(picked)

        val visibleCount = minOf(4, picked.size)
        repeat(visibleCount) {
            leftSlots.add(pendingEntries.removeAt(0))
        }
        rightSlots.addAll(leftSlots.mapNotNull { slot ->
            val entry = slot ?: return@mapNotNull null
            toChoice(entry)
        }.shuffled())
    }

    fun clearSelections() {
        selectedLeftSlot = -1
        selectedRightSlot = -1
    }

    fun refillIfReady() {
        while (pendingEntries.isNotEmpty()) {
            val emptyLeft = leftSlots.mapIndexedNotNull { index, value ->
                if (value == null) index else null
            }
            val emptyRight = rightSlots.mapIndexedNotNull { index, value ->
                if (value == null) index else null
            }
            if (emptyLeft.size < 2 || emptyRight.size < 2) {
                return
            }
            val nextEntry = pendingEntries.removeAt(0)
            val leftIndex = emptyLeft[Random.nextInt(emptyLeft.size)]
            val rightIndex = emptyRight[Random.nextInt(emptyRight.size)]
            leftSlots[leftIndex] = nextEntry
            rightSlots[rightIndex] = toChoice(nextEntry)
        }
    }

    fun handleMatchAttempt() {
        if (selectedLeftSlot < 0 || selectedRightSlot < 0) return

        val left = leftSlots.getOrNull(selectedLeftSlot)
        val right = rightSlots.getOrNull(selectedRightSlot)
        if (left == null || right == null) return

        if (left.entryId == right.entryId) {
            jingle.playCorrect()
            solvedCount += 1

            // Do not refill immediately in the same slots.
            // Keep blank slots and refill later at random positions for better thinking.
            leftSlots[selectedLeftSlot] = null
            rightSlots[selectedRightSlot] = null
            refillIfReady()

            if (solvedCount >= totalTarget && totalTarget > 0) {
                stage = MatchingStage.RESULT
            }
        } else {
            jingle.playIncorrect()
            missCount += 1
        }
        clearSelections()
    }

    DeckWallpaperBackground(
        deckId = deckId,
        wallpaperUriString = deckWallpaperUris[deckId],
        modifier = Modifier
            .fillMaxSize()
            .swipeToBack(onBack = onBack)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
        when (stage) {
            MatchingStage.SELECT_COUNT -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Matching: $deckId", style = MaterialTheme.typography.headlineSmall)
                    Text("Select challenge size", style = MaterialTheme.typography.titleMedium)
                    Text("Available entries: ${entries.size}", style = MaterialTheme.typography.bodySmall)
                    MatchingCountSelector(total = entries.size, onStart = ::startMatching)
                }
            }

            MatchingStage.RUNNING -> {
                if (totalTarget == 0) {
                    Text("No entries available", style = MaterialTheme.typography.titleMedium)
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Matching: $deckId", style = MaterialTheme.typography.headlineSmall)
                        Text("Solved: $solvedCount / $totalTarget", style = MaterialTheme.typography.bodyMedium)
                        Text("Misses: $missCount", style = MaterialTheme.typography.bodySmall)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(leftSlots.size, key = { "pair_$it" }) { index ->
                                val entry = leftSlots.getOrNull(index)
                                val choice = rightSlots.getOrNull(index)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (entry == null) {
                                        MatchPlaceholderCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            minHeight = 76.dp
                                        )
                                    } else {
                                        MatchSelectableCard(
                                            text = entry.displayTerm,
                                            selected = selectedLeftSlot == index,
                                            onClick = {
                                                selectedLeftSlot = if (selectedLeftSlot == index) -1 else index
                                                handleMatchAttempt()
                                            },
                                            accent = palette.primary,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            minHeight = 76.dp
                                        )
                                    }

                                    if (choice == null) {
                                        MatchPlaceholderCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            minHeight = 76.dp
                                        )
                                    } else {
                                        MatchSelectableCard(
                                            text = choice.text,
                                            selected = selectedRightSlot == index,
                                            onClick = {
                                                selectedRightSlot = if (selectedRightSlot == index) -1 else index
                                                handleMatchAttempt()
                                            },
                                            accent = palette.accent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            minHeight = 76.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MatchingStage.RESULT -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Matching Result", style = MaterialTheme.typography.headlineSmall)
                    Text("Solved: $solvedCount / $totalTarget", style = MaterialTheme.typography.titleMedium)
                    Text("Misses: $missCount", style = MaterialTheme.typography.bodyMedium)
                    Text("Words and meanings", style = MaterialTheme.typography.titleMedium)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(resultEntries, key = { it.entryId }) { entry ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                tonalElevation = 3.dp,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = entry.displayTerm,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = entry.meaningJa.ifBlank { entry.meaningEn.ifBlank { "-" } },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Button(onClick = { stage = MatchingStage.SELECT_COUNT }) {
                        Text("Restart")
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MatchPlaceholderCard(
    modifier: Modifier = Modifier,
    minHeight: Dp = 68.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp
    ) {
        Text(
            text = " ",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun MatchingCountSelector(total: Int, onStart: (Int) -> Unit) {
    val options = listOf(10, 25, 50, 75, 100, -1)
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { count ->
            val label = if (count == -1) "ALL" else count.toString()
            val enabled = count == -1 || total >= count
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onStart(count) },
                shape = RoundedCornerShape(14.dp),
                color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (enabled) 3.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Count", style = MaterialTheme.typography.bodyMedium)
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun MatchSelectableCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    minHeight: Dp = 68.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 6.dp else 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
