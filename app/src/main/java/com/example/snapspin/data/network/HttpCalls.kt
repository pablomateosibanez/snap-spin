package com.example.snapspin.data.network

import com.example.snapspin.domain.error.AppError
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Adapta el `enqueue` de OkHttp a corrutinas, cancelando la llamada si se cancela el scope. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = continuation.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
    })
}

/**
 * Ejecuta la petición y devuelve el cuerpo como texto, traduciendo cualquier fallo de transporte
 * o código HTTP de error a un [AppError]. Es el único punto donde la capa de datos conoce OkHttp,
 * y también donde se deja traza en Logcat de todo lo que se habla con las APIs.
 *
 * @param notFoundAsNull si es `true`, un 404 devuelve `null` en vez de lanzar.
 */
suspend fun OkHttpClient.fetchText(
    request: Request,
    service: String,
    notFoundAsNull: Boolean = false,
): String? = withContext(Dispatchers.IO) {
    val url = request.url.toString()
    ApiLog.request(service, request.method, url)

    val startedAt = System.nanoTime()
    val response = try {
        newCall(request).await()
    } catch (io: IOException) {
        ApiLog.failure(service, "sin respuesta de $url", io)
        throw AppError.Network(io)
    }
    val millis = (System.nanoTime() - startedAt) / 1_000_000

    response.use {
        val body = runCatching { it.body.string() }.getOrDefault("")

        if (it.isSuccessful) {
            ApiLog.response(service, it.code, url, millis)
            return@withContext body
        }
        if (it.code == 404 && notFoundAsNull) {
            ApiLog.response(service, it.code, url, millis)
            return@withContext null
        }

        // El cuerpo de un error trae siempre la explicación real del servicio: se registra entero
        // aunque luego en pantalla se muestre resumido.
        ApiLog.httpError(service, it.code, url, body)

        when (it.code) {
            401 -> throw AppError.Unauthorized(service)
            429 -> throw AppError.RateLimited(service)
            // Un 403 no siempre es un token malo: Google lo usa también para "falta activar la
            // facturación" o "la API no está habilitada". Si el servidor explica el motivo, se
            // muestra tal cual en vez de acusar a las credenciales.
            403 -> throw body.errorDetail()
                ?.let { detail -> AppError.Remote(service, 403, detail) }
                ?: AppError.Unauthorized(service)
            else -> throw AppError.Remote(service, it.code, body.errorDetail())
        }
    }
}

/** Extrae el `message` de un cuerpo de error JSON sin necesidad de un DTO por servicio. */
private fun String.errorDetail(): String? = MESSAGE_REGEX.find(this)
    ?.groupValues?.getOrNull(1)
    ?.takeIf { it.isNotBlank() }
    ?.take(MAX_DETAIL_LENGTH)

private const val MAX_DETAIL_LENGTH = 220

private val MESSAGE_REGEX = """"(?:message|error_description|error)"\s*:\s*"([^"]*)"""".toRegex()
