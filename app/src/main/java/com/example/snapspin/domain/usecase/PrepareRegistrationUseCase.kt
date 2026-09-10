package com.example.snapspin.domain.usecase

import com.example.snapspin.domain.error.AppError
import com.example.snapspin.domain.model.CatalogQuery
import com.example.snapspin.domain.model.RegistrationProposal
import com.example.snapspin.domain.model.RegistrationStep
import com.example.snapspin.domain.model.ScanInput
import com.example.snapspin.domain.model.ScanMode
import com.example.snapspin.domain.repository.CollectionRepository
import com.example.snapspin.domain.repository.MusicCatalogRepository
import com.example.snapspin.domain.service.CoverRecognitionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Avance del proceso automático previo a la confirmación del usuario. */
sealed interface RegistrationProgress {
    data class Working(val step: RegistrationStep) : RegistrationProgress
    data class Ready(val proposal: RegistrationProposal) : RegistrationProgress
}

/**
 * Convierte una captura de cámara en una propuesta de alta lista para confirmar.
 *
 * Encadena, sin intervención del usuario: reconocimiento (sólo para portadas) -> búsqueda en el
 * catálogo -> elección automática de edición -> comprobación de duplicados en la colección.
 *
 * Discogs no busca por imagen: lo que reconoce Vision entra como texto libre y es el propio
 * catálogo, con su relevancia, quien decide qué disco es.
 *
 * Se expone como [Flow] para poder ir informando de la fase en curso; el último elemento emitido
 * es siempre [RegistrationProgress.Ready]. Cualquier fallo se propaga como [AppError].
 */
class PrepareRegistrationUseCase(
    private val catalog: MusicCatalogRepository,
    private val collection: CollectionRepository,
    private val coverRecognition: CoverRecognitionService,
    private val selectStandardEdition: SelectStandardEditionUseCase,
) {

    operator fun invoke(input: ScanInput): Flow<RegistrationProgress> = flow {
        val candidates = when (input) {
            is ScanInput.Barcode -> {
                emit(RegistrationProgress.Working(RegistrationStep.SEARCHING_CATALOG))
                catalog.search(CatalogQuery.ByBarcode(input.value.trim()))
                    .ifEmpty { throw AppError.NotFound("ningún disco con el código ${input.value}") }
            }

            is ScanInput.Cover -> {
                emit(RegistrationProgress.Working(RegistrationStep.RECOGNISING_COVER))
                val identity = coverRecognition.identify(input.jpeg)
                emit(RegistrationProgress.Working(RegistrationStep.SEARCHING_CATALOG))
                catalog.search(CatalogQuery.ByFreeText(identity.query))
                    .ifEmpty {
                        throw AppError.NotFound("ningún disco que encaje con «${identity.query}»")
                    }
            }
        }

        emit(RegistrationProgress.Working(RegistrationStep.CHOOSING_EDITION))
        val edition = selectStandardEdition(candidates)

        emit(RegistrationProgress.Working(RegistrationStep.CHECKING_COLLECTION))
        val folder = collection.defaultFolder()
        val existing = collection.instancesOf(edition.release.id)

        emit(
            RegistrationProgress.Ready(
                RegistrationProposal(
                    edition = edition,
                    folder = folder,
                    existingInstances = existing,
                    source = if (input is ScanInput.Barcode) ScanMode.BARCODE else ScanMode.COVER,
                )
            )
        )
    }

}
