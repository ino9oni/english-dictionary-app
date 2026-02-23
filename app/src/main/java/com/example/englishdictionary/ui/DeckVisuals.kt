package com.example.englishdictionary.ui

import androidx.compose.ui.graphics.Color
import com.example.englishdictionary.R

data class DeckPalette(
    val primary: Color,
    val container: Color,
    val accent: Color
)

fun deckPalette(deckId: String): DeckPalette {
    return when (deckId) {
        "mtg_story" -> DeckPalette(
            primary = Color(0xFF5A3A1F),
            container = Color(0xFFF8F0E4),
            accent = Color(0xFF8E44AD)
        )

        "toeic_business" -> DeckPalette(
            primary = Color(0xFF1C3E5A),
            container = Color(0xFFEAF2F8),
            accent = Color(0xFF0F6C5D)
        )

        "eiken_pre2" -> DeckPalette(
            primary = Color(0xFF2D4A2C),
            container = Color(0xFFF0F7EC),
            accent = Color(0xFF1F6D8C)
        )

        else -> DeckPalette(
            primary = Color(0xFF2D3748),
            container = Color(0xFFF3F4F6),
            accent = Color(0xFF2563EB)
        )
    }
}

fun deckImageRes(imageName: String, deckId: String): Int {
    return when {
        imageName == "deck_mtg_fantasy" || deckId == "mtg_story" -> R.drawable.deck_mtg_fantasy
        imageName == "deck_toeic_business" || deckId == "toeic_business" -> R.drawable.deck_toeic_business
        imageName == "deck_eiken_pre2" || deckId == "eiken_pre2" -> R.drawable.deck_eiken_pre2
        else -> R.drawable.deck_default
    }
}

fun deckWallpaperRes(deckId: String): Int {
    return when (deckId) {
        "mtg_story" -> R.drawable.wallpaper_mtg_landscape
        "toeic_business" -> R.drawable.wallpaper_toeic_landscape
        "eiken_pre2" -> R.drawable.wallpaper_eiken_landscape
        else -> R.drawable.wallpaper_default_landscape
    }
}
