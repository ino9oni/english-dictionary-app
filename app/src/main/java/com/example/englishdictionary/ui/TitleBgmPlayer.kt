package com.example.englishdictionary.ui

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class TitleBgmPlayer(
    private val context: Context
) {
    private var player: MediaPlayer? = null
    private var released = false

    fun start() {
        if (released) return
        if (player == null) {
            player = createPlayer() ?: return
        }
        runCatching {
            val current = player ?: return
            if (!current.isPlaying) {
                current.start()
            }
        }
    }

    fun pause() {
        if (released) return
        runCatching {
            val current = player ?: return
            if (current.isPlaying) {
                current.pause()
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        val current = player
        player = null
        runCatching {
            current?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        }
    }

    private fun createPlayer(): MediaPlayer? {
        val assetPath = resolveTitleBgmAsset(context) ?: return null
        val assetFile = copyAssetToCache(assetPath) ?: return null
        return runCatching {
            MediaPlayer().apply {
                setDataSource(assetFile.absolutePath)
                isLooping = true
                setVolume(0.55f, 0.55f)
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    true
                }
                prepare()
            }
        }.getOrNull()
    }

    private fun copyAssetToCache(assetPath: String): File? {
        return runCatching {
            val safeName = assetPath.replace('/', '_')
            val dir = File(context.cacheDir, "bgm_cache").apply { mkdirs() }
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
}
