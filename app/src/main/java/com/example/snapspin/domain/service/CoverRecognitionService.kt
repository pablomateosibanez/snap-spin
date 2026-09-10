package com.example.snapspin.domain.service

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.AlbumIdentity

/**
 * Reconocimiento de portadas.
 *
 * La implementación actual usa Google Cloud Vision, pero cualquier otro motor (ML Kit,
 * un servicio propio, un stub en tests) puede ocupar su lugar sin tocar el dominio.
 */
interface CoverRecognitionService {

    /**
     * Identifica el disco a partir de una imagen JPEG de su portada.
     *
     * @throws AppError.CoverNotRecognised si no se obtiene ninguna pista utilizable.
     */
    suspend fun identify(jpeg: ByteArray): AlbumIdentity
}
