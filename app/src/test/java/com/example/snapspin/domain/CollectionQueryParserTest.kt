package com.example.snapspin.domain

import com.example.snapspin.domain.usecase.CollectionQueryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El rango de años se escribe dentro del propio buscador, como lo escribiría uno de corrido. */
class CollectionQueryParserTest {

    private fun parse(query: String) = CollectionQueryParser.parse(query)

    @Test
    fun `reconoce un rango con guion`() {
        val parsed = parse("1960-1980")

        assertEquals(1960..1980, parsed.yearRange)
        assertEquals("", parsed.text)
    }

    @Test
    fun `reconoce el rango escrito en castellano`() {
        assertEquals(1960..1980, parse("1960 a 1980").yearRange)
        assertEquals(1960..1980, parse("desde 1960 hasta 1980").yearRange)
        assertEquals(1960..1980, parse("1960 .. 1980").yearRange)
    }

    @Test
    fun `combina texto y rango en la misma consulta`() {
        val parsed = parse("rock 1960-1980")

        assertEquals(1960..1980, parsed.yearRange)
        assertEquals("rock", parsed.text)
    }

    @Test
    fun `admite rangos abiertos por cualquiera de los dos lados`() {
        assertEquals(1970, parse("desde 1970").yearRange?.first)
        assertEquals(1970, parse("1970-").yearRange?.first)
        assertEquals(1980, parse("hasta 1980").yearRange?.last)
        assertEquals(1980, parse("-1980").yearRange?.last)
    }

    @Test
    fun `ordena el rango aunque se escriba al reves`() {
        assertEquals(1960..1980, parse("1980-1960").yearRange)
    }

    @Test
    fun `un ano suelto sigue siendo una busqueda de texto`() {
        val parsed = parse("1974")

        assertNull(parsed.yearRange)
        assertEquals("1974", parsed.text)
    }

    @Test
    fun `una busqueda normal no se ve afectada`() {
        val parsed = parse("fleetwood mac")

        assertNull(parsed.yearRange)
        assertEquals("fleetwood mac", parsed.text)
    }
}
