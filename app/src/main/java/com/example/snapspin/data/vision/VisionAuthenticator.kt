package com.example.snapspin.data.vision

/** Forma de autenticarse contra Cloud Vision. */
sealed interface VisionCredential {
    /** Clave simple de API, se envía como parámetro `key`. */
    data class ApiKey(val value: String) : VisionCredential

    /** Token OAuth2, se envía en la cabecera `Authorization`. */
    data class BearerToken(val value: String) : VisionCredential
}

/**
 * Proveedor de credenciales para Cloud Vision.
 *
 * Aísla al cliente HTTP de *cómo* se obtiene el permiso: una API key fija o un token OAuth2
 * firmado a partir de la cuenta de servicio.
 */
interface VisionAuthenticator {
    suspend fun credential(): VisionCredential
}

/** Autenticación por API key (la más simple, si el proyecto de Google la tiene habilitada). */
class ApiKeyVisionAuthenticator(private val apiKey: String) : VisionAuthenticator {
    override suspend fun credential(): VisionCredential = VisionCredential.ApiKey(apiKey)
}
