package com.example.snapspin.data

import com.example.snapspin.data.vision.CoverIdentityParser
import com.example.snapspin.data.vision.dto.AnnotateImageResponseDto
import com.example.snapspin.data.vision.dto.BestGuessLabelDto
import com.example.snapspin.data.vision.dto.WebDetectionDto
import com.example.snapspin.data.vision.dto.WebEntityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverIdentityParserTest {

    @Test
    fun `usa la conjetura de Google como consulta de texto libre`() {
        val identity = CoverIdentityParser.parse(
            response(bestGuess = "the dark side of the moon pink floyd")
        )

        assertEquals("the dark side of the moon pink floyd", identity?.query)
    }

    @Test
    fun `limpia los espacios sobrantes de la conjetura`() {
        val identity = CoverIdentityParser.parse(response(bestGuess = "  nevermind   nirvana \n"))

        assertEquals("nevermind nirvana", identity?.query)
    }

    @Test
    fun `sin conjetura combina las entidades reconocidas mas puntuadas`() {
        val identity = CoverIdentityParser.parse(
            response(
                entities = listOf(
                    WebEntityDto(description = "Nirvana", score = 0.9f),
                    WebEntityDto(description = "Nevermind", score = 0.8f),
                    WebEntityDto(description = "Kurt Cobain", score = 0.4f),
                )
            )
        )

        assertEquals("Nirvana Nevermind Kurt Cobain", identity?.query)
    }

    @Test
    fun `descarta las entidades genericas que no acotan la busqueda`() {
        val identity = CoverIdentityParser.parse(
            response(
                entities = listOf(
                    WebEntityDto(description = "Album", score = 1.5f),
                    WebEntityDto(description = "Vinyl", score = 1.4f),
                    WebEntityDto(description = "Rumours", score = 0.7f),
                    WebEntityDto(description = "Fleetwood Mac", score = 0.6f),
                )
            )
        )

        assertEquals("Rumours Fleetwood Mac", identity?.query)
    }

    @Test
    fun `la conjetura tiene prioridad sobre las entidades`() {
        val identity = CoverIdentityParser.parse(
            response(
                bestGuess = "rumours fleetwood mac",
                entities = listOf(WebEntityDto(description = "Stevie Nicks", score = 0.9f)),
            )
        )

        assertEquals("rumours fleetwood mac", identity?.query)
    }

    @Test
    fun `sin ninguna senal util no devuelve identidad`() {
        assertNull(CoverIdentityParser.parse(AnnotateImageResponseDto()))
        assertNull(CoverIdentityParser.parse(response()))
        assertNull(
            CoverIdentityParser.parse(
                response(entities = listOf(WebEntityDto(description = "Album", score = 1f)))
            )
        )
    }

    private fun response(
        bestGuess: String? = null,
        entities: List<WebEntityDto> = emptyList(),
    ) = AnnotateImageResponseDto(
        webDetection = WebDetectionDto(
            webEntities = entities,
            bestGuessLabels = bestGuess?.let { listOf(BestGuessLabelDto(it)) } ?: emptyList(),
        )
    )
}
