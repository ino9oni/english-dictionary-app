package com.example.englishdictionary.domain

import com.example.englishdictionary.model.AppEntry
import java.util.Locale

object SearchRanker {
    fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    fun search(entries: List<AppEntry>, query: String): List<AppEntry> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) {
            return entries.sortedBy { it.term.lowercase(Locale.US) }
        }

        return entries
            .mapNotNull { entry ->
                val normalizedTerm = normalize(entry.term)
                val score = when {
                    normalizedTerm == normalizedQuery -> 0
                    normalizedTerm.startsWith(normalizedQuery) -> 1
                    normalizedTerm.contains(normalizedQuery) -> 2
                    else -> return@mapNotNull null
                }
                score to entry
            }
            .sortedWith(compareBy<Pair<Int, AppEntry>> { it.first }.thenBy { it.second.term.lowercase(Locale.US) })
            .map { it.second }
    }
}
