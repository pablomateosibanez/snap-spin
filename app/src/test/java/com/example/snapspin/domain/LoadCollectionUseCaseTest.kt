package com.example.snapspin.domain

import com.example.snapspin.domain.usecase.LoadCollectionUseCase
import com.example.snapspin.fake.FakeCollectionRepository
import com.example.snapspin.fake.collectionItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadCollectionUseCaseTest {

    @Test
    fun `carga los discos de la unica coleccion sin preguntar nada`() = runTest {
        val collection = FakeCollectionRepository().apply {
            items = listOf(collectionItem(1, "Nirvana", "Nevermind"))
        }

        val result = LoadCollectionUseCase(collection)()

        assertEquals(1, result.size)
        assertEquals(0, collection.refreshCount)
    }

    @Test
    fun `al refrescar salta la cache del repositorio`() = runTest {
        val collection = FakeCollectionRepository()

        LoadCollectionUseCase(collection)(forceRefresh = true)

        assertEquals(1, collection.refreshCount)
    }
}
