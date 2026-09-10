package com.example.snapspin.domain

import com.example.snapspin.domain.model.EditionChoiceReason
import com.example.snapspin.domain.usecase.SelectStandardEditionUseCase
import com.example.snapspin.fake.FakeCatalogRepository
import com.example.snapspin.fake.candidate
import com.example.snapspin.fake.details
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectStandardEditionUseCaseTest {

    @Test
    fun `elige la edicion de precio mediano`() = runTest {
        val catalog = FakeCatalogRepository(
            detailsById = mapOf(
                1L to details(1, 5.0),
                2L to details(2, 10.0),
                3L to details(3, 20.0),
                4L to details(4, 50.0),
                5L to details(5, 100.0),
            )
        )
        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(candidate(1), candidate(2), candidate(3), candidate(4), candidate(5))
        )

        assertEquals(3L, selection.release.id)
        assertEquals(EditionChoiceReason.MEDIAN_PRICE, selection.reason)
        assertEquals(5, selection.editionsEvaluated)
    }

    @Test
    fun `con un numero par de precios toma el inferior de los dos centrales`() = runTest {
        val catalog = FakeCatalogRepository(
            detailsById = mapOf(
                1L to details(1, 5.0),
                2L to details(2, 10.0),
                3L to details(3, 20.0),
                4L to details(4, 50.0),
            )
        )
        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(candidate(1), candidate(2), candidate(3), candidate(4))
        )

        assertEquals(2L, selection.release.id)
    }

    @Test
    fun `sin precios de mercado cae en la edicion mas extendida`() = runTest {
        val catalog = FakeCatalogRepository(
            detailsById = mapOf(
                1L to details(1, null),
                2L to details(2, null),
            )
        )
        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(candidate(1, have = 4), candidate(2, have = 900))
        )

        assertEquals(2L, selection.release.id)
        assertEquals(EditionChoiceReason.MOST_COMMON, selection.reason)
    }

    @Test
    fun `solo compara ediciones del mismo disco que el resultado mas relevante`() = runTest {
        val catalog = FakeCatalogRepository(
            detailsById = mapOf(
                1L to details(1, 40.0),
                2L to details(2, 45.0),
                9L to details(9, 3.0),
            )
        )

        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(
                candidate(1, masterId = 100L),
                candidate(2, masterId = 100L),
                // Un recopilatorio barato que la búsqueda coló más abajo: no debe ganar.
                candidate(9, have = 5000, masterId = 777L),
            )
        )

        assertEquals(1L, selection.release.id)
        assertEquals(2, selection.editionsEvaluated)
    }

    @Test
    fun `sin master registra directamente el resultado mas relevante`() = runTest {
        val catalog = FakeCatalogRepository(
            detailsById = mapOf(1L to details(1, 40.0), 2L to details(2, 5.0))
        )

        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(candidate(1, masterId = null), candidate(2, have = 900))
        )

        assertEquals(1L, selection.release.id)
        assertEquals(EditionChoiceReason.ONLY_MATCH, selection.reason)
    }

    @Test
    fun `ignora las ediciones cuyo detalle falla`() = runTest {
        val catalog = FakeCatalogRepository(detailsById = mapOf(2L to details(2, 30.0)))

        val selection = SelectStandardEditionUseCase(catalog)(
            listOf(candidate(1), candidate(2))
        )

        assertEquals(2L, selection.release.id)
        assertEquals(EditionChoiceReason.ONLY_MATCH, selection.reason)
    }
}
