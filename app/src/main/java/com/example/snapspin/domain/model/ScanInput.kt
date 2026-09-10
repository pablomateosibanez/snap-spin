package com.example.snapspin.domain.model

/** Dato capturado por la cámara que dispara el registro. */
sealed interface ScanInput {
    /** Código de barras (EAN/UPC) leído del canto o la contraportada. */
    data class Barcode(val value: String) : ScanInput

    /** Fotografía JPEG de la portada del disco. */
    data class Cover(val jpeg: ByteArray) : ScanInput {
        override fun equals(other: Any?) = this === other ||
            (other is Cover && jpeg.contentEquals(other.jpeg))

        override fun hashCode() = jpeg.contentHashCode()
    }
}
