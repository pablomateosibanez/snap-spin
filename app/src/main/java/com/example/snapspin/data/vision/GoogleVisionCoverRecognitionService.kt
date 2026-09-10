package com.example.snapspin.data.vision

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.AlbumIdentity
import com.example.snapspin.domain.service.CoverRecognitionService

/**
 * Implementación del reconocimiento de portadas con Google Cloud Vision.
 *
 * Es la única clase de todo el proyecto que une "portada" y "Google": el resto de la app trabaja
 * contra [CoverRecognitionService].
 */
class GoogleVisionCoverRecognitionService(
    private val api: GoogleVisionApi,
) : CoverRecognitionService {

    override suspend fun identify(jpeg: ByteArray): AlbumIdentity {
        if (jpeg.isEmpty()) throw AppError.CoverNotRecognised()
        val annotation = api.annotate(jpeg)
        return CoverIdentityParser.parse(annotation) ?: throw AppError.CoverNotRecognised()
    }
}
