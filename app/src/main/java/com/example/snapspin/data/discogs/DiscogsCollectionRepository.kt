package com.example.snapspin.data.discogs

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.CollectionFolder
import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.repository.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Implementación de la colección del usuario sobre la API de Discogs. */
class DiscogsCollectionRepository(
    private val api: DiscogsApi,
    private val masterYears: MasterYearStore,
) : CollectionRepository {

    private val folderMutex = Mutex()
    private var cachedFolder: CollectionFolder? = null

    private val itemsMutex = Mutex()
    private var cachedItems: List<CollectionItem>? = null

    /**
     * Discogs expone siempre una carpeta virtual con id 0 ("All") que no admite altas. La cuenta
     * de destino tiene una única colección real, así que se toma la primera carpeta con id > 0
     * y se cachea para no repetir la petición en cada registro.
     */
    override suspend fun defaultFolder(): CollectionFolder = folderMutex.withLock {
        cachedFolder?.let { return@withLock it }
        val folder = api.collectionFolders().folders
            .map { it.toDomain() }
            .firstOrNull { it.id != ALL_FOLDER_ID }
            ?: throw AppError.NotFound("ninguna colección en tu cuenta de Discogs")
        cachedFolder = folder
        folder
    }

    /**
     * Descarga la colección entera paginando y la guarda en memoria: buscar y reordenar después
     * no cuesta ninguna petición más. La caché se invalida sola al añadir o quitar discos.
     */
    override suspend fun items(folderId: Long, forceRefresh: Boolean): List<CollectionItem> =
        itemsMutex.withLock {
            if (!forceRefresh) cachedItems?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                val items = mutableListOf<CollectionItem>()
                var page = 1
                var totalPages: Int
                do {
                    val response = api.collectionReleases(folderId, page)
                    response.releases.forEach { dto ->
                        // El año del álbum se rellena de la caché si ya se conoce; el resto se
                        // resuelve después en segundo plano.
                        val cachedYear = dto.basicInformation?.masterId
                            ?.takeIf { it > 0 }
                            ?.let(masterYears::year)
                        items += dto.toCollectionItem(position = items.size, albumYear = cachedYear)
                    }
                    totalPages = response.pagination.pages
                    page++
                } while (page <= totalPages && page <= MAX_PAGES)

                items.toList().also { cachedItems = it }
            }
        }

    override suspend fun albumYear(masterId: Long): Int? {
        masterYears.year(masterId)?.let { return it }
        val year = api.master(masterId).year?.takeIf { it > 0 } ?: return null
        masterYears.save(masterId, year)
        // La lista cacheada en memoria se actualiza para que un refresco no pierda lo resuelto.
        itemsMutex.withLock {
            cachedItems = cachedItems?.map {
                if (it.masterId == masterId) it.copy(albumYear = year) else it
            }
        }
        return year
    }

    override suspend fun instancesOf(releaseId: Long): List<CollectionInstance> =
        api.collectionItemsByRelease(releaseId).releases
            .map { it.toDomain() }
            .filter { it.folderId != ALL_FOLDER_ID }

    override suspend fun addRelease(folderId: Long, releaseId: Long): CollectionInstance {
        val response = api.addToCollection(folderId, releaseId)
        invalidateItems()
        return CollectionInstance(
            instanceId = response.instanceId,
            folderId = folderId,
            releaseId = releaseId,
        )
    }

    override suspend fun removeInstance(instance: CollectionInstance) {
        api.removeFromCollection(instance.folderId, instance.releaseId, instance.instanceId)
        // Se descuenta de la caché en lugar de invalidarla: el listado no necesita volver a
        // descargarse entero por borrar un disco.
        itemsMutex.withLock {
            cachedItems = cachedItems?.filterNot { it.instanceId == instance.instanceId }
        }
    }

    private suspend fun invalidateItems() = itemsMutex.withLock { cachedItems = null }

    private companion object {
        const val ALL_FOLDER_ID = 0L

        /** Tope de seguridad: 100 discos por página. */
        const val MAX_PAGES = 25
    }
}
