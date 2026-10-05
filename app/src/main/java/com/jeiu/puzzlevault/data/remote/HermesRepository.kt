package com.jeiu.puzzlevault.data.remote

/**
 * Repository interface abstracting Hermes API calls behind a single contract.
 * Implementations can swap between real Retrofit calls and MockHermesService
 * without touching game logic.
 */
interface HermesRepository {

    suspend fun login(username: String): Map<String, Any>

    suspend fun createRoom(): Map<String, Any>

    suspend fun joinRoom(roomCode: String, playerId: String): Map<String, Any>

    suspend fun getPuzzleSync(): Map<String, Any>

    suspend fun submitSolution(puzzleId: String, payload: Map<String, Any>): Map<String, Any>
}