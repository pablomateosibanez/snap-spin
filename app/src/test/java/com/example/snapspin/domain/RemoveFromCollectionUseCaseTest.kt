package com.example.snapspin.domain

import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.usecase.RemoveFromCollectionUseCase
import com.example.snapspin.fake.FakeCollectionRepository
import com.example.snapspin.fake.collectionItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoveFromCollectionUseCaseTest {

    @Test
    fun `borra el ejemplar concreto, con su carpeta y su edicion`() = runTest {
        val collection = FakeCollectionRepository()
        val item = collectionItem(42, "Nirvana", "Nevermind", folderId = 7L)

        RemoveFromCollectionUseCase(collection)(item)

        assertEquals(
            listOf(CollectionInstance(instanceId = 42L, folderId = 7L, releaseId = 42L)),
            collection.removed,
        )
    }
}
