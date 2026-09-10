package com.example.snapspin.domain.model

/** Criterio de búsqueda en el catálogo. */
sealed interface CatalogQuery {
    /** Código EAN/UPC leído del disco. */
    data class ByBarcode(val barcode: String) : CatalogQuery

    /** Texto libre; el catálogo ordena los resultados por relevancia. */
    data class ByFreeText(val text: String) : CatalogQuery
}
