package com.example.englishdictionary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.model.AppExpressionMeaning
import com.example.englishdictionary.model.AppVerbPrepositionDetail

data class EntryDetailRow(
    val label: String,
    val value: String,
    val playText: String? = null
)

fun buildEntryDetailRows(entry: AppEntry): List<EntryDetailRow> {
    val examplesText = entry.examples.joinToString("\n") { it.textEn }
    return buildList {
        add(EntryDetailRow("Word", entry.displayTerm, playText = entry.term))
        if (entry.pronunciationIpa.isNotBlank()) add(EntryDetailRow("Pronunciation", "/${entry.pronunciationIpa}/"))
        if (entry.pos.isNotBlank()) add(EntryDetailRow("POS", entry.pos))
        add(EntryDetailRow("Meaning (E)", entry.meaningEn.ifBlank { "-" }))
        add(EntryDetailRow("Meaning (J)", entry.meaningJa.ifBlank { "-" }))
        if (entry.verbPrepositionDetails.isNotEmpty()) {
            add(
                EntryDetailRow(
                    "Frequent verb + preposition (Top 5)",
                    formatVerbPrepositionDetails(entry.verbPrepositionDetails.take(5))
                )
            )
        }
        if (entry.prepositionUsages.isNotEmpty()) {
            add(EntryDetailRow("Usage with prepositions", entry.prepositionUsages.joinToString("\n")))
        }
        if (entry.verbPrepositionUsages.isNotEmpty()) {
            add(
                EntryDetailRow(
                    "Verb + preposition",
                    formatExpressionMeanings(entry.verbPrepositionUsages)
                )
            )
        }
        if (entry.commonCollocations.isNotEmpty()) {
            add(
                EntryDetailRow(
                    "Common collocations",
                    formatExpressionMeanings(entry.commonCollocations)
                )
            )
        }
        if (entry.idioms.isNotEmpty()) {
            add(EntryDetailRow("Idioms", formatExpressionMeanings(entry.idioms)))
        }
        if (entry.latinEtymology.isNotBlank()) {
            add(EntryDetailRow("Latin etymology", entry.latinEtymology))
        }
        if (entry.relatedTerms.isNotEmpty()) {
            add(EntryDetailRow("Related terms", entry.relatedTerms.joinToString(", ")))
        }
        if (entry.examples.isNotEmpty()) {
            add(
                EntryDetailRow(
                    "Examples",
                    entry.examples.joinToString("\n\n") { ex ->
                        if (ex.textJa.isNullOrBlank()) ex.textEn else "${ex.textEn}\n${ex.textJa}"
                    },
                    playText = examplesText
                )
            )
        }
        if (entry.synonyms.isNotEmpty()) add(EntryDetailRow("Synonyms", entry.synonyms.joinToString(", ")))
        if (entry.confusables.isNotEmpty()) add(EntryDetailRow("Confusables", entry.confusables.joinToString(", ")))
        if (entry.canonicalTranslation.isNotBlank()) {
            add(EntryDetailRow("Canonical translation", entry.canonicalTranslation))
        }
        if (entry.sourceQuotes.isNotEmpty()) {
            add(
                EntryDetailRow(
                    "Source quotes",
                    entry.sourceQuotes.joinToString("\n\n") { quote ->
                        "${quote.source}\n${quote.quote}"
                    }
                )
            )
        }
    }
}

@Composable
fun EntryDetailTable(
    rows: List<EntryDetailRow>,
    modifier: Modifier = Modifier,
    onPlay: ((String) -> Unit)? = null
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(18.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(rows, key = { index, row -> "${row.label}_$index" }) { _, row ->
                EntryDetailTableRow(row = row, onPlay = onPlay)
            }
        }
    }
}

private fun formatExpressionMeanings(values: List<AppExpressionMeaning>): String {
    return values.joinToString("\n") {
        if (it.meaning.isBlank()) {
            it.expression
        } else {
            "${it.expression}: ${it.meaning}"
        }
    }
}

private fun formatVerbPrepositionDetails(values: List<AppVerbPrepositionDetail>): String {
    return values.joinToString("\n\n") { item ->
        buildString {
            append(item.expression)
            if (item.meaningEn.isNotBlank() || item.meaningJa.isNotBlank()) {
                append("\n")
                if (item.meaningEn.isNotBlank()) append("E: ${item.meaningEn}")
                if (item.meaningEn.isNotBlank() && item.meaningJa.isNotBlank()) append(" / ")
                if (item.meaningJa.isNotBlank()) append("J: ${item.meaningJa}")
            }
            if (item.exampleEn.isNotBlank() || item.exampleJa.isNotBlank()) {
                append("\n")
                if (item.exampleEn.isNotBlank()) append("Ex(E): ${item.exampleEn}")
                if (item.exampleEn.isNotBlank() && item.exampleJa.isNotBlank()) append("\n")
                if (item.exampleJa.isNotBlank()) append("Ex(J): ${item.exampleJa}")
            }
        }
    }
}

@Composable
private fun EntryDetailTableRow(
    row: EntryDetailRow,
    onPlay: ((String) -> Unit)?
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(0.34f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!row.playText.isNullOrBlank() && onPlay != null) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                            .clickable { onPlay(row.playText) }
                    )
                }
            }
            Text(
                text = row.value,
                modifier = Modifier.weight(0.66f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
