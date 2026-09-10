package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.repository.CollectionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Completa el año de salida de los álbumes que aún no lo tienen.
 *
 * Discogs sólo da el año del álbum pidiendo cada obra por separado y limita a 60 peticiones por
 * minuto, así que se resuelve en segundo plano, por lotes pequeños y con una pausa entre ellos:
 * la lista se ve al instante con el año de la edición y los años reales van entrando solos.
 *
 * Como el año de salida de un álbum no cambia nunca, el repositorio lo cachea y esto sólo se
 * paga la primera vez.
 */
class ResolveAlbumYearsUseCase(
    private val collection: CollectionRepository,
    private val batchSize: Int = DEFAULT_BATCH,
    private val pauseBetweenBatchesMillis: Long = DEFAULT_PAUSE_MILLIS,
) {

    /** Emite pares (obra, año) según se van resolviendo. */
    operator fun invoke(items: List<CollectionItem>): Flow<Pair<Long, Int>> = flow {
        val pending = items
            .filter { it.albumYear == null }
            .mapNotNull { it.masterId }
            .distinct()

        pending.chunked(batchSize).forEachIndexed { index, batch ->
            if (index > 0) delay(pauseBetweenBatchesMillis)

            val resolved = coroutineScope {
                batch
                    .map { masterId ->
                        async {
                            // Una obra que falle no debe cortar la resolución de las demás.
                            masterId to runCatching { collection.albumYear(masterId) }.getOrNull()
                        }
                    }
                    .awaitAll()
            }

            resolved.forEach { (masterId, year) -> if (year != null) emit(masterId to year) }
        }
    }

    private companion object {
        const val DEFAULT_BATCH = 4

        /** 4 peticiones cada 5 s ≈ 48/min, por debajo del límite de Discogs. */
        const val DEFAULT_PAUSE_MILLIS = 5_000L
    }
}
