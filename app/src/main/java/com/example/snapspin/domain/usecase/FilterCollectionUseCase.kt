package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.CollectionOrder
import java.text.Normalizer

/**
 * Filtra y ordena el listado de la colección.
 *
 * Es una función pura sobre la lista ya descargada: buscar y reordenar no cuesta ni una petición,
 * así que la lista responde al instante mientras se escribe.
 *
 * La búsqueda mira grupo, título, géneros, estilos y el año de salida del álbum, de forma que
 * "soul", "1977" o "breeders" son consultas igual de válidas. Además reconoce rangos de años
 * dentro de la propia consulta ("1960-1980", "desde 1970"), combinables con el texto.
 */
class FilterCollectionUseCase {

    operator fun invoke(
        items: List<CollectionItem>,
        query: String,
        order: CollectionOrder,
    ): List<CollectionItem> {
        val parsed = CollectionQueryParser.parse(query)
        val needle = parsed.text.normalise()

        val filtered = items
            .filter { item -> parsed.yearRange?.contains(item.displayYear) ?: true }
            .filter { item -> needle.isBlank() || item.searchableText().contains(needle) }

        return when (order) {
            CollectionOrder.RECENT_FIRST -> filtered.sortedBy { it.position }
            CollectionOrder.OLDEST_FIRST -> filtered.sortedByDescending { it.position }
            CollectionOrder.ARTIST -> filtered.sortedWith(
                compareBy({ it.artist.normalise() }, { it.title.normalise() })
            )
            CollectionOrder.TITLE -> filtered.sortedWith(
                compareBy({ it.title.normalise() }, { it.artist.normalise() })
            )
        }
    }

    /** Un disco sin año conocido no puede entrar en un rango: se descarta mientras esté activo. */
    private fun IntRange.contains(year: Int?): Boolean = year != null && year in this

    private fun CollectionItem.searchableText(): String = buildList {
        add(artist)
        add(title)
        addAll(genres)
        addAll(styles)
        displayYear?.let { add(it.toString()) }
    }.joinToString(" ").normalise()

    /** Compara sin distinguir mayúsculas ni tildes: "Sabina" encuentra "Sabiná" y "SABINA". */
    private fun String.normalise(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()

    private companion object {
        val DIACRITICS = """\p{Mn}+""".toRegex()
    }
}
