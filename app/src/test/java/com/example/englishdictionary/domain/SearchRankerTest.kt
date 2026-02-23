package com.example.englishdictionary.domain

import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.model.EntrySource
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankerTest {
    @Test
    fun `exact match ranks before prefix and substring`() {
        val entries = listOf(
            entry(term = "walk"),
            entry(term = "planeswalker"),
            entry(term = "walker")
        )

        val result = SearchRanker.search(entries, "walk")

        assertEquals("walk", result[0].term)
        assertEquals("walker", result[1].term)
        assertEquals("planeswalker", result[2].term)
    }

    @Test
    fun `normalization removes punctuation and case`() {
        val entries = listOf(entry(term = "Planes-Walker"))

        val result = SearchRanker.search(entries, "planes walker")

        assertEquals(1, result.size)
        assertEquals("Planes-Walker", result[0].term)
    }

    private fun entry(term: String): AppEntry {
        return AppEntry(
            deckId = "mtg_story",
            entryId = term,
            term = term,
            displayTerm = term,
            pos = "noun",
            meaningEn = "",
            meaningJa = "",
            loreNote = "",
            canonicalTranslation = "",
            tags = emptyList(),
            synonyms = emptyList(),
            confusables = emptyList(),
            examples = emptyList(),
            sourceQuotes = emptyList(),
            updatedAt = "",
            source = EntrySource.BUNDLED
        )
    }
}
