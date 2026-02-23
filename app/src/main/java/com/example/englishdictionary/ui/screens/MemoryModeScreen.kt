package com.example.englishdictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishdictionary.ui.DeckWallpaperBackground
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.swipeToBack

@Composable
fun MemoryModeScreen(
    deckId: String,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val entries by viewModel.observeDeckEntries(deckId).collectAsStateWithLifecycle(initialValue = emptyList())
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()
    var index by rememberSaveable { mutableIntStateOf(0) }
    val palette = deckPalette(deckId)

    if (index > entries.lastIndex && entries.isNotEmpty()) {
        index = entries.lastIndex
    }

    val current = entries.getOrNull(index)

    DeckWallpaperBackground(
        deckId = deckId,
        wallpaperUriString = deckWallpaperUris[deckId],
        modifier = Modifier
            .fillMaxSize()
            .swipeToBack(onBack = onBack)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Memory Mode", style = MaterialTheme.typography.headlineSmall, color = palette.primary)
            Text("Deck: $deckId", style = MaterialTheme.typography.bodySmall)

            if (current == null) {
                Text("No entries available", style = MaterialTheme.typography.titleMedium)
                return@DeckWallpaperBackground
            }

            Text("${index + 1} / ${entries.size}", style = MaterialTheme.typography.bodySmall)

            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        current.displayTerm,
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (current.pronunciationIpa.isNotBlank() || current.pos.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (current.pronunciationIpa.isNotBlank()) append("[${current.pronunciationIpa}] ")
                                if (current.pos.isNotBlank()) append(current.pos)
                            }.trim(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("(E) ${current.meaningEn}")
                    if (current.meaningJa.isNotBlank()) {
                        Text("(J) ${current.meaningJa}")
                    }

                    if (current.prepositionUsages.isNotEmpty()) {
                        Text("Usage with prepositions:")
                        Text(current.prepositionUsages.joinToString("\n"))
                    }
                    if (current.verbPrepositionUsages.isNotEmpty()) {
                        Text("Verb + preposition:")
                        Text(
                            current.verbPrepositionUsages.joinToString("\n") {
                                if (it.meaning.isBlank()) it.expression else "${it.expression}: ${it.meaning}"
                            }
                        )
                    }
                    if (current.commonCollocations.isNotEmpty()) {
                        Text("Common collocations:")
                        Text(
                            current.commonCollocations.joinToString("\n") {
                                if (it.meaning.isBlank()) it.expression else "${it.expression}: ${it.meaning}"
                            }
                        )
                    }
                    if (current.idioms.isNotEmpty()) {
                        Text("Idioms:")
                        Text(
                            current.idioms.joinToString("\n") {
                                if (it.meaning.isBlank()) it.expression else "${it.expression}: ${it.meaning}"
                            }
                        )
                    }
                    if (current.latinEtymology.isNotBlank()) {
                        Text("Latin etymology:")
                        Text(current.latinEtymology)
                    }
                    if (current.relatedTerms.isNotEmpty()) {
                        Text("Related terms:")
                        Text(current.relatedTerms.joinToString(", "))
                    }

                    if (current.examples.isNotEmpty()) {
                        Text("Examples:")
                        current.examples.forEachIndexed { i, ex ->
                            Text("${i + 1}. ${ex.textEn}")
                            if (!ex.textJa.isNullOrBlank()) {
                                Text(ex.textJa)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (index > 0) index -= 1 }, enabled = index > 0) {
                    Text("Prev")
                }
                Button(onClick = { if (index < entries.lastIndex) index += 1 }, enabled = index < entries.lastIndex) {
                    Text("Next")
                }
                Button(onClick = { onOpenEntry(current.entryId) }) {
                    Text("Detail")
                }
            }
        }
    }
}
