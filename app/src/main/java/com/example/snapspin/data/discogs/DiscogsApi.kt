package com.example.snapspin.data.discogs

import com.example.snapspin.data.discogs.dto.AddToCollectionResponseDto
import com.example.snapspin.data.discogs.dto.CollectionItemsResponseDto
import com.example.snapspin.data.discogs.dto.FoldersResponseDto
import com.example.snapspin.data.discogs.dto.MasterDto
import com.example.snapspin.data.discogs.dto.ReleaseDto
import com.example.snapspin.data.discogs.dto.SearchResponseDto
import com.example.snapspin.data.network.fetchText
import com.example.snapspin.domain.error.AppError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Acceso HTTP crudo a la API de Discogs. Devuelve DTOs; no conoce el dominio.
 *
 * Es deliberadamente una clase concreta y no una interfaz: la abstracción que ve el dominio son
 * los repositorios ([DiscogsCatalogRepository], [DiscogsCollectionRepository]).
 */
class DiscogsApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val config: DiscogsConfig,
) {

    suspend fun search(parameters: Map<String, String>): SearchResponseDto {
        val url = url("database", "search") {
            parameters.forEach { (name, value) -> addQueryParameter(name, value) }
            addQueryParameter("type", "release")
            addQueryParameter("per_page", MAX_SEARCH_RESULTS.toString())
        }
        return get(url)
    }

    suspend fun release(releaseId: Long): ReleaseDto {
        val url = url("releases", releaseId.toString()) {
            addQueryParameter("curr_abbr", config.currency)
        }
        return get(url)
    }

    /** Obra: es la única fuente del año en que salió el álbum. */
    suspend fun master(masterId: Long): MasterDto = get(url("masters", masterId.toString()))

    suspend fun collectionFolders(): FoldersResponseDto =
        get(url("users", config.username, "collection", "folders"))

    /** Ejemplares de una edición en la colección; Discogs responde 404 si no hay ninguno. */
    suspend fun collectionItemsByRelease(releaseId: Long): CollectionItemsResponseDto {
        val url = url("users", config.username, "collection", "releases", releaseId.toString())
        val body = client.fetchText(Request.Builder().url(url).build(), SERVICE, notFoundAsNull = true)
            ?: return CollectionItemsResponseDto()
        return decode(body)
    }

    /**
     * Una página de la colección, ordenada por fecha de alta descendente: así el orden que
     * devuelve el servicio ya es el "orden de entrada" que se muestra por defecto.
     */
    suspend fun collectionReleases(folderId: Long, page: Int): CollectionItemsResponseDto {
        val url = url(
            "users", config.username, "collection", "folders",
            folderId.toString(), "releases",
        ) {
            addQueryParameter("sort", "added")
            addQueryParameter("sort_order", "desc")
            addQueryParameter("per_page", PAGE_SIZE.toString())
            addQueryParameter("page", page.toString())
        }
        return get(url)
    }

    suspend fun addToCollection(folderId: Long, releaseId: Long): AddToCollectionResponseDto {
        val url = url(
            "users", config.username, "collection", "folders",
            folderId.toString(), "releases", releaseId.toString(),
        )
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return decode(client.fetchText(request, SERVICE).orEmpty())
    }

    suspend fun removeFromCollection(folderId: Long, releaseId: Long, instanceId: Long) {
        val url = url(
            "users", config.username, "collection", "folders", folderId.toString(),
            "releases", releaseId.toString(), "instances", instanceId.toString(),
        )
        client.fetchText(Request.Builder().url(url).delete().build(), SERVICE)
    }

    private suspend inline fun <reified T> get(url: HttpUrl): T =
        decode(client.fetchText(Request.Builder().url(url).build(), SERVICE).orEmpty())

    private inline fun <reified T> decode(body: String): T = try {
        json.decodeFromString(body)
    } catch (e: SerializationException) {
        throw AppError.Parsing(SERVICE, e)
    }

    private fun url(vararg segments: String, block: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        config.baseUrl.toHttpUrl().newBuilder()
            .apply { segments.forEach { addPathSegment(it) } }
            .apply(block)
            .build()

    companion object {
        const val SERVICE = "Discogs"
        private const val MAX_SEARCH_RESULTS = 25
        private const val PAGE_SIZE = 100
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
