package com.example.snapspin.domain

import com.example.snapspin.domain.usecase.ResolveAlbumYearsUseCase
import com.example.snapspin.fake.FakeCollectionRepository
import com.example.snapspin.fake.collectionItem
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El año de salida del álbum cuesta una petición por obra, así que sólo se piden las que faltan
 * y nunca dos veces la misma.
 */
class ResolveAlbumYearsUseCaseTest {

    private val collection = FakeCollectionRepository()

    private fun useCase() = ResolveAlbumYearsUseCase(
        collection = collection,
        batchSize = 2,
        pauseBetweenBatchesMillis = 5_000L,
    )

    @Test
    fun `resuelve solo los albumes cuyo ano se desconoce`() = runTest {
        collection.albumYears = mapOf(10L to 1974, 20L to 1977)
        val items = listOf(
            collectionItem(1, "Barry White", "Can't Get Enough", masterId = 10L),
            collectionItem(2, "Fleetwood Mac", "Rumours", masterId = 20L),
            // Éste ya lo trae de la caché: no debe volver a pedirse.
            collectionItem(3, "Nirvana", "Nevermind", masterId = 30L, albumYear = 1991),
        )

        val resolved = useCase()(items).toList()

        assertEquals(listOf(10L to 1974, 20L to 1977), resolved)
        assertEquals(listOf(10L, 20L), collection.albumYearRequests)
    }

    @Test
    fun `no pide dos veces la misma obra aunque haya varias ediciones`() = runTest {
        collection.albumYears = mapOf(10L to 1974)
        val items = listOf(
            collectionItem(1, "Barry White", "Can't Get Enough", masterId = 10L),
            collectionItem(2, "Barry White", "Can't Get Enough", masterId = 10L),
        )

        val resolved = useCase()(items).toList()

        assertEquals(listOf(10L to 1974), resolved)
        assertEquals(listOf(10L), collection.albumYearRequests)
    }

    @Test
    fun `una obra sin ano no rompe la resolucion de las demas`() = runTest {
        collection.albumYears = mapOf(20L to 1977)
        val items = listOf(
            collectionItem(1, "Desconocido", "Sin master", masterId = 10L),
            collectionItem(2, "Fleetwood Mac", "Rumours", masterId = 20L),
        )

        assertEquals(listOf(20L to 1977), useCase()(items).toList())
    }

    @Test
    fun `los discos sin obra asociada se ignoran`() = runTest {
        val items = listOf(collectionItem(1, "Rareza", "Sin master", masterId = null))

        assertEquals(emptyList<Pair<Long, Int>>(), useCase()(items).toList())
        assertEquals(emptyList<Long>(), collection.albumYearRequests)
    }
}
