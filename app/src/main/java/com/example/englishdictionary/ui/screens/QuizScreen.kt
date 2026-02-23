package com.example.englishdictionary.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.ui.DeckWallpaperBackground
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.FeedbackJinglePlayer
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.swipeToBack
import kotlinx.coroutines.delay

enum class QuizStage {
    SELECT_COUNT,
    RUNNING,
    RESULT
}

private enum class QuizEntryCategory(val label: String) {
    RANDOM("Random"),
    FREQUENT("Frequent"),
    ALPHABET("Alphabet"),
    DIFFICULT("Difficult")
}

@Composable
fun QuizScreen(
    deckId: String,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.observeDeckEntries(deckId).collectAsStateWithLifecycle(initialValue = emptyList())
    val wrongCountMap by viewModel.observeWrongCounts(deckId).collectAsStateWithLifecycle(initialValue = emptyMap())
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()

    var stage by rememberSaveable { mutableStateOf(QuizStage.SELECT_COUNT.name) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAnswer by rememberSaveable { mutableStateOf(false) }
    var inReviewMode by rememberSaveable { mutableStateOf(false) }
    var autoNextPaused by rememberSaveable { mutableStateOf(false) }
    var autoDetailEnabled by rememberSaveable { mutableStateOf(true) }
    var quizEntryIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategoryName by rememberSaveable { mutableStateOf(QuizEntryCategory.RANDOM.name) }
    var selectedAlphabet by rememberSaveable { mutableStateOf("A") }

    val entriesById = remember(entries) { entries.associateBy { it.entryId } }
    val currentQuizEntries = remember(quizEntryIds, entriesById) {
        quizEntryIds.mapNotNull { entriesById[it] }
    }
    val answerMap = remember { mutableStateMapOf<String, Boolean>() }

    val jingle = remember(deckId) { FeedbackJinglePlayer(context.applicationContext, deckId) }
    DisposableEffect(Unit) {
        onDispose { jingle.release() }
    }

    val quizStage = QuizStage.valueOf(stage)
    val currentEntry = currentQuizEntries.getOrNull(currentIndex)
    val palette = deckPalette(deckId)
    val selectedCategory = QuizEntryCategory.valueOf(selectedCategoryName)
    val availableCategories = remember(deckId) { availableQuizCategories(deckId) }
    val filteredQuizPool = remember(
        entries,
        selectedCategory,
        selectedAlphabet,
        wrongCountMap,
        deckId
    ) {
        filterEntriesForQuiz(
            entries = entries,
            category = selectedCategory,
            alphabet = selectedAlphabet,
            wrongCountMap = wrongCountMap,
            deckId = deckId
        )
    }

    LaunchedEffect(showAnswer, currentIndex, currentQuizEntries.size, stage, autoNextPaused) {
        if (stage == QuizStage.RUNNING.name && showAnswer && !autoNextPaused) {
            delay(5000)
            if (currentIndex < currentQuizEntries.lastIndex) {
                currentIndex += 1
                showAnswer = false
            } else {
                stage = QuizStage.RESULT.name
            }
        }
    }

    DeckWallpaperBackground(
        deckId = deckId,
        wallpaperUriString = deckWallpaperUris[deckId],
        modifier = Modifier
            .fillMaxSize()
            .swipeToBack(onBack = onBack)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Quiz: $deckId", style = MaterialTheme.typography.headlineSmall, color = palette.primary)

            when (quizStage) {
                QuizStage.SELECT_COUNT -> {
                    QuizCountSelector(
                        modifier = Modifier.fillMaxWidth(),
                        availableCategories = availableCategories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategoryName = it.name },
                        selectedAlphabet = selectedAlphabet,
                        onSelectAlphabet = { selectedAlphabet = it },
                        total = filteredQuizPool.size,
                        onStart = { count ->
                            val selected = selectEntries(
                                entries = filteredQuizPool,
                                count = count,
                                category = selectedCategory
                            )
                            quizEntryIds = selected.map { it.entryId }
                            answerMap.clear()
                            currentIndex = 0
                            showAnswer = false
                            inReviewMode = false
                            autoNextPaused = false
                            autoDetailEnabled = true
                            stage = QuizStage.RUNNING.name
                        }
                    )
                }

                QuizStage.RUNNING -> {
                    if (currentEntry == null) {
                        Text("No entries available", style = MaterialTheme.typography.titleMedium)
                    } else {
                        val goNext = {
                            if (currentIndex < currentQuizEntries.lastIndex) {
                                currentIndex += 1
                                showAnswer = false
                            } else {
                                stage = QuizStage.RESULT.name
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .swipeLeftToNext(
                                    enabled = showAnswer,
                                    onNext = { goNext() }
                                ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${currentIndex + 1} / ${currentQuizEntries.size}" +
                                    if (inReviewMode) " (Review wrong only)" else "",
                                style = MaterialTheme.typography.bodySmall
                            )

                            AnimatedContent(
                                targetState = currentEntry.entryId,
                                transitionSpec = {
                                    slideInHorizontally(animationSpec = tween(250)) { fullWidth -> fullWidth } togetherWith
                                        slideOutHorizontally(animationSpec = tween(250)) { fullWidth -> -fullWidth }
                                },
                                label = "quizCardTransition"
                            ) {
                                QuizCard(
                                    entry = currentEntry,
                                    showAnswer = showAnswer,
                                    autoDetailEnabled = autoDetailEnabled,
                                    containerColor = palette.container,
                                    showNextIndicator = showAnswer,
                                    onSwipeLeft = { goNext() }
                                )
                            }

                            if (showAnswer) {
                                Text(
                                    if (autoNextPaused) {
                                        "Auto next paused. Swipe right-to-left to move to the next word."
                                    } else {
                                        "Answer shown. Next card opens automatically in 5 seconds. Swipe right-to-left to move now."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (autoNextPaused) {
                                Text(
                                    "Auto next paused. You can answer now, or swipe right-to-left to skip to next.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BottomActionButton(
                                        text = if (autoNextPaused) "Resume Auto" else "Pause Auto",
                                        onClick = { autoNextPaused = !autoNextPaused },
                                        modifier = Modifier.weight(1f),
                                        minHeight = 56.dp
                                    )
                                    BottomActionButton(
                                        text = if (autoDetailEnabled) "AutoDetail ON" else "AutoDetail OFF",
                                        onClick = { autoDetailEnabled = !autoDetailEnabled },
                                        modifier = Modifier.weight(1f),
                                        minHeight = 56.dp
                                    )
                                    BottomActionButton(
                                        text = "Detail",
                                        onClick = { onOpenEntry(currentEntry.entryId) },
                                        modifier = Modifier.weight(1f),
                                        minHeight = 56.dp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BottomActionButton(
                                        text = "○ 意味が分かる",
                                        onClick = {
                                            if (!showAnswer) {
                                                answerMap[currentEntry.entryId] = true
                                                viewModel.recordQuizAnswer(deckId, currentEntry.entryId, known = true)
                                                jingle.playCorrect()
                                                showAnswer = true
                                            }
                                        },
                                        enabled = !showAnswer,
                                        modifier = Modifier.weight(1f),
                                        minHeight = 68.dp,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    BottomActionButton(
                                        text = "× 分からない",
                                        onClick = {
                                            if (!showAnswer) {
                                                answerMap[currentEntry.entryId] = false
                                                viewModel.recordQuizAnswer(deckId, currentEntry.entryId, known = false)
                                                jingle.playIncorrect()
                                                showAnswer = true
                                            }
                                        },
                                        enabled = !showAnswer,
                                        modifier = Modifier.weight(1f),
                                        minHeight = 68.dp,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                QuizStage.RESULT -> {
                    val total = currentQuizEntries.size
                    val correct = answerMap.values.count { it }
                    val wrongEntries = currentQuizEntries.filter { answerMap[it.entryId] == false }
                    val skipped = total - (answerMap.values.count { it } + answerMap.values.count { !it })

                    Text("Result", style = MaterialTheme.typography.headlineSmall)
                    Text("Correct: $correct / $total")
                    Text("Wrong: ${wrongEntries.size}")
                    if (skipped > 0) {
                        Text("Skipped: $skipped")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (wrongEntries.isNotEmpty()) {
                                    quizEntryIds = wrongEntries.map { it.entryId }
                                    answerMap.clear()
                                    currentIndex = 0
                                    showAnswer = false
                                    inReviewMode = true
                                    autoNextPaused = false
                                    stage = QuizStage.RUNNING.name
                                }
                            },
                            enabled = wrongEntries.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Review wrong only")
                        }

                        Button(
                            onClick = { stage = QuizStage.SELECT_COUNT.name },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Restart")
                        }
                    }

                    if (quizEntryIds.isNotEmpty()) {
                        Text("Tap a word to open detail", style = MaterialTheme.typography.titleSmall)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentQuizEntries, key = { it.entryId }) { entry ->
                            val known = answerMap[entry.entryId]
                            val mark = when (known) {
                                true -> "○"
                                false -> "×"
                                null -> "-"
                            }
                            val wrongCount = wrongCountMap[entry.entryId] ?: 0

                            Surface(
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenEntry(entry.entryId) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("$mark ${entry.displayTerm}", style = MaterialTheme.typography.titleMedium)
                                    Text(entry.meaningEn.ifBlank { "(no meaning_en)" })
                                    if (entry.meaningJa.isNotBlank()) {
                                        Text(entry.meaningJa)
                                    }
                                    Text("Wrong count (total): $wrongCount", style = MaterialTheme.typography.bodySmall)
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
private fun QuizCountSelector(
    modifier: Modifier = Modifier,
    availableCategories: List<QuizEntryCategory>,
    selectedCategory: QuizEntryCategory,
    onSelectCategory: (QuizEntryCategory) -> Unit,
    selectedAlphabet: String,
    onSelectAlphabet: (String) -> Unit,
    total: Int,
    onStart: (Int) -> Unit
) {
    val options = listOf(10, 25, 50, 75, 100, -1)
    val alphabet = remember { ('A'..'Z').map { it.toString() } }
    Text("Select category", style = MaterialTheme.typography.titleMedium)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableCategories) { category ->
            val selected = category == selectedCategory
            Surface(
                modifier = Modifier.clickable { onSelectCategory(category) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                tonalElevation = if (selected) 4.dp else 1.dp
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }

    if (selectedCategory == QuizEntryCategory.ALPHABET) {
        Text("Select alphabet", style = MaterialTheme.typography.titleSmall)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(alphabet) { letter ->
                val selected = selectedAlphabet == letter
                Surface(
                    modifier = Modifier.clickable { onSelectAlphabet(letter) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    tonalElevation = if (selected) 3.dp else 0.dp
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    Text("Select quiz size", style = MaterialTheme.typography.titleMedium)
    Text("Available entries: $total", style = MaterialTheme.typography.bodySmall)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { size ->
            val label = if (size == -1) "ALL" else size.toString()
            val enabled = total > 0 && (size == -1 || total >= size)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onStart(size) },
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
private fun QuizCard(
    entry: AppEntry,
    showAnswer: Boolean,
    autoDetailEnabled: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    showNextIndicator: Boolean,
    onSwipeLeft: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .swipeLeftToNext(enabled = showNextIndicator, onNext = onSwipeLeft),
        color = containerColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(entry.displayTerm, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                if (showNextIndicator) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Swipe left for next",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (entry.pronunciationIpa.isNotBlank()) {
                Text("[${entry.pronunciationIpa}]", style = MaterialTheme.typography.titleMedium)
            }
            if (showAnswer) {
                if (autoDetailEnabled) {
                    EntryDetailTable(
                        rows = buildEntryDetailRows(entry),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    )
                } else {
                    Text(entry.meaningEn.ifBlank { "(no meaning_en)" })
                    if (entry.meaningJa.isNotBlank()) {
                        Text(entry.meaningJa)
                    }
                    if (entry.pos.isNotBlank()) {
                        Text(entry.pos, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 56.dp,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.heightIn(min = minHeight)
    ) {
        Text(text)
    }
}

private fun Modifier.swipeLeftToNext(
    enabled: Boolean = true,
    thresholdPx: Float = 64f,
    onNext: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled) {
        var dragX = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                dragX += dragAmount
            },
            onDragEnd = {
                if (dragX < -thresholdPx) {
                    onNext()
                }
                dragX = 0f
            },
            onDragCancel = {
                dragX = 0f
            }
        )
    }
}

private fun selectEntries(
    entries: List<AppEntry>,
    count: Int,
    category: QuizEntryCategory
): List<AppEntry> {
    if (entries.isEmpty()) return emptyList()
    val prepared = when (category) {
        QuizEntryCategory.ALPHABET -> entries.sortedBy { it.term.lowercase() }
        QuizEntryCategory.DIFFICULT -> entries
        QuizEntryCategory.FREQUENT -> entries
        QuizEntryCategory.RANDOM -> entries.shuffled()
    }
    return if (count == -1 || count >= prepared.size) {
        prepared
    } else {
        prepared.take(count)
    }
}

private fun availableQuizCategories(deckId: String): List<QuizEntryCategory> {
    return if (deckId == "toeic_business" || deckId == "eiken_pre2") {
        listOf(
            QuizEntryCategory.RANDOM,
            QuizEntryCategory.FREQUENT,
            QuizEntryCategory.ALPHABET,
            QuizEntryCategory.DIFFICULT
        )
    } else {
        listOf(
            QuizEntryCategory.RANDOM,
            QuizEntryCategory.ALPHABET,
            QuizEntryCategory.DIFFICULT
        )
    }
}

private fun filterEntriesForQuiz(
    entries: List<AppEntry>,
    category: QuizEntryCategory,
    alphabet: String,
    wrongCountMap: Map<String, Int>,
    deckId: String
): List<AppEntry> {
    if (entries.isEmpty()) return emptyList()
    return when (category) {
        QuizEntryCategory.RANDOM -> entries.shuffled()
        QuizEntryCategory.ALPHABET -> {
            val prefix = alphabet.lowercase()
            entries.filter { it.term.lowercase().startsWith(prefix) }
                .sortedBy { it.term.lowercase() }
        }
        QuizEntryCategory.FREQUENT -> {
            val frequent = entries.filter { hasFrequentTag(it.tags) }
            if (frequent.isNotEmpty()) {
                frequent.shuffled()
            } else if (deckId == "toeic_business") {
                entries
                    .sortedWith(compareBy<AppEntry> { toeicBandRank(it.tags) }.thenBy { it.term.lowercase() })
                    .take(maxOf(40, entries.size / 2))
            } else {
                entries.shuffled()
            }
        }
        QuizEntryCategory.DIFFICULT -> {
            val withScore = entries.map { entry ->
                val wrong = wrongCountMap[entry.entryId] ?: 0
                val tagScore = if (hasDifficultTag(entry.tags)) 1 else 0
                val score = wrong * 10 + tagScore
                entry to score
            }
            val difficult = withScore
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
            if (difficult.isNotEmpty()) difficult else entries.shuffled()
        }
    }
}

private fun hasFrequentTag(tags: List<String>): Boolean {
    val normalized = tags.map { it.lowercase() }
    return normalized.any {
        it in setOf("frequent", "common", "core", "high_frequency", "highfrequency", "toeic700", "700")
    }
}

private fun hasDifficultTag(tags: List<String>): Boolean {
    val normalized = tags.map { it.lowercase() }
    return normalized.any {
        it in setOf("advanced", "difficult", "hard", "toeic900", "900")
    }
}

private fun toeicBandRank(tags: List<String>): Int {
    val normalized = tags.map { it.lowercase() }
    return when {
        normalized.any { it == "toeic700" || it == "700" } -> 0
        normalized.any { it == "toeic800" || it == "800" } -> 1
        normalized.any { it == "toeic900" || it == "900" || it == "advanced" } -> 2
        else -> 3
    }
}
