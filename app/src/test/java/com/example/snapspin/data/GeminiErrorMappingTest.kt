package com.example.snapspin.data

import com.example.snapspin.data.gemini.GeminiConfig
import com.example.snapspin.data.gemini.GeminiCoverRecognitionService
import com.example.snapspin.domain.error.AppError
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import java.io.IOException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El motivo real de un fallo de la API tiene que llegar hasta la pantalla: "sin saldo",
 * "demasiadas peticiones" y "clave inválida" se arreglan de formas distintas.
 */
class GeminiErrorMappingTest {

    private val service = GeminiCoverRecognitionService(
        config = GeminiConfig(apiKey = "clave", model = "gemini-flash-latest"),
        json = Json { ignoreUnknownKeys = true },
    )

    private fun map(error: Throwable): AppError = with(service) { error.toAppError() }

    private val creditsDepleted = """
        {"error": {"code": 429, "message": "Your prepayment credits are depleted. Please go to
        AI Studio at https://ai.studio/projects to manage your project and billing.",
        "status": "RESOURCE_EXHAUSTED"}}
    """.trimIndent()

    @Test
    fun `el mensaje de saldo agotado llega entero al usuario`() {
        val error = map(RuntimeException(creditsDepleted))

        assertTrue(error is AppError.Remote)
        assertEquals(429, (error as AppError.Remote).status)
        assertTrue(error.message.contains("prepayment credits are depleted"))
        assertTrue(error.message.contains("ai.studio"))
        // Al usuario se le enseña el motivo, no el JSON en bruto del servicio.
        assertFalse(error.message.contains("{"))
        assertFalse(error.message.contains("RESOURCE_EXHAUSTED"))
        println("En pantalla: " + error.message)
    }

    @Test
    fun `un fallo sin codigo reconocible conserva igualmente el texto del servicio`() {
        val error = map(IllegalStateException("The model gemini-2.5-flash is no longer available"))

        assertTrue(error is AppError.Remote)
        assertEquals(null, (error as AppError.Remote).status)
        assertTrue(error.message.contains("no longer available"))
    }

    @Test
    fun `una clave invalida se identifica como problema de credenciales`() {
        val error = map(InvalidAPIKeyException("API key not valid", null))

        assertTrue(error is AppError.Unauthorized)
        assertTrue(error.message.contains("Gemini"))
    }

    @Test
    fun `un fallo de red se sigue tratando como falta de conexion`() {
        assertTrue(map(IOException("timeout")) is AppError.Network)
    }

    @Test
    fun `si el servicio no da ningun mensaje el error sigue siendo legible`() {
        val error = map(RuntimeException())

        assertTrue(error is AppError.Remote)
        assertTrue(error.message.startsWith("Gemini ha devuelto un error"))
    }
}
