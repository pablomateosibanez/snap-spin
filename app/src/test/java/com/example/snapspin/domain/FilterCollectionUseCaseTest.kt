package com.example.snapspin.domain

import com.example.snapspin.domain.model.CollectionOrder
import com.example.snapspin.domain.usecase.FilterCollectionUseCase
import com.example.snapspin.fake.collectionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterCollectionUseCaseTest {

    private val filter = FilterCollectionUseCase()

    private val items = listOf(
        collectionItem(1, "Triángulo de Amor Bizarro", "Salve Discordia", 2019, position = 0,
            genres = listOf("Rock"), styles = listOf("Indie Rock")),
        collectionItem(2, "Pink Floyd", "The Dark Side Of The Moon", 1973, position = 1,
            genres = listOf("Rock"), styles = listOf("Prog Rock")),
        collectionItem(3, "Los Planetas", "Una Semana En El Motor", 1994, position = 2,
            genres = listOf("Rock"), styles = listOf("Indie Rock")),
        collectionItem(4, "Barry White", "Can't Get Enough", 2011, position = 3,
            albumYear = 1974, genres = listOf("Funk / Soul"), styles = listOf("Soul", "Disco")),
    )

    @Test
    fun `sin busqueda mantiene el orden de entrada, del mas reciente al mas antiguo`() {
        val result = filter(items, query = "", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(1L, 2L, 3L, 4L), result.map { it.releaseId })
    }

    @Test
    fun `el orden inverso muestra primero los registrados hace mas tiempo`() {
        val result = filter(items, query = "", order = CollectionOrder.OLDEST_FIRST)
        assertEquals(listOf(4L, 3L, 2L, 1L), result.map { it.releaseId })
    }

    @Test
    fun `ordena alfabeticamente por grupo`() {
        val result = filter(items, query = "", order = CollectionOrder.ARTIST)
        assertEquals(
            listOf("Barry White", "Los Planetas", "Pink Floyd", "Triángulo de Amor Bizarro"),
            result.map { it.artist },
        )
    }

    @Test
    fun `ordena alfabeticamente por titulo`() {
        val result = filter(items, query = "", order = CollectionOrder.TITLE)
        assertEquals(
            listOf("Can't Get Enough", "Salve Discordia", "The Dark Side Of The Moon", "Una Semana En El Motor"),
            result.map { it.title },
        )
    }

    @Test
    fun `busca por genero musical`() {
        val result = filter(items, query = "funk", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(4L), result.map { it.releaseId })
    }

    @Test
    fun `busca tambien por estilo`() {
        val result = filter(items, query = "prog rock", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(2L), result.map { it.releaseId })
    }

    @Test
    fun `busca por el ano de salida del album, no por el de la edicion`() {
        // El disco de Barry White está registrado en una reedición de 2011 del álbum de 1974.
        assertEquals(listOf(4L), filter(items, "1974", CollectionOrder.RECENT_FIRST).map { it.releaseId })
        assertEquals(emptyList<Long>(), filter(items, "2011", CollectionOrder.RECENT_FIRST).map { it.releaseId })
    }

    @Test
    fun `el ano mostrado es el del album cuando se conoce`() {
        val barryWhite = items.first { it.releaseId == 4L }
        assertEquals(1974, barryWhite.displayYear)
        assertEquals(2011, barryWhite.editionYear)
    }

    @Test
    fun `mientras no se conoce el ano del album se muestra el de la edicion`() {
        val nirvana = collectionItem(9, "Nirvana", "Nevermind", year = 1991, albumYear = null)
        assertEquals(1991, nirvana.displayYear)
    }

    @Test
    fun `busca por nombre de grupo sin distinguir mayusculas`() {
        val result = filter(items, query = "pink", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(2L), result.map { it.releaseId })
    }

    @Test
    fun `busca por titulo del disco`() {
        val result = filter(items, query = "dark side", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(2L), result.map { it.releaseId })
    }

    @Test
    fun `ignora las tildes al buscar`() {
        val result = filter(items, query = "triangulo", order = CollectionOrder.RECENT_FIRST)
        assertEquals(listOf(1L), result.map { it.releaseId })
    }

    @Test
    fun `filtra por rango de anos usando el ano del album`() {
        val result = filter(items, query = "1960-1980", order = CollectionOrder.OLDEST_FIRST)

        // Barry White (álbum de 1974, edición de 2011) y Pink Floyd (1973); fuera los de los 90 en adelante.
        assertEquals(listOf(4L, 2L), result.map { it.releaseId })
    }

    @Test
    fun `el rango se puede combinar con texto`() {
        val result = filter(items, query = "funk 1960-1980", order = CollectionOrder.RECENT_FIRST)

        assertEquals(listOf(4L), result.map { it.releaseId })
    }

    @Test
    fun `un rango abierto incluye todo lo posterior`() {
        val result = filter(items, query = "desde 1994", order = CollectionOrder.ARTIST)

        assertEquals(listOf("Los Planetas", "Triángulo de Amor Bizarro"), result.map { it.artist })
    }

    @Test
    fun `un disco sin ano conocido queda fuera del rango`() {
        val sinAno = collectionItem(9, "Sin Fecha", "Disco raro", year = null)

        assertEquals(
            emptyList<Long>(),
            filter(listOf(sinAno), "1900-2100", CollectionOrder.RECENT_FIRST).map { it.releaseId },
        )
        // Sin rango activo sigue apareciendo con normalidad.
        assertEquals(
            listOf(9L),
            filter(listOf(sinAno), "raro", CollectionOrder.RECENT_FIRST).map { it.releaseId },
        )
    }

    @Test
    fun `una busqueda sin coincidencias devuelve la lista vacia`() {
        assertEquals(emptyList<Long>(), filter(items, "queen", CollectionOrder.ARTIST).map { it.releaseId })
    }
}
