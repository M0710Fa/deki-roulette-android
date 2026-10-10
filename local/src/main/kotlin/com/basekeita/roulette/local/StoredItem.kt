package com.basekeita.roulette.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * 保存する項目の形（iOS の `StoredItem` と同じ `{id,label}`）。
 * 当たり・先頭・末尾の指定は含めない（前回の仕込みが次の起動に残らないようにする。SPEC §12）。
 */
@Serializable
data class StoredItem(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val label: String,
)

/** 保存したリストの形（iOS の `StoredSavedList` と同じ `{id,name,items}`）。 */
@Serializable
data class StoredSavedList(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val name: String,
    val items: List<StoredItem>,
)

/** UUID を文字列で読み書きする。UUID として読めない文字列は例外にし、JSON 全体を読めないものとして扱う（iOS の `JSONDecoder` と同じ）。 */
internal object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}
