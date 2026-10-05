@file:UseSerializers(UUIDSerializer::class)

package com.jeiu.puzzlevault.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.UUID

// ── Custom UUID serializer ──────────────────────────────────────────────────

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

// ── Domain models ──────────────────────────────────────────────────────────

@Serializable
data class PuzzleItem(
    val id: String,
    val type: String,
    val xGrid: Int,
    val yGrid: Int
)

@Serializable
data class LevelModel(
    val levelId: String,
    val title: String,
    val difficulty: String,
    val items: List<PuzzleItem>
) {
    fun toJsonString(): String = Json.encodeToString(serializer(), this)

    companion object {
        fun fromJsonString(jsonString: String): LevelModel =
            Json.decodeFromString<LevelModel>(jsonString)
    }
}
