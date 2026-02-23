package com.example.englishdictionary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishdictionary.data.DictionaryRepository
import com.example.englishdictionary.domain.SearchRanker
import com.example.englishdictionary.model.AppDeck
import com.example.englishdictionary.model.AppEntry
import com.example.englishdictionary.model.ReviewRating
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val repository: DictionaryRepository
) : ViewModel() {
    val decks: StateFlow<List<AppDeck>> = repository.observeDecks().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val deckWallpaperUris: StateFlow<Map<String, String>> = repository.observeDeckWallpaperUris().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    val deckEntryCounts: StateFlow<Map<String, Int>> = repository.observeDeckEntryCounts().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun observeDeckEntries(deckId: String) = repository.observeEntries(deckId)

    fun observeEntry(deckId: String, entryId: String) = repository.observeEntry(deckId, entryId)

    fun observeDueEntries(deckId: String) = repository.observeDueEntries(deckId)

    fun observeWrongCounts(deckId: String) = repository.observeWrongCounts(deckId)

    fun filterEntries(entries: List<AppEntry>, query: String): List<AppEntry> {
        return SearchRanker.search(entries, query)
    }

    fun review(deckId: String, entryId: String, rating: ReviewRating) {
        viewModelScope.launch {
            repository.reviewEntry(deckId, entryId, rating)
        }
    }

    fun recordQuizAnswer(deckId: String, entryId: String, known: Boolean) {
        viewModelScope.launch {
            repository.recordQuizAnswer(deckId, entryId, known)
        }
    }

    fun setDeckWallpaper(deckId: String, uriString: String?) {
        viewModelScope.launch {
            runCatching {
                repository.setDeckWallpaperUri(deckId, uriString)
            }.onFailure {
                _events.emit("Failed to update deck wallpaper")
            }
        }
    }

    class Factory(
        private val repository: DictionaryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
                return DictionaryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
