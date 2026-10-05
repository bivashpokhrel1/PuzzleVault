package com.jeiu.puzzlevault.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Deterministic daily puzzle seed generator using today's date (yyyyMMdd)
 * as the seed for kotlin.random.Random.
 */
class DailyChallengeGenerator {

    data class ChallengePuzzle(
        val id: String,
        val type: String,
        val seedValue: Int,
        val baseTimeLimitSec: Int
    )

    private val puzzleTypes = listOf("wire_circuit", "keypad", "grid_slide", "sequence", "pattern_match")

    suspend fun generateDailyChallenges(): List<ChallengePuzzle> =
        withContext(Dispatchers.Default) {
            val seed = todaySeed()
            val random = kotlin.random.Random(seed)
            val count = 5

            puzzleTypes.shuffled(random).take(count).mapIndexed { index, type ->
                ChallengePuzzle(
                    id = "daily_${seed}_$index",
                    type = type,
                    seedValue = random.nextInt(1000, 9999),
                    baseTimeLimitSec = when (type) {
                        "wire_circuit" -> 120
                        "keypad" -> 90
                        "grid_slide" -> 150
                        "sequence" -> 60
                        "pattern_match" -> 100
                        else -> 90
                    }
                )
            }
        }

    fun getAdjustedTimeLimit(puzzle: ChallengePuzzle, playerFailures: Int): Int {
        // Scale: each failure adds 15s (grace period), capped at 2x base
        val bonus = (playerFailures * 15).coerceAtMost(puzzle.baseTimeLimitSec)
        return puzzle.baseTimeLimitSec + bonus
    }

    private fun todaySeed(): Int {
        val dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return dateString.toInt()
    }
}
