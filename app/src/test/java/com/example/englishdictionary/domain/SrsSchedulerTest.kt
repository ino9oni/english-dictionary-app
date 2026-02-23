package com.example.englishdictionary.domain

import com.example.englishdictionary.model.ReviewRating
import com.example.englishdictionary.model.SrsPhase
import com.example.englishdictionary.model.SrsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsSchedulerTest {
    @Test
    fun `new plus good moves to review with 2 day interval`() {
        val next = SrsScheduler.next(
            current = null,
            deckId = "mtg_story",
            entryId = "planeswalker",
            rating = ReviewRating.GOOD,
            todayEpochDay = 10,
            nowEpochMillis = 1000
        )

        assertEquals(SrsPhase.REVIEW, next.phase)
        assertEquals(2, next.intervalDays)
        assertEquals(12, next.dueEpochDay)
    }

    @Test
    fun `again increases lapse and sets learning`() {
        val current = SrsState(
            deckId = "mtg_story",
            entryId = "planeswalker",
            phase = SrsPhase.REVIEW,
            ease = 2.5,
            intervalDays = 5,
            dueEpochDay = 20,
            lastReviewedAtEpochMillis = 10,
            lapseCount = 1
        )

        val next = SrsScheduler.next(
            current = current,
            deckId = "mtg_story",
            entryId = "planeswalker",
            rating = ReviewRating.AGAIN,
            todayEpochDay = 21,
            nowEpochMillis = 2000
        )

        assertEquals(SrsPhase.LEARNING, next.phase)
        assertEquals(2, next.lapseCount)
        assertEquals(22, next.dueEpochDay)
        assertTrue(next.ease < 2.5)
    }
}
