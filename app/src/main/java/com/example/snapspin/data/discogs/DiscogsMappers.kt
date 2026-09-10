package com.example.snapspin.data.discogs

import com.example.snapspin.data.discogs.dto.CollectionItemDto
import com.example.snapspin.data.discogs.dto.FolderDto
import com.example.snapspin.data.discogs.dto.ReleaseDto
import com.example.snapspin.data.discogs.dto.SearchResultDto
import com.example.snapspin.domain.model.CollectionFolder
import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.ReleaseCandidate
import com.example.snapspin.domain.model.ReleaseDetails

/** Discogs desambigua artistas homónimos con un sufijo numérico ("Nirvana (2)"). */
private val ARTIST_DISAMBIGUATION = """\s*\(\d+\)\s*$""".toRegex()

internal fun String.cleanArtistName(): String = replace(ARTIST_DISAMBIGUATION, "").trim()

internal fun SearchResultDto.toDomain() = ReleaseCandidate(
    id = id,
    masterId = masterId?.takeIf { it > 0 },
    displayTitle = title.trim(),
    year = year?.trim()?.take(4)?.toIntOrNull(),
    country = country?.trim()?.ifBlank { null },
    formats = format.filter { it.isNotBlank() },
    thumbnailUrl = coverImage?.ifBlank { null } ?: thumb?.ifBlank { null },
    communityHave = community?.have ?: 0,
    communityWant = community?.want ?: 0,
)

internal fun ReleaseDto.toDomain(currency: String) = ReleaseDetails(
    id = id,
    artist = artists
        .map { it.name.cleanArtistName() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { UNKNOWN_ARTIST },
    title = title.trim().ifBlank { UNKNOWN_TITLE },
    year = year?.takeIf { it > 0 },
    country = country?.trim()?.ifBlank { null },
    formats = formats
        .flatMap { listOf(it.name) + it.descriptions }
        .filter { it.isNotBlank() }
        .distinct(),
    labels = labels.map { it.name.cleanArtistName() }.filter { it.isNotBlank() }.distinct(),
    catalogNumber = labels.firstNotNullOfOrNull { it.catno?.trim()?.ifBlank { null } },
    coverImageUrl = images.firstOrNull { it.type == "primary" }?.uri
        ?: images.firstNotNullOfOrNull { it.uri }
        ?: thumb?.ifBlank { null },
    lowestPrice = lowestPrice?.takeIf { it > 0.0 },
    currency = currency,
    copiesForSale = numForSale,
)

internal fun FolderDto.toDomain() = CollectionFolder(id = id, name = name, itemCount = count)

internal fun CollectionItemDto.toDomain() = CollectionInstance(
    instanceId = instanceId,
    folderId = folderId,
    releaseId = id,
)

internal fun CollectionItemDto.toCollectionItem(position: Int, albumYear: Int?) = CollectionItem(
    instanceId = instanceId,
    releaseId = id,
    folderId = folderId,
    masterId = basicInformation?.masterId?.takeIf { it > 0 },
    artist = basicInformation?.artists
        ?.map { it.name.cleanArtistName() }
        ?.filter { it.isNotBlank() }
        ?.joinToString(", ")
        ?.ifBlank { null }
        ?: UNKNOWN_ARTIST,
    title = basicInformation?.title?.trim()?.ifBlank { null } ?: UNKNOWN_TITLE,
    editionYear = basicInformation?.year?.takeIf { it > 0 },
    albumYear = albumYear,
    genres = basicInformation?.genres.orEmpty().filter { it.isNotBlank() },
    styles = basicInformation?.styles.orEmpty().filter { it.isNotBlank() },
    coverUrl = basicInformation?.coverImage?.ifBlank { null }
        ?: basicInformation?.thumb?.ifBlank { null },
    labels = basicInformation?.labels.orEmpty()
        .map { it.name.cleanArtistName() }
        .filter { it.isNotBlank() }
        .distinct(),
    catalogNumber = basicInformation?.labels.orEmpty()
        .firstNotNullOfOrNull { it.catno?.trim()?.ifBlank { null } },
    formats = basicInformation?.formats.orEmpty()
        .flatMap { listOf(it.name) + it.descriptions }
        .filter { it.isNotBlank() }
        .distinct(),
    position = position,
)

private const val UNKNOWN_ARTIST = "Artista desconocido"
private const val UNKNOWN_TITLE = "Título desconocido"
