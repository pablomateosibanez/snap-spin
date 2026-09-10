package com.example.snapspin.domain.model

/** Carpeta ("colección") del usuario en Discogs. */
data class CollectionFolder(
    val id: Long,
    val name: String,
    val itemCount: Int,
)

/** Ejemplar concreto de un disco dentro de una carpeta de la colección. */
data class CollectionInstance(
    val instanceId: Long,
    val folderId: Long,
    val releaseId: Long,
)

/** Disco ya registrado en la colección, tal y como se muestra en el listado. */
data class CollectionItem(
    val instanceId: Long,
    val releaseId: Long,
    val folderId: Long,
    /** Obra a la que pertenece la edición; de ella sale el año de salida del álbum. */
    val masterId: Long?,
    val artist: String,
    val title: String,
    /** Año de la edición concreta que está registrada (puede ser una reedición). */
    val editionYear: Int?,
    /** Año en que salió el álbum. `null` mientras no se ha resuelto. */
    val albumYear: Int?,
    val genres: List<String>,
    val styles: List<String>,
    val coverUrl: String?,
    val labels: List<String>,
    val catalogNumber: String?,
    val formats: List<String>,
    /** Posición en el orden de alta (0 = el último registrado). */
    val position: Int,
) {
    /**
     * Año que se muestra: el de salida del álbum y, mientras no se conozca, el de la edición
     * (que en la mayoría de discos coincide).
     */
    val displayYear: Int? get() = albumYear ?: editionYear
}

/** Criterios de ordenación del listado de la colección. */
enum class CollectionOrder {
    /** Orden de entrada, del más reciente al más antiguo. */
    RECENT_FIRST,

    /** Orden de entrada, del más antiguo al más reciente. */
    OLDEST_FIRST,

    /** Alfabético por grupo (y, dentro del grupo, por título). */
    ARTIST,

    /** Alfabético por título del disco. */
    TITLE,
}
