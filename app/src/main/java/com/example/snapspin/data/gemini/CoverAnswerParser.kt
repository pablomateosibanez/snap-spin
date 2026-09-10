package com.example.snapspin.data.gemini

import com.example.snapspin.data.gemini.dto.CoverAnswerDto
import com.example.snapspin.domain.model.AlbumIdentity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Convierte la respuesta del modelo en la consulta que se manda a Discogs.
 *
 * Se aísla del SDK a propósito: es la única parte con lógica y así se puede probar sin red.
 */
internal object CoverAnswerParser {

    fun parse(raw: String?, json: Json): AlbumIdentity? {
        val payload = raw?.stripCodeFence()?.takeIf { it.isNotBlank() } ?: return null

        val answer = try {
            json.decodeFromString<CoverAnswerDto>(payload)
        } catch (e: SerializationException) {
            return null
        }

        if (!answer.isAlbumCover) return null

        val query = listOf(answer.artist, answer.album)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(WHITESPACE, " ")
            .trim()

        return query.takeIf { it.isNotBlank() }?.let(::AlbumIdentity)
    }

    /** Algunos modelos envuelven el JSON en un bloque markdown pese a pedirlo en crudo. */
    private fun String.stripCodeFence(): String = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    private val WHITESPACE = """\s+""".toRegex()
}
