package com.example.snapspin.data.gemini

import com.example.snapspin.data.network.ApiLog
import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.AlbumIdentity
import com.example.snapspin.domain.service.CoverRecognitionService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.RequestTimeoutException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Reconocimiento de portadas con Gemini.
 *
 * Sustituye al reconocimiento por detección web: en vez de depender de que la imagen exacta esté
 * indexada en internet, el modelo *mira* la carátula, así que también acierta con portadas sin
 * texto legible, tipografías raras o fotos tomadas de lado.
 *
 * Devuelve un texto ("grupo título") porque quien decide qué disco es sigue siendo la búsqueda
 * de Discogs, con su relevancia.
 */
class GeminiCoverRecognitionService(
    private val config: GeminiConfig,
    private val json: Json,
    /** Sólo para pruebas: sustituye la llamada real al SDK. */
    private val generateContent: (suspend (ByteArray) -> String?)? = null,
) : CoverRecognitionService {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = config.model,
            apiKey = config.apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0f
            },
            requestOptions = RequestOptions(config.timeoutMillis),
        )
    }

    override suspend fun identify(jpeg: ByteArray): AlbumIdentity {
        if (jpeg.isEmpty()) throw AppError.CoverNotRecognised()
        if (!config.isConfigured) throw AppError.Unauthorized(SERVICE)

        ApiLog.request(SERVICE, "generateContent", "${config.model} (${jpeg.size / 1024} KB)")

        val startedAt = System.nanoTime()
        val answer = callWithRetries(jpeg)
        ApiLog.response(SERVICE, 200, config.model, (System.nanoTime() - startedAt) / 1_000_000)

        return CoverAnswerParser.parse(answer, json) ?: run {
            ApiLog.failure(SERVICE, "respuesta no interpretable: ${answer?.take(MAX_LOGGED)}")
            throw AppError.CoverNotRecognised()
        }
    }

    /**
     * El nivel gratuito devuelve 503 ("high demand") con cierta frecuencia, y es un fallo
     * pasajero: reintentar con esperas crecientes lo resuelve casi siempre sin que el usuario
     * llegue a enterarse, porque ya está mirando el indicador de progreso.
     *
     * No se reintentan los errores de cuota ni de credenciales: ahí insistir sólo gasta peticiones.
     */
    private suspend fun callWithRetries(jpeg: ByteArray): String? {
        var attempt = 0
        while (true) {
            try {
                return callModel(jpeg)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val appError = error.toAppError()
                val retryable = appError is AppError.ServiceBusy || appError is AppError.Network
                if (!retryable || attempt >= MAX_RETRIES) throw appError

                attempt++
                val wait = RETRY_DELAYS_MILLIS[attempt - 1]
                ApiLog.failure(
                    SERVICE,
                    "fallo transitorio, reintento $attempt/$MAX_RETRIES en $wait ms",
                )
                delay(wait)
            }
        }
    }

    private suspend fun callModel(jpeg: ByteArray): String? =
        generateContent?.invoke(jpeg)
            ?: model.generateContent(
                content {
                    text(PROMPT)
                    blob("image/jpeg", jpeg)
                }
            ).text

    /**
     * Traduce las excepciones del SDK a errores del dominio.
     *
     * Sólo se reinterpretan las que tienen un significado claro para el usuario; el resto
     * conserva el mensaje literal del servicio. El SDK no distingue por subclase entre "cuota
     * agotada", "sin saldo de prepago" o "modelo retirado" —todas llegan como fallo genérico—
     * y son justo las que exigen acciones distintas, así que tragarse el texto dejaría al
     * usuario sin saber qué hacer.
     */
    internal fun Throwable.toAppError(): AppError {
        ApiLog.failure(SERVICE, "fallo de generateContent con ${config.model}", this)
        return when (this) {
            is InvalidAPIKeyException -> AppError.Unauthorized(SERVICE)
            // El modelo se ha negado a responder: para el usuario es una portada no reconocida.
            is PromptBlockedException, is ResponseStoppedException -> AppError.CoverNotRecognised()
            is RequestTimeoutException, is IOException -> AppError.Network(this)
            else -> when (val status = serviceStatus()) {
                in TRANSIENT_STATUSES -> AppError.ServiceBusy(SERVICE)
                else -> AppError.Remote(SERVICE, status, serviceMessage())
            }
        }
    }

    /** El SDK incrusta el JSON de error del servicio en el mensaje; de ahí sale el código. */
    private fun Throwable.serviceStatus(): Int? = rawMessage()
        ?.let { STATUS_CODE.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

    /**
     * El SDK 0.9.0 no sabe parsear un error de Google que no traiga el campo `details` y lanza un
     * `MissingFieldException` envolviendo el cuerpo original. Por eso se recorre toda la cadena
     * de causas: el JSON con el motivo real puede estar en cualquiera de ellas.
     */
    private fun Throwable.rawMessage(): String? = generateSequence(this) { it.cause }
        .take(MAX_CAUSE_DEPTH)
        .mapNotNull { it.message?.trim()?.takeIf(String::isNotBlank) }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }

    /**
     * El SDK entrega el cuerpo de error del servicio dentro del mensaje de la excepción. Se
     * extrae sólo el campo `message` para no enseñarle JSON en bruto al usuario; si no viene en
     * ese formato, se usa el texto tal cual.
     */
    private fun Throwable.serviceMessage(): String? {
        val raw = rawMessage() ?: return null
        val extracted = SERVICE_MESSAGE.find(raw)?.groupValues?.getOrNull(1)?.trim()
        return (extracted?.takeIf { it.isNotBlank() } ?: raw)
            .replace(WHITESPACE, " ")
            .take(MAX_DETAIL)
    }

    private companion object {
        const val SERVICE = "Gemini"
        const val MAX_DETAIL = 400
        const val MAX_LOGGED = 300
        const val MAX_CAUSE_DEPTH = 5
        const val MAX_RETRIES = 3

        /** 800 ms, 2 s y 4 s: cubre un pico de demanda sin dejar la pantalla colgada. */
        val RETRY_DELAYS_MILLIS = longArrayOf(800L, 2_000L, 4_000L)

        val TRANSIENT_STATUSES = listOf(500, 502, 503, 504)

        val STATUS_CODE = """"code"\s*:\s*(\d{3})""".toRegex()
        val SERVICE_MESSAGE = """"message"\s*:\s*"([^"]*)"""".toRegex()
        val WHITESPACE = """\s+""".toRegex()

        val PROMPT = """
            Identifica el disco a partir de esta fotografía de su portada.
            Responde únicamente con un objeto JSON con esta forma exacta:
            {"is_album_cover": true, "artist": "", "album": ""}
            - artist: nombre del grupo o artista tal y como figura en Discogs.
            - album: título del álbum, sin el formato ni la edición.
            - Si la imagen no es la portada de un disco, o no lo reconoces con seguridad,
              responde {"is_album_cover": false, "artist": "", "album": ""}.
        """.trimIndent()
    }
}
