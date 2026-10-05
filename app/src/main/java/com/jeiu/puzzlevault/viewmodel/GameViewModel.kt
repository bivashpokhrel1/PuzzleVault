package com.jeiu.puzzlevault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeiu.puzzlevault.PuzzleVaultApplication
import com.jeiu.puzzlevault.data.local.LocalLevelStorage
import com.jeiu.puzzlevault.data.remote.HermesRepository
import com.jeiu.puzzlevault.model.GameState
import com.jeiu.puzzlevault.model.LevelModel
import com.jeiu.puzzlevault.util.DailyChallengeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {

    private val repository: HermesRepository
        get() = PuzzleVaultApplication.instance.hermesRepository

    private val storage: LocalLevelStorage
        get() = PuzzleVaultApplication.instance.levelStorage

    private val dailyGenerator = DailyChallengeGenerator()

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _levels = MutableStateFlow<List<LevelModel>>(emptyList())
    val levels: StateFlow<List<LevelModel>> = _levels.asStateFlow()

    private val _dailyChallenges =
        MutableStateFlow<List<DailyChallengeGenerator.ChallengePuzzle>>(emptyList())
    val dailyChallenges: StateFlow<List<DailyChallengeGenerator.ChallengePuzzle>> =
        _dailyChallenges.asStateFlow()

    // ── Auth ────────────────────────────────────────────────────────────

    fun login(username: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.login(username)
                _state.update {
                    it.copy(
                        playerId = result["playerId"] as? String ?: "",
                        username = result["username"] as? String ?: "",
                        isLoggedIn = result["status"] == "ok",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ── Room ────────────────────────────────────────────────────────────

    fun createRoom() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.createRoom()
                _state.update {
                    it.copy(
                        currentRoomCode = result["roomCode"] as? String,
                        roomPlayerCount = result["playerCount"] as? Int ?: 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun joinRoom(roomCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.joinRoom(roomCode, _state.value.playerId)
                _state.update {
                    it.copy(
                        currentRoomCode = if (result["status"] == "joined") roomCode else null,
                        roomPlayerCount = result["playerCount"] as? Int ?: 0,
                        isLoading = false,
                        errorMessage = if (result["status"] == "not_found") "Room not found" else null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ── Puzzle Sync ─────────────────────────────────────────────────────

    fun syncPuzzle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.getPuzzleSync()
                @Suppress("UNCHECKED_CAST")
                _state.update {
                    it.copy(
                        currentPuzzleId = result["currentPuzzle"] as? String,
                        puzzleState = result["state"] as? Map<String, Any> ?: emptyMap(),
                        isPuzzleSolved = result["isSolved"] as? Boolean ?: false,
                        attempts = result["attempts"] as? Int ?: 0,
                        maxAttempts = result["maxAttempts"] as? Int ?: 5,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // ── Levels ──────────────────────────────────────────────────────────

    fun loadLevels() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { storage.loadAllLevels() }
            _levels.value = loaded
        }
    }

    fun saveLevel(level: LevelModel) {
        viewModelScope.launch {
            storage.saveLevel(level)
            loadLevels()
        }
    }

    // ── Daily Challenges ────────────────────────────────────────────────

    fun loadDailyChallenges() {
        viewModelScope.launch {
            val challenges = dailyGenerator.generateDailyChallenges()
            _dailyChallenges.value = challenges
        }
    }

    fun getAdjustedTime(puzzle: DailyChallengeGenerator.ChallengePuzzle, failures: Int): Int {
        return dailyGenerator.getAdjustedTimeLimit(puzzle, failures)
    }

    // ── General ─────────────────────────────────────────────────────────

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
