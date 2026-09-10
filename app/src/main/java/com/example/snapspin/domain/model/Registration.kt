package com.example.snapspin.domain.model

/** Motivo por el que se ha elegido automáticamente una edición entre todas las disponibles. */
enum class EditionChoiceReason {
    /** Precio mediano entre las ediciones con precio de mercado conocido. */
    MEDIAN_PRICE,

    /** Ninguna edición tenía precio: se ha usado la más extendida entre la comunidad. */
    MOST_COMMON,

    /** Sólo había una edición posible. */
    ONLY_MATCH,
}

/** Edición elegida automáticamente, con la traza de cómo se ha decidido. */
data class SelectedEdition(
    val release: ReleaseDetails,
    val reason: EditionChoiceReason,
    val editionsEvaluated: Int,
)

/**
 * Todo lo necesario para pintar el modal de confirmación y ejecutar el alta.
 * Se calcula antes de preguntar al usuario: así el único paso manual es un sí/no.
 */
data class RegistrationProposal(
    val edition: SelectedEdition,
    val folder: CollectionFolder,
    val existingInstances: List<CollectionInstance>,
    val source: ScanMode,
) {
    val release: ReleaseDetails get() = edition.release
    val alreadyInCollection: Boolean get() = existingInstances.isNotEmpty()
}

/** Resultado del alta en la colección. */
data class RegistrationResult(
    val release: ReleaseDetails,
    val folder: CollectionFolder,
    val replacedPreviousCopies: Int,
)

/** Fases del registro automático, para informar al usuario mientras trabaja. */
enum class RegistrationStep {
    RECOGNISING_COVER,
    SEARCHING_CATALOG,
    CHOOSING_EDITION,
    CHECKING_COLLECTION,
}
