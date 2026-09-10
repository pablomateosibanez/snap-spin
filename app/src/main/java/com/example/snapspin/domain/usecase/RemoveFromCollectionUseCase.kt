package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.model.CollectionInstance
import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.repository.CollectionRepository

/** Elimina de la colección el ejemplar concreto que se está viendo en el listado. */
class RemoveFromCollectionUseCase(
    private val collection: CollectionRepository,
) {
    suspend operator fun invoke(item: CollectionItem) = collection.removeInstance(
        CollectionInstance(
            instanceId = item.instanceId,
            folderId = item.folderId,
            releaseId = item.releaseId,
        )
    )
}
