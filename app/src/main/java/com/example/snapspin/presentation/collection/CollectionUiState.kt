package com.example.snapspin.presentation.collection

import com.example.snapspin.domain.model.CollectionItem
import com.example.snapspin.domain.model.CollectionOrder

data class CollectionUiState(
    val loading: Boolean = false,
    /** Discos ya filtrados y ordenados, listos para pintar. */
    val visible: List<CollectionItem> = emptyList(),
    val total: Int = 0,
    val query: String = "",
    val order: CollectionOrder = CollectionOrder.RECENT_FIRST,
    val error: String? = null,
    /** Disco cuyo detalle se está mostrando. */
    val selected: CollectionItem? = null,
    /** Disco cuyo borrado está pendiente de confirmar en el modal. */
    val pendingDeletion: CollectionItem? = null,
    val deleting: Boolean = false,
    /** Aviso puntual tras un borrado (snackbar). */
    val message: String? = null,
) {
    val isFiltered: Boolean get() = query.isNotBlank()
}
