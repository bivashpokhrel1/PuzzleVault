package com.jeiu.puzzlevault.model

/**
 * Represents the full game state flowing through GameViewModel.
 */
data class GameState(
    val playerId: String = "",
    val username: String = "",
    val isLoggedIn: Boolean = false,
    val currentRoomCode: String? = null,
    val roomPlayerCount: Int = 0,
    val currentPuzzleId: String? = null,
    val puzzleState: Map<String, Any> = emptyMap(),
    val isPuzzleSolved: Boolean = false,
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
