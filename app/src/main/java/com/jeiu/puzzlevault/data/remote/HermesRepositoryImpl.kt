package com.jeiu.puzzlevault.data.remote

import android.util.Log
import com.jeiu.puzzlevault.MockHermesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Tries the real Retrofit [HermesApi] first; falls back to [MockHermesService]
 * when the network is unreachable or the API returns an error — upholding the
 * offline-first principle in AGENTS.md rule #2.
 */
class HermesRepositoryImpl(
    private val api: HermesApi,
    private val mockService: MockHermesService
) : HermesRepository {

    companion object {
        private const val TAG = "PUZZLE_VAULT"
    }

    override suspend fun login(username: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject { put("username", username) }
            val response = api.login(payload)
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "login: real API success")
                jsonObjectToMap(response.body()!!)
            } else {
                Log.w(TAG, "login: API returned ${response.code()}, falling back to mock")
                mockService.login(username)
            }
        } catch (e: Exception) {
            Log.w(TAG, "login: API call failed (${e.message}), falling back to mock")
            mockService.login(username)
        }
    }

    override suspend fun createRoom(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject { put("type", "standard") }
            val response = api.createRoom(payload)
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "createRoom: real API success")
                jsonObjectToMap(response.body()!!)
            } else {
                Log.w(TAG, "createRoom: API returned ${response.code()}, falling back to mock")
                mockService.createRoom()
            }
        } catch (e: Exception) {
            Log.w(TAG, "createRoom: API call failed (${e.message}), falling back to mock")
            mockService.createRoom()
        }
    }

    override suspend fun joinRoom(roomCode: String, playerId: String): Map<String, Any> =
        withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject { put("playerId", playerId) }
                val response = api.joinRoom(roomCode, payload)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "joinRoom: real API success")
                    jsonObjectToMap(response.body()!!)
                } else {
                    Log.w(TAG, "joinRoom: API returned ${response.code()}, falling back to mock")
                    mockService.joinRoom(roomCode, playerId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "joinRoom: API call failed (${e.message}), falling back to mock")
                mockService.joinRoom(roomCode, playerId)
            }
        }

    override suspend fun getPuzzleSync(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPuzzleSync()
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "getPuzzleSync: real API success")
                jsonObjectToMap(response.body()!!)
            } else {
                Log.w(TAG, "getPuzzleSync: API returned ${response.code()}, falling back to mock")
                mockService.getPuzzleSync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getPuzzleSync: API call failed (${e.message}), falling back to mock")
            mockService.getPuzzleSync()
        }
    }

    override suspend fun submitSolution(
        puzzleId: String,
        payload: Map<String, Any>
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = mapToJsonObject(payload)
            val response = api.submitSolution(puzzleId, jsonPayload)
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "submitSolution: real API success")
                jsonObjectToMap(response.body()!!)
            } else {
                Log.w(TAG, "submitSolution: API returned ${response.code()}, falling back to mock")
                mockService.submitSolution(puzzleId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "submitSolution: API call failed (${e.message}), falling back to mock")
            mockService.submitSolution(puzzleId)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun jsonObjectToMap(json: JsonObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for ((key, element) in json) {
            when {
                element is kotlinx.serialization.json.JsonPrimitive -> {
                    val prim = element as kotlinx.serialization.json.JsonPrimitive
                    map[key] = when {
                        prim.isString -> prim.content
                        prim.content == "true" || prim.content == "false" ->
                            prim.content.toBooleanStrict()
                        else -> prim.content.toIntOrNull() ?: prim.content.toDoubleOrNull()
                        ?: prim.content
                    }
                }
            }
        }
        return map
    }

    private fun mapToJsonObject(map: Map<String, Any>): JsonObject {
        val builder = buildJsonObject { }
        val result = kotlinx.serialization.json.buildJsonObject {
            for ((key, value) in map) {
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    else -> put(key, value.toString())
                }
            }
        }
        return result
    }
}