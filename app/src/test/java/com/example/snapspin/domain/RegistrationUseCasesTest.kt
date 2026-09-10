package com.example.snapspin.domain

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.AlbumIdentity
import com.example.snapspin.domain.model.CatalogQuery
import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.RegistrationStep
import com.example.snapspin.domain.model.ScanInput
import com.example.snapspin.domain.usecase.ConfirmRegistrationUseCase
import com.example.snapspin.domain.usecase.PrepareRegistrationUseCase
import com.example.snapspin.domain.usecase.RegistrationProgress
import com.example.snapspin.domain.usecase.SelectStandardEditionUseCase
import com.example.snapspin.fake.FakeCatalogRepository
import com.example.snapspin.fake.FakeCollectionRepository
import com.example.snapspin.fake.FakeCoverRecognitionService
import com.example.snapspin.fake.candidate
import com.example.snapspin.fake.details
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationUseCasesTest {

    private val catalog = FakeCatalogRepository(detailsById = mapOf(7L to details(7, 12.0)))
    private val collection = FakeCollectionRepository()
    private val recognition = FakeCoverRecognitionService()

    private val prepare = PrepareRegistrationUseCase(
        catalog = catalog,
        collection = collection,
        coverRecognition = recognition,
        selectStandardEdition = SelectStandardEditionUseCase(catalog),
    )

    @Test
    fun `el codigo de barras llega hasta la propuesta sin pasos manuales`() = runTest {
        catalog.onSearch = { listOf(candidate(7)) }

        val progress = prepare(ScanInput.Barcode("0724382995035")).toList()

        assertEquals(
            listOf(
                RegistrationStep.SEARCHING_CATALOG,
                RegistrationStep.CHOOSING_EDITION,
                RegistrationStep.CHECKING_COLLECTION,
            ),
            progress.filterIsInstance<RegistrationProgress.Working>().map { it.step },
        )
        val proposal = (progress.last() as RegistrationProgress.Ready).proposal
        assertEquals(7L, proposal.release.id)
        assertTrue(catalog.queries.first() is CatalogQuery.ByBarcode)
    }

    @Test
    fun `la portada se busca como texto libre con lo que reconoce el servicio de vision`() =
        runTest {
            recognition.identity = AlbumIdentity("nevermind nirvana")
            catalog.onSearch = { listOf(candidate(7)) }

            val progress = prepare(ScanInput.Cover(ByteArray(4))).toList()

            assertEquals(
                listOf(CatalogQuery.ByFreeText("nevermind nirvana")),
                catalog.queries,
            )
            assertTrue(progress.last() is RegistrationProgress.Ready)
        }

    @Test
    fun `si el catalogo no encuentra nada para la portada se avisa con el texto reconocido`() =
        runTest {
            recognition.identity = AlbumIdentity("portada irreconocible")
            catalog.onSearch = { emptyList() }

            val error = runCatching { prepare(ScanInput.Cover(ByteArray(4))).toList() }
                .exceptionOrNull()

            assertTrue(error is AppError.NotFound)
            assertTrue(error!!.message!!.contains("portada irreconocible"))
        }

    @Test
    fun `detecta que el disco ya esta en la coleccion`() = runTest {
        catalog.onSearch = { listOf(candidate(7)) }
        collection.existing = listOf(CollectionInstance(instanceId = 5, folderId = 1, releaseId = 7))

        val proposal = (prepare(ScanInput.Barcode("123")).toList().last()
            as RegistrationProgress.Ready).proposal

        assertTrue(proposal.alreadyInCollection)
    }

    @Test
    fun `sobrescribir elimina los ejemplares previos antes de anadir`() = runTest {
        catalog.onSearch = { listOf(candidate(7)) }
        val previous = CollectionInstance(instanceId = 5, folderId = 1, releaseId = 7)
        collection.existing = listOf(previous)
        val proposal = (prepare(ScanInput.Barcode("123")).toList().last()
            as RegistrationProgress.Ready).proposal

        val result = ConfirmRegistrationUseCase(collection)(proposal, replaceExisting = true)

        assertEquals(listOf(previous), collection.removed)
        assertEquals(listOf(1L to 7L), collection.added)
        assertEquals(1, result.replacedPreviousCopies)
    }

    @Test
    fun `sin sobrescribir se anade sin tocar lo existente`() = runTest {
        catalog.onSearch = { listOf(candidate(7)) }
        val proposal = (prepare(ScanInput.Barcode("123")).toList().last()
            as RegistrationProgress.Ready).proposal

        val result = ConfirmRegistrationUseCase(collection)(proposal, replaceExisting = false)

        assertTrue(collection.removed.isEmpty())
        assertEquals(listOf(1L to 7L), collection.added)
        assertEquals(0, result.replacedPreviousCopies)
    }
}
