package com.jeiu.puzzlevault.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface skeleton for Hermes API endpoints.
 * Swap MockHermesService with a real implementation behind this interface when ready.
 */
interface HermesApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body payload: JsonObject): Response<JsonObject>

    @POST("api/v1/rooms")
    suspend fun createRoom(@Body payload: JsonObject): Response<JsonObject>

    @POST("api/v1/rooms/{roomCode}/join")
    suspend fun joinRoom(
        @Path("roomCode") roomCode: String,
        @Body payload: JsonObject
    ): Response<JsonObject>

    @GET("api/v1/puzzles/sync")
    suspend fun getPuzzleSync(): Response<JsonObject>

    @POST("api/v1/puzzles/{puzzleId}/submit")
    suspend fun submitSolution(
        @Path("puzzleId") puzzleId: String,
        @Body payload: JsonObject
    ): Response<JsonObject>

    @GET("api/v1/leaderboard/{roomCode}")
    suspend fun getLeaderboard(
        @Path("roomCode") roomCode: String
    ): Response<JsonObject>
}
