package com.example.snapspin.data.vision

import com.example.snapspin.data.vision.dto.AnnotateImageResponseDto
import com.example.snapspin.domain.model.AlbumIdentity

/**
 * Convierte la respuesta de Cloud Vision en el texto con el que buscar en Discogs.
 *
 * `bestGuessLabels` es literalmente la conjetura de Google sobre qué es la imagen y, en portadas
 * de discos, suele venir ya como "grupo + título" ("nevermind nirvana"). Se pasa tal cual a la
 * búsqueda de texto libre de Discogs, que ordena por relevancia y resuelve el emparejamiento
 * mucho mejor que cualquier heurística nuestra.
 *
 * Si Google no arriesga una conjetura, se recurre a las entidades reconocidas, descartando las
 * genéricas ("álbum", "vinilo", "rock"...) que no ayudan a acotar la búsqueda.
 */
internal object CoverIdentityParser {

    fun parse(response: AnnotateImageResponseDto): AlbumIdentity? {
        val web = response.webDetection ?: return null

        web.bestGuessLabels
            .firstOrNull { it.label.isNotBlank() }
            ?.label
            ?.collapseSpaces()
            ?.let { return AlbumIdentity(it) }

        val entities = web.webEntities
            .filter { it.description.isNotBlank() && !it.description.isGeneric() }
            .sortedByDescending { it.score }
            .take(MAX_ENTITY_TERMS)
            .map { it.description.trim() }

        return entities
            .takeIf { it.isNotEmpty() }
            ?.let { AlbumIdentity(it.joinToString(" ").collapseSpaces()) }
    }

    private fun String.isGeneric() = lowercase().trim() in GENERIC_TERMS

    private fun String.collapseSpaces() = replace(WHITESPACE, " ").trim()

    private const val MAX_ENTITY_TERMS = 3

    private val WHITESPACE = """\s+""".toRegex()

    /** Términos que Vision devuelve para casi cualquier portada y que sólo ensucian la búsqueda. */
    private val GENERIC_TERMS = setOf(
        "album", "álbum", "vinyl", "vinilo", "lp", "cd", "record", "records", "music", "música",
        "phonograph record", "lp record", "compact disc", "album cover", "cover art", "artwork",
        "song", "single", "ep", "band", "musician", "artist", "musical ensemble", "poster",
        "graphic design", "font", "art", "rock", "pop", "jazz", "sound recording",
    )
}
