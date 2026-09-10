package com.example.snapspin.data

import com.example.snapspin.data.gemini.GeminiConfig
import com.example.snapspin.data.gemini.GeminiCoverRecognitionService
import com.example.snapspin.domain.error.AppError
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El nivel gratuito de Gemini devuelve 503 con frecuencia cuando hay picos de demanda. Como es un
 * fallo pasajero, la app reintenta sola en vez de hacer que el usuario repita la foto.
 */
class GeminiRetryTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val answer = """{"is_album_cover": true, "artist": "The Breeders", "album": "Last Splash"}"""

    /** Error tal y como lo entrega el SDK 0.9.0: no sabe parsearlo y lo envuelve. */
    private fun highDemandError() = RuntimeException(
        "Unexpected Response:\n" +
            """{"error": {"code": 503, "message": "This model is currently experiencing high """ +
            """demand. Spikes in demand are usually temporary. Please try again later.", """ +
            """"status": "UNAVAILABLE"}}""",
        IllegalStateException(
            "kotlinx.serialization.MissingFieldException: Field 'details' is required for type " +
                "with serial name 'com.google.ai.client.generativeai.common.server.GRpcError'"
        ),
    )

    private fun service(responses: List<() -> String>): Pair<GeminiCoverRecognitionService, () -> Int> {
        var calls = 0
        val service = GeminiCoverRecognitionService(
            config = GeminiConfig(apiKey = "clave", model = "gemini-flash-latest"),
            json = json,
            generateContent = {
                val index = calls.coerceAtMost(responses.lastIndex)
                calls++
                responses[index]()
            },
        )
        return service to { calls }
    }

    @Test
    fun `un pico de demanda se resuelve reintentando, sin molestar al usuario`() = runTest {
        val (service, calls) = service(
            listOf({ throw highDemandError() }, { answer })
        )

        val identity = service.identify(ByteArray(4))

        assertEquals("The Breeders Last Splash", identity.query)
        assertEquals(2, calls())
    }

    @Test
    fun `si la saturacion no cede se avisa en castellano tras agotar los reintentos`() = runTest {
        val (service, calls) = service(listOf({ throw highDemandError() }))

        val error = runCatching { service.identify(ByteArray(4)) }.exceptionOrNull()

        assertTrue(error is AppError.ServiceBusy)
        assertTrue(error!!.message!!.contains("saturado"))
        assertEquals(4, calls()) // el intento inicial y tres reintentos
    }

    @Test
    fun `un corte de red tambien se reintenta`() = runTest {
        val (service, calls) = service(listOf({ throw IOException("timeout") }, { answer }))

        service.identify(ByteArray(4))

        assertEquals(2, calls())
    }

    @Test
    fun `no se insiste cuando el problema es de cuota o de saldo`() = runTest {
        val (service, calls) = service(
            listOf({
                throw RuntimeException(
                    """{"error": {"code": 429, "message": "Your prepayment credits are depleted.", "status": "RESOURCE_EXHAUSTED"}}"""
                )
            })
        )

        val error = runCatching { service.identify(ByteArray(4)) }.exceptionOrNull()

        assertTrue(error is AppError.Remote)
        assertEquals(429, (error as AppError.Remote).status)
        assertEquals(1, calls())
    }

    @Test
    fun `el motivo real se recupera aunque el SDK lo esconda en la causa`() = runTest {
        val (service, _) = service(
            listOf({
                throw RuntimeException(
                    "Something went wrong",
                    IllegalStateException(
                        """{"error": {"code": 400, "message": "API key not valid for this model", "status": "INVALID_ARGUMENT"}}"""
                    ),
                )
            })
        )

        val error = runCatching { service.identify(ByteArray(4)) }.exceptionOrNull()

        assertTrue(error is AppError.Remote)
        assertEquals(400, (error as AppError.Remote).status)
        assertTrue(error.message.contains("API key not valid for this model"))
    }
}
