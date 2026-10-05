package com.jeiu.puzzlevault

import kotlinx.coroutines.delay

/**
 * Simulates backend room management and state sync with network delays.
 * Ready to be swapped with real Hermes API via Retrofit when available.
 */
class MockHermesService {

    private val rooms = mutableMapOf<String, MutableList<String>>()

    suspend fun login(username: String): Map<String, Any> {
        delay(800) // simulated network latency
        return mapOf(
            "playerId" to "player_${username.hashCode().toUInt().toString(36)}",
            "username" to username,
            "token"    to "mock_token_${System.currentTimeMillis()}",
            "status"   to "ok"
        )
    }

    suspend fun createRoom(): Map<String, Any> {
        delay(600)
        val roomCode = "4829"
        rooms[roomCode] = mutableListOf()
        return mapOf(
            "roomCode" to roomCode,
            "playerCount" to 0,
            "status" to "created"
        )
    }

    suspend fun joinRoom(roomCode: String, playerId: String): Map<String, Any> {
        delay(500)
        val room = rooms[roomCode]
        return if (room != null) {
            room.add(playerId)
            mapOf(
                "roomCode" to roomCode,
                "playerCount" to room.size,
                "status" to "joined"
            )
        } else {
            mapOf(
                "roomCode" to roomCode,
                "status" to "not_found"
            )
        }
    }

    suspend fun getPuzzleSync(): Map<String, Any> {
        delay(400)
        return mapOf(
            "currentPuzzle" to "wire_circuit_1",
            "state" to mapOf(
                "connections" to listOf(
                    mapOf("from" to "A1", "to" to "B3"),
                    mapOf("from" to "C2", "to" to "D1")
                )
            ),
            "isSolved" to false,
            "attempts" to 2,
            "maxAttempts" to 5
        )
    }

    suspend fun submitSolution(puzzleId: String): Map<String, Any> {
        delay(500)
        return mapOf(
            "puzzleId" to puzzleId,
            "status" to "accepted",
            "score" to 100,
            "timestamp" to System.currentTimeMillis()
        )
    }
}
