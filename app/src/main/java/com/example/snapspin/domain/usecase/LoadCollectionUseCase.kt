package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.repository.CollectionRepository

/** Carga los discos ya registrados, resolviendo por su cuenta la única colección del usuario. */
class LoadCollectionUseCase(
    private val collection: CollectionRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<CollectionItem> =
        collection.items(collection.defaultFolder().id, forceRefresh)
}
