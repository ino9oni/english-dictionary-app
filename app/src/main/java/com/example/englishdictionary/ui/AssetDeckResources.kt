package com.example.englishdictionary.ui

import android.content.Context

private object AssetDeckResourceCache {
    private var bgFiles: List<String>? = null
    private var seFiles: List<String>? = null
    private var bgmFiles: List<String>? = null

    fun getBgFiles(context: Context): List<String> {
        val current = bgFiles
        if (current != null) return current
        val loaded = context.assets.list("bg")?.toList().orEmpty()
        bgFiles = loaded
        return loaded
    }

    fun getSeFiles(context: Context): List<String> {
        val current = seFiles
        if (current != null) return current
        val loaded = context.assets.list("se")?.toList().orEmpty()
        seFiles = loaded
        return loaded
    }

    fun getBgmFiles(context: Context): List<String> {
        val current = bgmFiles
        if (current != null) return current
        val loaded = context.assets.list("bgm")?.toList().orEmpty()
        bgmFiles = loaded
        return loaded
    }
}

private val backgroundExt = setOf("png", "jpg", "jpeg", "webp")
private val soundExt = setOf("wav", "mp3", "ogg", "m4a")

private fun hasExt(fileName: String, allow: Set<String>): Boolean {
    val dot = fileName.lastIndexOf('.')
    if (dot <= 0 || dot >= fileName.lastIndex) return false
    val ext = fileName.substring(dot + 1).lowercase()
    return allow.contains(ext)
}

private fun stripExt(name: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot > 0) name.substring(0, dot) else name
}

private fun normalizeCompact(value: String): String {
    return value.lowercase().replace(Regex("[^a-z0-9]"), "")
}

private fun scoreDeckMatch(fileName: String, deckId: String): Int {
    val base = stripExt(fileName).lowercase()
    val deckCompact = normalizeCompact(deckId)
    if (deckCompact.isBlank()) return 0

    var score = 0
    if (normalizeCompact(base).contains(deckCompact)) {
        score += 10
    }

    val tokens = deckId.lowercase()
        .split('_', '-', ' ')
        .map { it.trim() }
        .filter { it.length >= 3 || it.any(Char::isDigit) }
    tokens.forEach { token ->
        if (base.contains(token)) {
            score += 3
        }
    }
    return score
}

fun resolveDeckBackgroundAsset(context: Context, deckId: String): String? {
    val files = AssetDeckResourceCache.getBgFiles(context)
    if (files.isEmpty()) return null

    val normalized = deckId.trim().lowercase()
    val imageCandidates = files.filter { hasExt(it, backgroundExt) }.sorted()

    val deckMatch = if (normalized.isBlank()) {
        null
    } else {
        val strict = imageCandidates.firstOrNull { name ->
            val lower = name.lowercase()
            lower.startsWith("bg_${normalized}.") ||
                lower.startsWith("${normalized}_bg.") ||
                lower.startsWith("default-${normalized}.")
        }
        if (strict != null) {
            strict
        } else {
            imageCandidates
                .map { it to scoreDeckMatch(it, normalized) }
                .sortedByDescending { it.second }
                .firstOrNull { it.second > 0 }
                ?.first
        }
    }
    if (deckMatch != null) return "bg/$deckMatch"

    val defaultMatch = imageCandidates.firstOrNull {
        val lower = it.lowercase()
        lower.startsWith("bg_default.") ||
            lower.startsWith("default.") ||
            lower.startsWith("default-")
    }
    return defaultMatch?.let { "bg/$it" }
}

private fun resolveDeckSeAsset(context: Context, deckId: String, kind: String): String? {
    val files = AssetDeckResourceCache.getSeFiles(context)
    if (files.isEmpty()) return null

    val normalized = deckId.trim().lowercase()
    val soundCandidates = files.filter { hasExt(it, soundExt) }

    val deckSpecificPrefixes = when (kind) {
        "correct" -> listOf(
            "se_correct_${normalized}.",
            "${normalized}_se_correct.",
            "correct_${normalized}.",
            "${normalized}_correct.",
            "good_${normalized}.",
            "${normalized}_good.",
            "ok_${normalized}.",
            "${normalized}_ok."
        )
        "incorrect" -> listOf(
            "se_incorrect_${normalized}.",
            "${normalized}_se_incorrect.",
            "incorrect_${normalized}.",
            "${normalized}_incorrect.",
            "notgood_${normalized}.",
            "${normalized}_notgood.",
            "ng_${normalized}.",
            "${normalized}_ng.",
            "wrong_${normalized}.",
            "${normalized}_wrong."
        )
        else -> emptyList()
    }
    val commonPrefixes = when (kind) {
        "correct" -> listOf("se_correct.", "correct.", "good.", "ok.")
        "incorrect" -> listOf("se_incorrect.", "incorrect.", "notgood.", "ng.", "wrong.")
        else -> emptyList()
    }

    val deckSpecific = soundCandidates.firstOrNull { name ->
        val lower = name.lowercase()
        deckSpecificPrefixes.any { lower.startsWith(it) }
    }
    if (deckSpecific != null) return "se/$deckSpecific"

    val common = soundCandidates.firstOrNull { name ->
        val lower = name.lowercase()
        commonPrefixes.any { lower.startsWith(it) }
    }
    return common?.let { "se/$it" }
}

fun resolveDeckCorrectSeAsset(context: Context, deckId: String): String? {
    return resolveDeckSeAsset(context, deckId, "correct")
}

fun resolveDeckIncorrectSeAsset(context: Context, deckId: String): String? {
    return resolveDeckSeAsset(context, deckId, "incorrect")
}

fun resolveTitleBgmAsset(context: Context): String? {
    val files = AssetDeckResourceCache.getBgmFiles(context)
    if (files.isEmpty()) return null
    val soundCandidates = files.filter { hasExt(it, soundExt) }.sorted()
    val preferred = soundCandidates.firstOrNull { name ->
        val lower = name.lowercase()
        lower.startsWith("title-bgm.") || lower.startsWith("title_bgm.")
    } ?: soundCandidates.firstOrNull()
    return preferred?.let { "bgm/$it" }
}
