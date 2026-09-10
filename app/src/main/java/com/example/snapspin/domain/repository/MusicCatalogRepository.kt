package com.example.snapspin.domain.repository

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.CatalogQuery
import com.example.snapspin.domain.model.ReleaseCandidate
import com.example.snapspin.domain.model.ReleaseDetails

/**
 * Catálogo de discos. La implementación actual habla con Discogs, pero el dominio no lo sabe.
 *
 * Todas las funciones lanzan [AppError] ante cualquier fallo.
 */
interface MusicCatalogRepository {

    /** Busca ediciones que encajen con [query], ordenadas por relevancia del propio catálogo. */
    suspend fun search(query: CatalogQuery): List<ReleaseCandidate>

    /** Detalle de una edición, incluido su precio de mercado. */
    suspend fun releaseDetails(releaseId: Long): ReleaseDetails
}
