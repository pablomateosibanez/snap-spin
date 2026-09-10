package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.EditionChoiceReason
import com.example.snapspin.domain.model.ReleaseCandidate
import com.example.snapspin.domain.model.ReleaseDetails
import com.example.snapspin.domain.model.SelectedEdition
import com.example.snapspin.domain.repository.MusicCatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Elige automáticamente la edición "estándar" del disco para evitarle al usuario el paso de
 * escoger entre las decenas de prensajes que devuelve una búsqueda.
 *
 * Dos reglas, en este orden:
 *
 * 1. **Qué disco es** lo decide el catálogo: el primer resultado es el más relevante, y sólo se
 *    consideran las demás ediciones si son prensajes de esa misma obra (mismo `master`). Así una
 *    búsqueda por texto libre no acaba registrando un recopilatorio que apareció más abajo.
 * 2. **Qué edición registrar**: entre las ediciones de ese disco, la del **precio mediano** de
 *    mercado; se descartan así tanto la reedición tirada de precio como el prensaje japonés de
 *    coleccionista. Si ninguna tiene precio publicado, la más extendida entre la comunidad.
 */
class SelectStandardEditionUseCase(
    private val catalog: MusicCatalogRepository,
    private val maxEditionsInspected: Int = DEFAULT_MAX_EDITIONS,
    private val maxParallelRequests: Int = DEFAULT_PARALLELISM,
) {

    suspend operator fun invoke(candidates: List<ReleaseCandidate>): SelectedEdition {
        val best = candidates.firstOrNull()
            ?: throw AppError.NotFound("ninguna edición del disco")

        val details = fetchDetails(shortlist(best, candidates))
        if (details.isEmpty()) throw AppError.NotFound("el detalle de la edición en el catálogo")

        if (details.size == 1) {
            return SelectedEdition(details.first(), EditionChoiceReason.ONLY_MATCH, details.size)
        }

        val priced = details
            .filter { (it.lowestPrice ?: 0.0) > 0.0 }
            .sortedBy { it.lowestPrice }

        return if (priced.isEmpty()) {
            SelectedEdition(details.first(), EditionChoiceReason.MOST_COMMON, details.size)
        } else {
            // Mediana: con un número par de precios se toma el inferior de los dos centrales,
            // que es el que mejor representa la edición "normal" del disco.
            val median = priced[(priced.size - 1) / 2]
            SelectedEdition(median, EditionChoiceReason.MEDIAN_PRICE, details.size)
        }
    }

    /**
     * Ediciones a comparar: las de la misma obra que el resultado más relevante, empezando por
     * las que más gente tiene. Sin `master` no hay con qué agrupar, así que se registra
     * directamente ese resultado más relevante.
     */
    private fun shortlist(
        best: ReleaseCandidate,
        candidates: List<ReleaseCandidate>,
    ): List<ReleaseCandidate> {
        val master = best.masterId ?: return listOf(best)
        return candidates
            .filter { it.masterId == master }
            .sortedByDescending { it.communityHave }
            .take(maxEditionsInspected)
    }

    private suspend fun fetchDetails(shortlist: List<ReleaseCandidate>): List<ReleaseDetails> =
        coroutineScope {
            val gate = Semaphore(maxParallelRequests)
            shortlist
                .map { candidate ->
                    async {
                        // Una edición que falle (borrada, bloqueada por región...) no debe tumbar
                        // el registro entero: simplemente no entra en la comparación.
                        runCatching { gate.withPermit { catalog.releaseDetails(candidate.id) } }
                            .getOrNull()
                    }
                }
                .mapNotNull { it.await() }
        }

    private companion object {
        const val DEFAULT_MAX_EDITIONS = 8
        const val DEFAULT_PARALLELISM = 4
    }
}
