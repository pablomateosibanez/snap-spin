package com.example.snapspin.data.discogs

import com.example.snapspin.domain.model.CatalogQuery
import com.example.snapspin.domain.model.ReleaseCandidate
import com.example.snapspin.domain.model.ReleaseDetails
import com.example.snapspin.domain.repository.MusicCatalogRepository

/** Implementación del catálogo sobre la API de Discogs. */
class DiscogsCatalogRepository(
    private val api: DiscogsApi,
    private val config: DiscogsConfig,
) : MusicCatalogRepository {

    override suspend fun search(query: CatalogQuery): List<ReleaseCandidate> {
        val parameters = when (query) {
            is CatalogQuery.ByBarcode -> mapOf("barcode" to query.barcode)
            is CatalogQuery.ByFreeText -> mapOf("q" to query.text)
        }
        return api.search(parameters).results.map { it.toDomain() }
    }

    override suspend fun releaseDetails(releaseId: Long): ReleaseDetails =
        api.release(releaseId).toDomain(config.currency)
}
