package com.example.snapspin.data

import com.example.snapspin.data.gemini.CoverAnswerParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverAnswerParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private fun parse(raw: String?) = CoverAnswerParser.parse(raw, json)

    @Test
    fun `compone la consulta con grupo y titulo`() {
        val identity = parse(
            """{"is_album_cover": true, "artist": "Fleetwood Mac", "album": "Rumours"}"""
        )

        assertEquals("Fleetwood Mac Rumours", identity?.query)
    }

    @Test
    fun `acepta la respuesta envuelta en un bloque markdown`() {
        val identity = parse(
            """
            ```json
            {"is_album_cover": true, "artist": "Nirvana", "album": "Nevermind"}
            ```
            """.trimIndent()
        )

        assertEquals("Nirvana Nevermind", identity?.query)
    }

    @Test
    fun `si el modelo dice que no es una portada no devuelve nada`() {
        assertNull(parse("""{"is_album_cover": false, "artist": "", "album": ""}"""))
    }

    @Test
    fun `con solo el titulo sigue valiendo para buscar`() {
        val identity = parse("""{"is_album_cover": true, "artist": "", "album": "Kid A"}""")

        assertEquals("Kid A", identity?.query)
    }

    @Test
    fun `descarta una respuesta afirmativa pero vacia`() {
        assertNull(parse("""{"is_album_cover": true, "artist": "  ", "album": ""}"""))
    }

    @Test
    fun `descarta lo que no sea el JSON pedido`() {
        assertNull(parse("No puedo identificar esta portada."))
        assertNull(parse(""))
        assertNull(parse(null))
    }

    @Test
    fun `normaliza los espacios sobrantes del modelo`() {
        val identity = parse(
            """{"is_album_cover": true, "artist": " The  Breeders ", "album": "Last Splash "}"""
        )

        assertEquals("The Breeders Last Splash", identity?.query)
    }
}
