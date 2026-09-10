package com.example.snapspin.fake

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.AlbumIdentity
import com.example.snapspin.domain.model.CatalogQuery
import com.example.snapspin.domain.model.CollectionFolder
import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.ReleaseCandidate
import com.example.snapspin.domain.model.ReleaseDetails
import com.example.snapspin.domain.repository.CollectionRepository
import com.example.snapspin.domain.repository.MusicCatalogRepository
import com.example.snapspin.domain.service.CoverRecognitionService

fun candidate(id: Long, have: Int = 0, masterId: Long? = 100L) = ReleaseCandidate(
    id = id,
    masterId = masterId,
    displayTitle = "Grupo - Disco",
    year = 1973,
    country = "Spain",
    formats = listOf("Vinyl", "LP"),
    thumbnailUrl = null,
    communityHave = have,
    communityWant = 0,
)

fun details(id: Long, price: Double?) = ReleaseDetails(
    id = id,
    artist = "Grupo",
    title = "Disco",
    year = 1973,
    country = "Spain",
    formats = listOf("Vinyl", "LP"),
    labels = listOf("Sello"),
    catalogNumber = "CAT-$id",
    coverImageUrl = null,
    lowestPrice = price,
    currency = "EUR",
    copiesForSale = 3,
)

class FakeCatalogRepository(
    var onSearch: (CatalogQuery) -> List<ReleaseCandidate> = { emptyList() },
    var detailsById: Map<Long, ReleaseDetails> = emptyMap(),
) : MusicCatalogRepository {

    val queries = mutableListOf<CatalogQuery>()

    override suspend fun search(query: CatalogQuery): List<ReleaseCandidate> {
        queries += query
        return onSearch(query)
    }

    override suspend fun releaseDetails(releaseId: Long): ReleaseDetails =
        detailsById[releaseId] ?: throw AppError.NotFound("la edición $releaseId")
}

class FakeCollectionRepository(
    private val folder: CollectionFolder = CollectionFolder(1L, "Uncategorized", 10),
    var existing: List<CollectionInstance> = emptyList(),
) : CollectionRepository {

    val added = mutableListOf<Pair<Long, Long>>()
    val removed = mutableListOf<CollectionInstance>()

    var items: List<CollectionItem> = emptyList()
    var refreshCount = 0
    var albumYears: Map<Long, Int> = emptyMap()
    val albumYearRequests = mutableListOf<Long>()

    override suspend fun defaultFolder(): CollectionFolder = folder

    override suspend fun items(folderId: Long, forceRefresh: Boolean): List<CollectionItem> {
        if (forceRefresh) refreshCount++
        return items
    }

    override suspend fun instancesOf(releaseId: Long): List<CollectionInstance> =
        existing.filter { it.releaseId == releaseId }

    override suspend fun albumYear(masterId: Long): Int? {
        albumYearRequests += masterId
        return albumYears[masterId]
    }

    override suspend fun addRelease(folderId: Long, releaseId: Long): CollectionInstance {
        added += folderId to releaseId
        return CollectionInstance(99L, folderId, releaseId)
    }

    override suspend fun removeInstance(instance: CollectionInstance) {
        removed += instance
    }
}

class FakeCoverRecognitionService(
    var identity: AlbumIdentity? = null,
) : CoverRecognitionService {
    override suspend fun identify(jpeg: ByteArray): AlbumIdentity =
        identity ?: throw AppError.CoverNotRecognised()
}

fun collectionItem(
    id: Long,
    artist: String,
    title: String,
    year: Int? = 1990,
    position: Int = 0,
    folderId: Long = 1L,
    masterId: Long? = id,
    albumYear: Int? = null,
    genres: List<String> = emptyList(),
    styles: List<String> = emptyList(),
) = CollectionItem(
    instanceId = id,
    releaseId = id,
    folderId = folderId,
    masterId = masterId,
    artist = artist,
    title = title,
    editionYear = year,
    albumYear = albumYear,
    genres = genres,
    styles = styles,
    coverUrl = null,
    labels = emptyList(),
    catalogNumber = null,
    formats = emptyList(),
    position = position,
)
