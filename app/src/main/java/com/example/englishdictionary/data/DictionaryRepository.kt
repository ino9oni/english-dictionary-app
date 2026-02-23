package com.example.englishdictionary.data

import android.content.Context
import android.net.Uri
import com.example.englishdictionary.data.local.AppDatabase
import com.example.englishdictionary.data.local.SrsStateEntity
import com.example.englishdictionary.data.local.StudyLogEntity
import com.example.englishdictionary.data.local.UserEntryEntity
import com.example.englishdictionary.domain.SrsScheduler
import com.example.englishdictionary.model.AppDeck
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.model.AppExample
import com.example.englishdictionary.model.AppSourceQuote
import com.example.englishdictionary.model.EntrySource
import com.example.englishdictionary.model.ReviewRating
import com.example.englishdictionary.model.SrsPhase
import com.example.englishdictionary.model.SrsState
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DictionaryRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val loader = AssetDeckLoader(context, json)
    private val initializeMutex = Mutex()
    private var initialized = false
    private val wallpaperPrefs = context.getSharedPreferences("deck_wallpapers", Context.MODE_PRIVATE)

    private val bundledDecks = MutableStateFlow<List<AppDeck>>(emptyList())
    private val bundledEntriesByDeck = MutableStateFlow<Map<String, List<AppEntry>>>(emptyMap())
    private val deckWallpaperUris = MutableStateFlow(loadDeckWallpaperUris())

    suspend fun initialize() {
        initializeMutex.withLock {
            if (initialized) return
            // Asset IO + JSON parsing can be heavy for large decks.
            // Run off the main thread to avoid startup jank/ANR.
            val loaded = withContext(Dispatchers.IO) {
                loader.load()
            }
            bundledDecks.value = loaded.decks
            bundledEntriesByDeck.value = loaded.entriesByDeck
            initialized = true
        }
    }

    fun observeDecks(): Flow<List<AppDeck>> = bundledDecks

    fun observeDeckWallpaperUris(): Flow<Map<String, String>> = deckWallpaperUris

    fun observeDeckEntryCounts(): Flow<Map<String, Int>> {
        return combine(
            bundledDecks,
            bundledEntriesByDeck,
            database.userEntryDao().observeAll()
        ) { decks, bundled, userEntries ->
            val userByDeck = userEntries.groupBy { it.deckId }
            val allDeckIds = linkedSetOf<String>().apply {
                addAll(decks.map { it.deckId })
                addAll(bundled.keys)
                addAll(userByDeck.keys)
            }

            allDeckIds.associateWith { deckId ->
                val merged = LinkedHashMap<String, AppEntry>()
                bundled[deckId].orEmpty().forEach { merged[it.entryId] = it }
                userByDeck[deckId].orEmpty().map { it.toAppEntry() }.forEach { merged[it.entryId] = it }
                merged.size
            }
        }
    }

    suspend fun setDeckWallpaperUri(deckId: String, uriString: String?) {
        val normalized = uriString?.trim().orEmpty().ifBlank { null }
        val previous = deckWallpaperUris.value[deckId]
        withContext(Dispatchers.IO) {
            wallpaperPrefs.edit().apply {
                if (normalized == null) {
                    remove(deckId)
                } else {
                    putString(deckId, normalized)
                }
            }.apply()
        }
        if (!previous.isNullOrBlank() && previous != normalized) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(previous),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        val updated = deckWallpaperUris.value.toMutableMap()
        if (normalized == null) {
            updated.remove(deckId)
        } else {
            updated[deckId] = normalized
        }
        deckWallpaperUris.value = updated.toMap()
    }

    fun observeEntries(deckId: String): Flow<List<AppEntry>> {
        return combine(
            bundledEntriesByDeck,
            database.userEntryDao().observeByDeck(deckId)
        ) { bundled, userEntries ->
            val bundledList = bundled[deckId].orEmpty()
            val userList = userEntries.map { it.toAppEntry() }

            // Merge policy: bundled base + user override by entry_id.
            val merged = LinkedHashMap<String, AppEntry>()
            bundledList.forEach { merged[it.entryId] = it }
            userList.forEach { merged[it.entryId] = it }

            merged.values.sortedBy { it.term.lowercase() }
        }
    }

    fun observeEntry(deckId: String, entryId: String): Flow<AppEntry?> {
        return observeEntries(deckId).map { entries ->
            entries.firstOrNull { it.entryId == entryId }
        }
    }

    fun observeWrongCounts(deckId: String): Flow<Map<String, Int>> {
        return database.studyLogDao().observeWrongCounts(deckId).map { rows ->
            rows.associate { it.entryId to it.wrongCount }
        }
    }

    fun observeDueEntries(deckId: String): Flow<List<AppEntry>> {
        return combine(
            observeEntries(deckId),
            database.srsStateDao().observeByDeck(deckId)
        ) { entries, states ->
            val stateMap = states.associateBy { it.entryId }
            val today = LocalDate.now().toEpochDay()

            entries
                .filter { entry ->
                    val state = stateMap[entry.entryId]
                    state == null || state.dueEpochDay <= today
                }
                .sortedWith(
                    compareBy<AppEntry> { stateMap[it.entryId]?.dueEpochDay ?: Long.MIN_VALUE }
                        .thenBy { it.term.lowercase() }
                )
        }
    }

    suspend fun reviewEntry(deckId: String, entryId: String, rating: ReviewRating) {
        val now = Instant.now()
        val current = database.srsStateDao().get(deckId, entryId)?.toDomain()
        val next = SrsScheduler.next(
            current = current,
            deckId = deckId,
            entryId = entryId,
            rating = rating,
            todayEpochDay = LocalDate.now().toEpochDay(),
            nowEpochMillis = now.toEpochMilli()
        )

        database.srsStateDao().upsert(next.toEntity())
        database.studyLogDao().insert(
            StudyLogEntity(
                deckId = deckId,
                entryId = entryId,
                rating = rating.name,
                reviewedAtEpochMillis = now.toEpochMilli()
            )
        )
    }

    suspend fun recordQuizAnswer(deckId: String, entryId: String, known: Boolean) {
        database.studyLogDao().insert(
            StudyLogEntity(
                deckId = deckId,
                entryId = entryId,
                rating = if (known) "KNOWN" else "UNKNOWN",
                reviewedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    private fun loadDeckWallpaperUris(): Map<String, String> {
        return wallpaperPrefs.all.mapNotNull { (key, value) ->
            val uri = value as? String
            if (uri.isNullOrBlank()) null else key to uri
        }.toMap()
    }

    private fun UserEntryEntity.toAppEntry(): AppEntry {
        return AppEntry(
            deckId = deckId,
            entryId = entryId,
            term = term,
            displayTerm = displayTerm,
            pronunciationIpa = "",
            pos = pos,
            meaningEn = meaningEn,
            meaningJa = meaningJa,
            prepositionUsages = emptyList(),
            verbPrepositionUsages = emptyList(),
            verbPrepositionDetails = emptyList(),
            commonCollocations = emptyList(),
            idioms = emptyList(),
            latinEtymology = "",
            relatedTerms = emptyList(),
            loreNote = loreNote,
            canonicalTranslation = canonicalTranslation,
            tags = tagsCsv.toCsvList(),
            synonyms = synonymsCsv.toCsvList(),
            confusables = confusablesCsv.toCsvList(),
            examples = decodeExamples(examplesJson),
            sourceQuotes = decodeQuotes(sourceQuotesJson),
            updatedAt = updatedAt,
            source = EntrySource.USER
        )
    }

    private fun String.toCsvList(): List<String> {
        if (isBlank()) return emptyList()
        return split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun decodeExamples(raw: String): List<AppExample> {
        return runCatching {
            json.decodeFromString(ListSerializer(AppExample.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeQuotes(raw: String): List<AppSourceQuote> {
        return runCatching {
            json.decodeFromString(ListSerializer(AppSourceQuote.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun SrsStateEntity.toDomain(): SrsState {
        return SrsState(
            deckId = deckId,
            entryId = entryId,
            phase = runCatching { SrsPhase.valueOf(phase) }.getOrDefault(SrsPhase.NEW),
            ease = ease,
            intervalDays = intervalDays,
            dueEpochDay = dueEpochDay,
            lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
            lapseCount = lapseCount
        )
    }

    private fun SrsState.toEntity(): SrsStateEntity {
        return SrsStateEntity(
            deckId = deckId,
            entryId = entryId,
            phase = phase.name,
            ease = ease,
            intervalDays = intervalDays,
            dueEpochDay = dueEpochDay,
            lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
            lapseCount = lapseCount
        )
    }

}
