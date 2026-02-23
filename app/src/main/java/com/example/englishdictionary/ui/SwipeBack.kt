package com.example.englishdictionary.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.swipeToBack(
    enabled: Boolean = true,
    thresholdPx: Float = 110f,
    onBack: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled) {
        var dragX = 0f
        var dragY = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount ->
                dragX += dragAmount
                dragY += abs(dragAmount)
            },
            onDragEnd = {
                if (dragX > thresholdPx && dragX > dragY * 0.7f) {
                    onBack()
                }
                dragX = 0f
                dragY = 0f
            },
            onDragCancel = {
                dragX = 0f
                dragY = 0f
            }
        )
    }
}
