package com.example.snapspin.domain.repository

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.CollectionFolder
import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.CollectionItem

/**
 * Colección del usuario. Abstrae por completo al proveedor (Discogs).
 *
 * Todas las funciones lanzan [AppError] ante cualquier fallo.
 */
interface CollectionRepository {

    /**
     * Carpeta donde se dan de alta los discos. Como la cuenta sólo tiene una colección,
     * la implementación la resuelve sola y el usuario nunca tiene que elegirla.
     */
    suspend fun defaultFolder(): CollectionFolder

    /**
     * Todos los discos de la carpeta, en orden de alta (el más reciente primero).
     *
     * @param forceRefresh ignora la copia cacheada y vuelve a pedirlos al servicio.
     */
    suspend fun items(folderId: Long, forceRefresh: Boolean = false): List<CollectionItem>

    /** Ejemplares de [releaseId] que ya están en la colección (vacío si no está registrado). */
    suspend fun instancesOf(releaseId: Long): List<CollectionInstance>

    /**
     * Año en que salió el álbum al que pertenece [masterId], o `null` si no se puede averiguar.
     * Cuesta una petición por obra, así que la implementación lo cachea.
     */
    suspend fun albumYear(masterId: Long): Int?

    /** Da de alta la edición en la carpeta indicada. */
    suspend fun addRelease(folderId: Long, releaseId: Long): CollectionInstance

    /** Elimina un ejemplar concreto de la colección. */
    suspend fun removeInstance(instance: CollectionInstance)
}
