package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.model.RegistrationProposal
import com.example.snapspin.domain.model.RegistrationResult
import com.example.snapspin.domain.repository.CollectionRepository

/**
 * Ejecuta el alta en la colección una vez el usuario ha confirmado.
 *
 * Si el disco ya estaba registrado y se pide sobrescribir, se eliminan los ejemplares anteriores
 * antes de añadir el nuevo, de modo que la colección no acumule duplicados.
 */
class ConfirmRegistrationUseCase(
    private val collection: CollectionRepository,
) {

    suspend operator fun invoke(
        proposal: RegistrationProposal,
        replaceExisting: Boolean,
    ): RegistrationResult {
        val replaced = if (replaceExisting) {
            proposal.existingInstances.onEach { collection.removeInstance(it) }.size
        } else {
            0
        }

        collection.addRelease(proposal.folder.id, proposal.release.id)

        return RegistrationResult(
            release = proposal.release,
            folder = proposal.folder,
            replacedPreviousCopies = replaced,
        )
    }
}
