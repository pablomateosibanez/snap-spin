package com.example.snapspin.domain.usecase

/** Consulta del buscador ya interpretada: texto libre y, si se ha escrito, un rango de años. */
data class ParsedQuery(
    val text: String,
    val yearRange: IntRange?,
)

/**
 * Interpreta lo que se escribe en el buscador de la colección.
 *
 * Se reconoce un rango de años dentro de la propia consulta, en las formas en que uno lo
 * escribiría de corrido —"1960-1980", "1960 a 1980", "desde 1970", "hasta 1980"— y el resto del
 * texto sigue funcionando como búsqueda normal. Así "rock 1960-1980" combina las dos cosas sin
 * necesidad de más controles en pantalla.
 *
 * Un año suelto ("1974") no se trata como rango: sigue siendo una búsqueda de texto, que ya
 * encuentra los discos de ese año.
 */
internal object CollectionQueryParser {

    fun parse(raw: String): ParsedQuery {
        val query = raw.trim()

        for ((pattern, toRange) in PATTERNS) {
            val match = pattern.find(query) ?: continue
            val range = toRange(match) ?: continue
            return ParsedQuery(
                text = query.removeRange(match.range).replace(WHITESPACE, " ").trim(),
                yearRange = range,
            )
        }

        return ParsedQuery(text = query, yearRange = null)
    }

    private fun MatchResult.year(group: Int): Int? =
        groupValues.getOrNull(group)?.toIntOrNull()?.takeIf { it in MIN_YEAR..MAX_YEAR }

    /** El orden importa: primero el rango completo, que contiene a los parciales. */
    private val PATTERNS: List<Pair<Regex, (MatchResult) -> IntRange?>> = listOf(
        // "1960-1980", "1960 a 1980", "desde 1960 hasta 1980", "1960..1980"
        """(?:desde\s+)?(\d{4})\s*(?:-|–|—|\.\.|a|hasta|to)\s*(\d{4})"""
            .toRegex(RegexOption.IGNORE_CASE) to { match ->
            val from = match.year(1)
            val to = match.year(2)
            if (from != null && to != null) minOf(from, to)..maxOf(from, to) else null
        },
        // "desde 1970"
        """desde\s+(\d{4})""".toRegex(RegexOption.IGNORE_CASE) to { match ->
            match.year(1)?.let { it..MAX_YEAR }
        },
        // "hasta 1980"
        """hasta\s+(\d{4})""".toRegex(RegexOption.IGNORE_CASE) to { match ->
            match.year(1)?.let { MIN_YEAR..it }
        },
        // "1970-" (de ese año en adelante)
        """(\d{4})\s*[-–—]\s*(?!\d)""".toRegex() to { match ->
            match.year(1)?.let { it..MAX_YEAR }
        },
        // "-1980" (hasta ese año)
        """(?<!\d)[-–—]\s*(\d{4})""".toRegex() to { match ->
            match.year(1)?.let { MIN_YEAR..it }
        },
    )

    private const val MIN_YEAR = 1000
    private const val MAX_YEAR = 9999

    private val WHITESPACE = """\s+""".toRegex()
}
