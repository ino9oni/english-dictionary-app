package com.example.englishdictionary.model

import kotlinx.serialization.Serializable

enum class EntrySource {
    BUNDLED,
    USER
}

data class AppDeck(
    val deckId: String,
    val name: String,
    val description: String,
    val languagePrimary: String,
    val languageSupport: List<String>,
    val version: Int,
    val tags: List<String>,
    val imageName: String = ""
)

@Serializable
data class AppExample(
    val textEn: String,
    val textJa: String?
)

@Serializable
data class AppSourceQuote(
    val source: String,
    val quote: String
)

@Serializable
data class AppExpressionMeaning(
    val expression: String,
    val meaning: String = ""
)

@Serializable
data class AppVerbPrepositionDetail(
    val expression: String,
    val meaningEn: String = "",
    val meaningJa: String = "",
    val exampleEn: String = "",
    val exampleJa: String = ""
)

data class AppEntry(
    val deckId: String,
    val entryId: String,
    val term: String,
    val displayTerm: String,
    val pronunciationIpa: String = "",
    val pos: String,
    val meaningEn: String,
    val meaningJa: String,
    val prepositionUsages: List<String> = emptyList(),
    val verbPrepositionUsages: List<AppExpressionMeaning> = emptyList(),
    val verbPrepositionDetails: List<AppVerbPrepositionDetail> = emptyList(),
    val commonCollocations: List<AppExpressionMeaning> = emptyList(),
    val idioms: List<AppExpressionMeaning> = emptyList(),
    val latinEtymology: String = "",
    val relatedTerms: List<String> = emptyList(),
    val loreNote: String,
    val canonicalTranslation: String,
    val tags: List<String>,
    val synonyms: List<String>,
    val confusables: List<String>,
    val examples: List<AppExample>,
    val sourceQuotes: List<AppSourceQuote>,
    val updatedAt: String,
    val source: EntrySource
)

enum class ReviewRating {
    AGAIN,
    GOOD,
    EASY
}

enum class SrsPhase {
    NEW,
    LEARNING,
    REVIEW
}

data class SrsState(
    val deckId: String,
    val entryId: String,
    val phase: SrsPhase,
    val ease: Double,
    val intervalDays: Int,
    val dueEpochDay: Long,
    val lastReviewedAtEpochMillis: Long?,
    val lapseCount: Int
)
