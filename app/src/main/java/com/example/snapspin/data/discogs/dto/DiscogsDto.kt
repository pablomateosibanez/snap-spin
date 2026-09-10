package com.example.snapspin.data.discogs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val results: List<SearchResultDto> = emptyList(),
)

@Serializable
data class SearchResultDto(
    val id: Long,
    val title: String = "",
    /** Discogs devuelve el año como texto y a veces vacío. */
    val year: String? = null,
    val country: String? = null,
    val format: List<String> = emptyList(),
    val thumb: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    val community: CommunityDto? = null,
    @SerialName("master_id") val masterId: Long? = null,
)

@Serializable
data class CommunityDto(
    val want: Int = 0,
    val have: Int = 0,
)

@Serializable
data class ReleaseDto(
    val id: Long,
    val title: String = "",
    val year: Int? = null,
    val country: String? = null,
    val artists: List<ArtistDto> = emptyList(),
    val formats: List<FormatDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val images: List<ImageDto> = emptyList(),
    val thumb: String? = null,
    @SerialName("lowest_price") val lowestPrice: Double? = null,
    @SerialName("num_for_sale") val numForSale: Int = 0,
)

@Serializable
data class ArtistDto(val name: String = "")

@Serializable
data class FormatDto(
    val name: String = "",
    val descriptions: List<String> = emptyList(),
)

@Serializable
data class LabelDto(
    val name: String = "",
    val catno: String? = null,
)

@Serializable
data class ImageDto(
    val type: String? = null,
    val uri: String? = null,
    @SerialName("uri150") val thumbnailUri: String? = null,
)

@Serializable
data class FoldersResponseDto(
    val folders: List<FolderDto> = emptyList(),
)

@Serializable
data class FolderDto(
    val id: Long,
    val name: String = "",
    val count: Int = 0,
)

@Serializable
data class CollectionItemsResponseDto(
    val pagination: PaginationDto = PaginationDto(),
    val releases: List<CollectionItemDto> = emptyList(),
)

@Serializable
data class PaginationDto(
    val page: Int = 1,
    val pages: Int = 1,
    val items: Int = 0,
)

@Serializable
data class CollectionItemDto(
    val id: Long,
    @SerialName("instance_id") val instanceId: Long,
    @SerialName("folder_id") val folderId: Long,
    @SerialName("date_added") val dateAdded: String? = null,
    @SerialName("basic_information") val basicInformation: BasicInformationDto? = null,
)

/** Ficha reducida que Discogs incrusta en cada elemento de la colección. */
@Serializable
data class BasicInformationDto(
    val title: String = "",
    val year: Int? = null,
    val artists: List<ArtistDto> = emptyList(),
    @SerialName("cover_image") val coverImage: String? = null,
    val thumb: String? = null,
    @SerialName("master_id") val masterId: Long? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val formats: List<FormatDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
)

/** Obra (master): agrupa todas las ediciones y guarda el año en que salió el álbum. */
@Serializable
data class MasterDto(
    val id: Long = 0,
    val year: Int? = null,
)

@Serializable
data class AddToCollectionResponseDto(
    @SerialName("instance_id") val instanceId: Long,
)
