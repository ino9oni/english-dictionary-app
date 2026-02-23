package com.example.englishdictionary.domain

import com.example.englishdictionary.model.ReviewRating
import com.example.englishdictionary.model.SrsPhase
import com.example.englishdictionary.model.SrsState
import kotlin.math.max
import kotlin.math.roundToInt

object SrsScheduler {
    fun next(
        current: SrsState?,
        deckId: String,
        entryId: String,
        rating: ReviewRating,
        todayEpochDay: Long,
        nowEpochMillis: Long
    ): SrsState {
        val base = current ?: SrsState(
            deckId = deckId,
            entryId = entryId,
            phase = SrsPhase.NEW,
            ease = 2.5,
            intervalDays = 0,
            dueEpochDay = todayEpochDay,
            lastReviewedAtEpochMillis = null,
            lapseCount = 0
        )

        val reviewedBase = base.copy(lastReviewedAtEpochMillis = nowEpochMillis)

        return when (rating) {
            ReviewRating.AGAIN -> reviewedBase.copy(
                phase = SrsPhase.LEARNING,
                ease = max(1.3, base.ease - 0.2),
                intervalDays = 1,
                dueEpochDay = todayEpochDay + 1,
                lapseCount = base.lapseCount + 1
            )

            ReviewRating.GOOD -> {
                if (base.phase == SrsPhase.NEW || base.phase == SrsPhase.LEARNING) {
                    reviewedBase.copy(
                        phase = SrsPhase.REVIEW,
                        ease = base.ease + 0.05,
                        intervalDays = 2,
                        dueEpochDay = todayEpochDay + 2
                    )
                } else {
                    val nextInterval = max(base.intervalDays + 1, (base.intervalDays * base.ease).roundToInt())
                    reviewedBase.copy(
                        phase = SrsPhase.REVIEW,
                        ease = base.ease + 0.05,
                        intervalDays = nextInterval,
                        dueEpochDay = todayEpochDay + nextInterval
                    )
                }
            }

            ReviewRating.EASY -> {
                if (base.phase == SrsPhase.NEW || base.phase == SrsPhase.LEARNING) {
                    reviewedBase.copy(
                        phase = SrsPhase.REVIEW,
                        ease = base.ease + 0.15,
                        intervalDays = 4,
                        dueEpochDay = todayEpochDay + 4
                    )
                } else {
                    val easyEase = base.ease + 0.15
                    val nextInterval = max(base.intervalDays + 2, (base.intervalDays * (easyEase + 0.15)).roundToInt())
                    reviewedBase.copy(
                        phase = SrsPhase.REVIEW,
                        ease = easyEase,
                        intervalDays = nextInterval,
                        dueEpochDay = todayEpochDay + nextInterval
                    )
                }
            }
        }
    }
}
