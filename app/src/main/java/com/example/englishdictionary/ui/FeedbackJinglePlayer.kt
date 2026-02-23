package com.example.englishdictionary.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import java.io.File

class FeedbackJinglePlayer(
    private val context: Context,
    private val deckId: String
) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    private val handler = Handler(Looper.getMainLooper())
    private val activePlayers = mutableSetOf<MediaPlayer>()
    private var released = false

    fun playCorrect() {
        if (playAsset(resolveDeckCorrectSeAsset(context, deckId))) return
        playToneSequence(
            listOf(
                ToneGenerator.TONE_DTMF_5 to 90L,
                ToneGenerator.TONE_DTMF_7 to 90L,
                ToneGenerator.TONE_DTMF_9 to 140L
            )
        )
    }

    fun playIncorrect() {
        if (playAsset(resolveDeckIncorrectSeAsset(context, deckId))) return
        playToneSequence(listOf(ToneGenerator.TONE_PROP_NACK to 220L))
    }

    private fun playAsset(assetPath: String?): Boolean {
        if (released || assetPath.isNullOrBlank()) return false
        val assetFile = copyAssetToCache(assetPath) ?: return false
        return runCatching {
            val player = MediaPlayer()
            player.setDataSource(assetFile.absolutePath)
            player.setOnCompletionListener { completed ->
                completed.release()
                activePlayers.remove(completed)
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                activePlayers.remove(mp)
                true
            }
            player.prepare()
            activePlayers.add(player)
            player.start()
            true
        }.getOrDefault(false)
    }

    private fun copyAssetToCache(assetPath: String): File? {
        return runCatching {
            val safeName = assetPath.replace('/', '_')
            val dir = File(context.cacheDir, "se_cache").apply { mkdirs() }
            val outFile = File(dir, safeName)
            if (!outFile.exists()) {
                context.assets.open(assetPath).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            outFile
        }.getOrNull()
    }

    private fun playToneSequence(sequence: List<Pair<Int, Long>>) {
        if (released) return
        handler.removeCallbacksAndMessages(null)
        var offset = 0L
        sequence.forEach { (tone, duration) ->
            handler.postDelayed(
                {
                    if (!released) {
                        toneGenerator.startTone(tone, duration.toInt())
                    }
                },
                offset
            )
            offset += duration + 24L
        }
    }

    fun release() {
        released = true
        handler.removeCallbacksAndMessages(null)
        activePlayers.toList().forEach { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        }
        activePlayers.clear()
        toneGenerator.release()
    }
}
