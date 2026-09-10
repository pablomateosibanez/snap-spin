package com.example.snapspin.domain.model

/**
 * Edición devuelta por una búsqueda en el catálogo.
 *
 * Es información ligera: sirve para ordenar y descartar candidatas antes de pedir el detalle
 * (que es lo que cuesta peticiones contra la API).
 */
data class ReleaseCandidate(
    val id: Long,
    /** Obra a la que pertenece la edición; agrupa todos los prensajes de un mismo disco. */
    val masterId: Long?,
    val displayTitle: String,
    val year: Int?,
    val country: String?,
    val formats: List<String>,
    val thumbnailUrl: String?,
    val communityHave: Int,
    val communityWant: Int,
)
