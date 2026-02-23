package com.example.englishdictionary.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeckIndexJson(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("generated_at")
    val generatedAt: String,
    val decks: List<DeckIndexItemJson>
)

@Serializable
data class DeckIndexItemJson(
    @SerialName("deck_id")
    val deckId: String,
    val name: String,
    @SerialName("language_primary")
    val languagePrimary: String = "en",
    @SerialName("language_support")
    val languageSupport: List<String> = emptyList(),
    val version: Int,
    val path: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    @SerialName("image_name")
    val imageName: String = ""
)

@Serializable
data class DeckFileJson(
    @SerialName("schema_version")
    val schemaVersion: String,
    val deck: DeckHeaderJson,
    val entries: List<DeckEntryJson>
)

@Serializable
data class DeckHeaderJson(
    @SerialName("deck_id")
    val deckId: String,
    val name: String,
    val version: Int
)

@Serializable
data class DeckEntryJson(
    @SerialName("entry_id")
    val entryId: String,
    val term: String,
    @SerialName("display_term")
    val displayTerm: String = "",
    @SerialName("pronunciation_ipa")
    val pronunciationIpa: String = "",
    val pos: String = "",
    @SerialName("meaning_en")
    val meaningEn: String = "",
    @SerialName("meaning_ja")
    val meaningJa: String = "",
    @SerialName("preposition_usages")
    val prepositionUsages: List<String> = emptyList(),
    @SerialName("verb_preposition_usages")
    val verbPrepositionUsages: List<DeckExpressionMeaningJson> = emptyList(),
    @SerialName("verb_preposition_details")
    val verbPrepositionDetails: List<DeckVerbPrepositionDetailJson> = emptyList(),
    @SerialName("common_collocations")
    val commonCollocations: List<DeckExpressionMeaningJson> = emptyList(),
    val idioms: List<DeckExpressionMeaningJson> = emptyList(),
    @SerialName("latin_etymology")
    val latinEtymology: String = "",
    @SerialName("related_terms")
    val relatedTerms: List<String> = emptyList(),
    @SerialName("lore_note")
    val loreNote: String = "",
    @SerialName("canonical_translation")
    val canonicalTranslation: String = "",
    val tags: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val confusables: List<String> = emptyList(),
    val examples: List<DeckExampleJson> = emptyList(),
    @SerialName("source_quotes")
    val sourceQuotes: List<DeckSourceQuoteJson> = emptyList(),
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class DeckExpressionMeaningJson(
    val expression: String,
    val meaning: String = ""
)

@Serializable
data class DeckVerbPrepositionDetailJson(
    val expression: String,
    @SerialName("meaning_en")
    val meaningEn: String = "",
    @SerialName("meaning_ja")
    val meaningJa: String = "",
    @SerialName("example_en")
    val exampleEn: String = "",
    @SerialName("example_ja")
    val exampleJa: String = ""
)

@Serializable
data class DeckExampleJson(
    @SerialName("text_en")
    val textEn: String,
    @SerialName("text_ja")
    val textJa: String? = null
)

@Serializable
data class DeckSourceQuoteJson(
    val source: String,
    val quote: String
)
