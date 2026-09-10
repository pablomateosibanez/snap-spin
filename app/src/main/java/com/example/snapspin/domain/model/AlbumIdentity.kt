package com.example.snapspin.domain.model

/**
 * Lo que el servicio de visión ha sido capaz de deducir de una portada.
 *
 * Es deliberadamente un texto suelto y no un par grupo/título: Cloud Vision no devuelve campos
 * estructurados, y separarlos a base de heurísticas era poco fiable. La búsqueda de texto libre
 * de Discogs hace ese trabajo mucho mejor.
 */
data class AlbumIdentity(val query: String)
