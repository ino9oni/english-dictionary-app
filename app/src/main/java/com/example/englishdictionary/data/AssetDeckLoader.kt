package com.example.englishdictionary.data

import android.content.Context
import com.example.englishdictionary.model.AppDeck
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.model.AppExpressionMeaning
import com.example.englishdictionary.model.AppExample
import com.example.englishdictionary.model.AppSourceQuote
import com.example.englishdictionary.model.AppVerbPrepositionDetail
import com.example.englishdictionary.model.DeckFileJson
import com.example.englishdictionary.model.DeckIndexJson
import com.example.englishdictionary.model.EntrySource
import kotlinx.serialization.json.Json

class AssetDeckLoader(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    data class LoadedBundles(
        val decks: List<AppDeck>,
        val entriesByDeck: Map<String, List<AppEntry>>
    )

    fun load(): LoadedBundles {
        val indexString = context.assets.open("decks/index.json").bufferedReader().use { it.readText() }
        val index = json.decodeFromString<DeckIndexJson>(indexString)

        val decks = index.decks.map {
            AppDeck(
                deckId = it.deckId,
                name = it.name,
                description = it.description,
                languagePrimary = it.languagePrimary,
                languageSupport = it.languageSupport,
                version = it.version,
                tags = it.tags,
                imageName = it.imageName
            )
        }

        val entriesByDeck = mutableMapOf<String, List<AppEntry>>()
        for (deckItem in index.decks) {
            val assetPath = deckItem.path.removePrefix("assets/")
            val deckString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val deckFile = json.decodeFromString<DeckFileJson>(deckString)
            val mappedEntries = deckFile.entries.map { entry ->
                AppEntry(
                    deckId = deckItem.deckId,
                    entryId = entry.entryId,
                    term = entry.term,
                    displayTerm = if (entry.displayTerm.isBlank()) entry.term else entry.displayTerm,
                    pronunciationIpa = entry.pronunciationIpa,
                    pos = entry.pos,
                    meaningEn = entry.meaningEn,
                    meaningJa = entry.meaningJa,
                    prepositionUsages = entry.prepositionUsages,
                    verbPrepositionUsages = entry.verbPrepositionUsages.map {
                        AppExpressionMeaning(expression = it.expression, meaning = it.meaning)
                    },
                    verbPrepositionDetails = entry.verbPrepositionDetails.map {
                        AppVerbPrepositionDetail(
                            expression = it.expression,
                            meaningEn = it.meaningEn,
                            meaningJa = it.meaningJa,
                            exampleEn = it.exampleEn,
                            exampleJa = it.exampleJa
                        )
                    },
                    commonCollocations = entry.commonCollocations.map {
                        AppExpressionMeaning(expression = it.expression, meaning = it.meaning)
                    },
                    idioms = entry.idioms.map {
                        AppExpressionMeaning(expression = it.expression, meaning = it.meaning)
                    },
                    latinEtymology = entry.latinEtymology,
                    relatedTerms = entry.relatedTerms,
                    loreNote = entry.loreNote,
                    canonicalTranslation = entry.canonicalTranslation,
                    tags = entry.tags,
                    synonyms = entry.synonyms,
                    confusables = entry.confusables,
                    examples = entry.examples.map {
                        AppExample(textEn = it.textEn, textJa = it.textJa)
                    },
                    sourceQuotes = entry.sourceQuotes.map {
                        AppSourceQuote(source = it.source, quote = it.quote)
                    },
                    updatedAt = entry.updatedAt,
                    source = EntrySource.BUNDLED
                )
            }
            entriesByDeck[deckItem.deckId] = mappedEntries
        }

        return LoadedBundles(decks = decks, entriesByDeck = entriesByDeck)
    }
}
