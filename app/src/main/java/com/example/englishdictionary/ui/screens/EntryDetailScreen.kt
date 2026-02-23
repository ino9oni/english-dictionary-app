package com.example.englishdictionary.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishdictionary.ui.DeckWallpaperBackground
import com.example.englishdictionary.ui.DictionaryViewModel
import com.example.englishdictionary.ui.deckPalette
import com.example.englishdictionary.ui.swipeToBack
import java.util.Locale

@Composable
fun EntryDetailScreen(
    deckId: String,
    entryId: String,
    viewModel: DictionaryViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entry by viewModel.observeEntry(deckId, entryId).collectAsStateWithLifecycle(initialValue = null)
    val deckWallpaperUris by viewModel.deckWallpaperUris.collectAsStateWithLifecycle()
    val palette = deckPalette(deckId)

    var ttsReady by remember { mutableStateOf(false) }
    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun speakText(text: String) {
        if (!ttsReady) {
            Toast.makeText(context, "TTS is not ready yet.", Toast.LENGTH_SHORT).show()
            return
        }
        val languageResult = textToSpeech.setLanguage(Locale.US)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "US English voice data is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }
        val result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "entry_tts")
        if (result == TextToSpeech.ERROR) {
            Toast.makeText(context, "Failed to play speech.", Toast.LENGTH_SHORT).show()
        }
    }

    DeckWallpaperBackground(
        deckId = deckId,
        wallpaperUriString = deckWallpaperUris[deckId],
        modifier = Modifier
            .fillMaxSize()
            .swipeToBack(onBack = onBack)
    ) {
        if (entry == null) {
            Text("Entry not found", style = MaterialTheme.typography.headlineSmall)
            return@DeckWallpaperBackground
        }

        val data = entry!!
        val rows = remember(data) { buildEntryDetailRows(data) }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = data.displayTerm,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Swipe right to go back",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            EntryDetailTable(
                rows = rows,
                modifier = Modifier.fillMaxWidth(),
                onPlay = { text -> speakText(text) }
            )
        }
    }
}
