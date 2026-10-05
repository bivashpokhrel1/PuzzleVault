package com.jeiu.puzzlevault.data.local

import android.content.Context
import com.jeiu.puzzlevault.model.LevelModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JSON-based level serialization in internal app storage (context.filesDir).
 */
class LocalLevelStorage(private val context: Context) {

    private val levelsDir: File
        get() = File(context.filesDir, "levels").also { it.mkdirs() }

    suspend fun saveLevel(level: LevelModel) = withContext(Dispatchers.IO) {
        val file = File(levelsDir, "${level.levelId}.json")
        file.writeText(level.toJsonString())
    }

    suspend fun loadLevel(levelId: String): LevelModel? = withContext(Dispatchers.IO) {
        val file = File(levelsDir, "$levelId.json")
        if (file.exists()) {
            LevelModel.fromJsonString(file.readText())
        } else {
            null
        }
    }

    suspend fun loadAllLevels(): List<LevelModel> = withContext(Dispatchers.IO) {
        levelsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { LevelModel.fromJsonString(it.readText()) }
            ?: emptyList()
    }

    suspend fun deleteLevel(levelId: String) = withContext(Dispatchers.IO) {
        File(levelsDir, "$levelId.json").delete()
    }
}
